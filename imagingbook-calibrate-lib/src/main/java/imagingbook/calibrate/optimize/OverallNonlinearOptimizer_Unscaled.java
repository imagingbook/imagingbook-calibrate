/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.List;

/**
 * Nonlinear optimizer based on the Levenberg-Marquart method, where the Jacobian matrix is calculated numerically
 * (i.e., by estimating the first partial derivatives from finite differences). The advantage is that the calculation of
 * the Jacobian is independent of the calibration model, while performance and runtime are similar to the analytic
 * version (removed).
 * The optimizer packs the intrinsic camera parameters, distortion parameters and all external view
 * parameters into a single parameter vector and tries to minimize the reprojection errors over all pairs of
 * model/image points.
 *
 * @author WB
 */
public class OverallNonlinearOptimizer_Unscaled implements NonlinearOptimizer {

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 100;

    private final Pnt2d[][] modPts;
    private final Pnt2d[][] obsPts;
    private final int M;                // number of views
    private final int N;
    private final int K;
    private final int camParCount;      // number of camera parameters (7+)
    private final int viewParCount;     // number of view parameters (6)

    private final Camera initCam;
    private Camera finalCamera;
    private final ViewTransform[] initViews;
    private ViewTransform[] finalViews;
    private final double[] initialParameters;
    private final double[] parameterScales;

    private final double[][] J;
    private final MultivariateVectorFunction valueFun;
    private final MultivariateMatrixFunction jacobianFun;
    private LeastSquaresOptimizer.Optimum result;


    public OverallNonlinearOptimizer_Unscaled(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;
        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
        this.M = obsPntSet.size();
        this.N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); //getTotalPointCount();
        this.initialParameters = makeInitialParameters();
        this.parameterScales = makeParameterScales();
        this.K = initialParameters.length;
        this.J = new double[2 * N + 1][K];  // extra row for penalty
        this.valueFun = getValueFunction();
        this.jacobianFun = getJacobianFunction();
    }

    // -------------------------------------------------------------------------------------

    private MultivariateVectorFunction getValueFunction() {
        return params -> {
            //double[] params = unscaleParameters(paramsS, parameterScales);
            System.out.println("OverallNonlinearOptimizer: p = " + Matrix.toString(params));
            double[] a = Arrays.copyOfRange(params, 0, camParCount);
            Camera cam = initCam.fromParameters(a);
            double[] Y = new double[2 * N + 1];     // extra entry for penalty
            int r = 0;
            for (int k = 0; k < M; k++) {
                int qk = camParCount + k * viewParCount;
                double[] wk = Arrays.copyOfRange(params, qk, qk + viewParCount);
                ViewTransform Vk = new ViewTransform(wk);
                for (int i = 0; i < modPts[k].length; i++) {
                    double[] uv = cam.project(Vk, modPts[k][i]);
                    Y[r * 2 + 0] = uv[0];
                    Y[r * 2 + 1] = uv[1];
                    r = r + 1;
                }
            }
            Y[2 * N] = 1000000 * a[2];
            System.out.println("OverallNonlinearOptimizer: Y = " + Matrix.toString(Arrays.copyOf(Y, 20)));
            return Y;
        };
    }

    // ------------------------------------

    private MultivariateMatrixFunction getJacobianFunction() {
        return params -> {
            // double[] params = unscaleParameters(paramsS, parameterScales);
            double[] uvRef = valueFun.value(params);      // values from undisturbed parameters
            double[] a = Arrays.copyOfRange(params, 0, camParCount);    // camera parameters
            Camera camOrig = initCam.fromParameters(a);

            for (int i = 0; i < J.length; i++) {        // clear recycled Jacobian matrix
                Arrays.fill(J[i], 0.0);
            }

            // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
            for (int p = 0; p < a.length; p++) {    // for all camera parameters
                double ap = a[p];                    // keep original parameter value
                double delta = estimateDelta(ap);
                a[p] = a[p] + delta;        // modify parameter p
                Camera camMod = camOrig.fromParameters(a);    // modified camera

                for (int k = 0, r = 0; k < M; k++) {    // for all views k, r = row
                    int m = camParCount + k * viewParCount;
                    double[] w = Arrays.copyOfRange(params, m, m + viewParCount);
                    ViewTransform view = new ViewTransform(w);
                    for (int j = 0; j < modPts[k].length; j++, r += 2) {    // for all model points: calculate disturbed value
                        Pnt2d Pj = modPts[k][j];
                        double[] uvMod = camMod.project(view, Pj);
                        J[r + 0][p] = (uvMod[0] - uvRef[r + 0]) / delta;   // dX
                        J[r + 1][p] = (uvMod[1] - uvRef[r + 1]) / delta;   // dY
                    }
                }
                a[p] = ap;    // revert parameter a[p] to original value
            }

            // Step 2: calculate the diagonal blocks, one for each view
            for (int k = 0; k < M; k++) {    // for all views/blocks
                final int start = camParCount + k * viewParCount;
                double[] w = Arrays.copyOfRange(params, start, start + viewParCount);
                final int c = a.length + k * w.length;        // leftmost matrix column of block i
                for (int p = 0; p < w.length; p++) {    // for all parameters in w
                    double wp = w[p];                    // keep original parameter value w[p]
                    double delta = estimateDelta(wp);
                    w[p] = w[p] + delta;                // modify parameter w_k
                    ViewTransform view = new ViewTransform(w);
                    int r = 2 * k * modPts[k].length;    // row
                    for (int j = 0; j < modPts[k].length; j++) {        // for all model points: calculate disturbed value
                        Pnt2d Pj = modPts[k][j];
                        double[] uvMod = camOrig.project(view, Pj);
                        J[r + 0][c + p] = (uvMod[0] - uvRef[r + 0]) / delta;   // dX
                        J[r + 1][c + p] = (uvMod[1] - uvRef[r + 1]) / delta;   // dY
                        r = r + 2;
                    }
                    w[p] = wp; // w[k] - DELTA;		// revert parameter w[p] to original value
                }
            }
            J[2 * N][2] = 1.0;  // Jacobian for parameter gamma

            System.out.println("J condition No = " + Matrix.getConditionNumber(J));
            return J;
        };
    }

    private static final double EPS = 1.5e-8; 	// = sqrt(2.2 * 10^{-16})

    /**
     * Returns a positive delta value adapted to the magnitude of the parameter x
     * @param x
     * @return
     */
    private static double estimateDelta(double x) {
        //final double eps = 1.5e-8;	// = sqrt(2.2 * 10^{-16})
        double dx = EPS * Math.max(Math.abs(x), 1); // dx >= eps
        // avoid numerical truncation problems (add and subtract again) -
        // not sure this survives the compiler !?
        double tmp = x + dx;
        return tmp - x;
    }

    private double[] scaleParameters(double[] unscaledParams, double[] scales) {
        double[] sp = new double[unscaledParams.length];
        for (int i = 0; i < unscaledParams.length; i++) {
            sp[i] = unscaledParams[i] * scales[i];
        }
        return sp;
    }

    private double[] unscaleParameters(double[] scaledParams, double[] scales) {
        double[] usp = new double[scaledParams.length];
        for (int i = 0; i < scaledParams.length; i++) {
            usp[i] = scaledParams[i] / scales[i];
        }
        return usp;
    }

    // ---------------------------------------------------------------------------------------

    /**
     * Performs Levenberg-Marquardt non-linear optimization to get better estimates of the
     * parameters.
     */
    public void optimize() {
        // RealVector start = new ArrayRealVector(initialParameters, false);
        // System.out.println("OverallNonlinearOptimizer: start = " + Matrix.toString(start));
        double[] observed = makeObservedVector();
        // System.out.println("OverallNonlinearOptimizer: observed size = " + observed.getDimension() + " last item = " + observed.getEntry(2 * N));

        MultivariateJacobianFunction model = LeastSquaresFactory.model(valueFun, jacobianFun);
        // System.out.println("OverallNonlinearOptimizer: value size = " + valueFun.value(initialParameters).length);
        // System.out.println("OverallNonlinearOptimizer: jacob size = " + jacobianFun.value(initialParameters).length);

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                // .weight(new DiagonalMatrix(weights, false))
                .target(observed)
                .model(model)
                .start(initialParameters)
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        // LeastSquaresOptimizer.Optimum result = lm.optimize(LeastSquaresFactory.create(
        //         model,
        //         observed,
        //         start,
        //         null,
        //         maxEvaluations,
        //         maxIterations));

//		System.out.println(NonlinearOptimizer.class.getSimpleName() + "; iterations = " + result.getIterations());
        this.result = result;
        updateEstimates(result.getPoint());
    }

    // -----------------------------------------------------------------------------------------

    private double[] makeInitialParameters() {
        double[] cp = initCam.getParameters();
        System.out.println("NonlinearOptimizer: cp.length = " + cp.length);
        double[] p = new double[cp.length + M * viewParCount];
        System.out.println("NonlinearOptimizer: p.length = " + p.length);
        System.out.println("NonlinearOptimizer: M = " + M);

        // insert camera parameters at beginning of c
        System.arraycopy(cp, 0, p, 0, cp.length);

        // insert M view parameters
        int start = cp.length;
        for (int k = 0; k < M; k++) {
            double[] w = initViews[k].getParameters();
            System.arraycopy(w, 0, p, start, w.length);
            start = start + w.length;
        }
        return p;
    }

    private double[] makeParameterScales() {
        double[] cp = initCam.getParameters();
        double[] scales = new double[cp.length + M * viewParCount];
        Arrays.fill(scales, 1.0);
        scales[0] = 0.01; // alpha
        scales[1] = 0.01; // beta
        scales[2] = 50; // gamma
        scales[3] = 0.01;   // uc
        scales[4] = 0.01;   // vc

        // insert M view parameters
        int start = cp.length;
        for (int k = 0; k < M; k++) {
            double[] vp = initViews[k].getParameters();
            // TODO!!
            start += vp.length;
        }
        return scales;
    }

    /**
     * Stack the observed image coordinates of the calibration pattern points into a vector.
     * @return the observed vector
     */
    double[] makeObservedVector() {
        double[] obs = new double[2 * N + 1];  // one extra cell = 0!
        for (int k = 0, r = 0; k < M; k++) {
            for (int i = 0; i < obsPts[k].length; i++, r++) {
                obs[r * 2 + 0] = obsPts[k][i].getX();
                obs[r * 2 + 1] = obsPts[k][i].getY();
            }
        }
        // obs = [u_{0,0}, v_{0,0}, u_{0,1}, v_{0,1}, ..., u_{M-1,N-1}, v_{M-1,N-1}]
        return obs;
    }

    private double[] makeWeightMatrix() {
        double[] weights = new double[2 * N + 1];
        Arrays.fill(weights, 1.0);
        return weights;
    }

    private void updateEstimates(RealVector parameters) {
        double[] c = parameters.toArray();
        double[] s = Arrays.copyOfRange(c, 0, camParCount);
        finalCamera = initCam.fromParameters(s);
        finalViews = new ViewTransform[M];
        int start = s.length;
        for (int k = 0; k < M; k++) {
            double[] wk = Arrays.copyOfRange(c, start, start + viewParCount);
            finalViews[k] = new ViewTransform(wk);
            start = start + wk.length;
        }
    }

    // -------------------------------------------------------------------------------------

    /**
     * Returns the optimized camera parameters.
     * @return the optimized camera parameters
     */
    public Camera getFinalCamera() {
        return finalCamera;
    }

    /**
     * Returns the optimized view parameters.
     * @return the optimized view parameters
     */
    public List<ViewTransform> getFinalViews() {
        return Arrays.asList(finalViews);
    }

    public int getIterations() {
        return result.getIterations();
    }

    public int getEvaluations() {
        return result.getEvaluations();
    }

    public RealVector getResiduals() {
        return result.getResiduals();
    }

}
