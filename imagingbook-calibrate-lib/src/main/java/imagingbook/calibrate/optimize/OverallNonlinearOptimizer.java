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
public class OverallNonlinearOptimizer {

    static double GLOBAL_PARAMETER_SCALE = 1.0;

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

    private final double[] observed;

    private final double[][] J;
    private final MultivariateVectorFunction valueFun;
    private final MultivariateMatrixFunction jacobianFun;
    private LeastSquaresOptimizer.Optimum result;


    public OverallNonlinearOptimizer(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;
        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
        this.M = obsPntSet.size();
        this.N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); //getTotalPointCount();


        double[] camScales = {10000, 10000, 1, 3000, 2000};   // alpha, beta, gamma, uc, vc
        double[] distScales = new double[initCam.getDistortionParameters().length];
                    Arrays.fill(distScales, 0.1);
        double[] viewScales = {0.5, 0.5, 0.5, 100, 100, 100};
        this.parameterScales = makeParameterScales(camScales, distScales, viewScales, M);
        System.out.println("parameterScales = " + Matrix.toString(parameterScales));

        this.initialParameters = makeInitialParameters();
        this.K = initialParameters.length;
        this.J = new double[2 * N + 1][K];  // extra row for gamma penalty
        this.valueFun = getValueFunction();
        this.jacobianFun = getJacobianFunction();
        this.observed = makeObservedVector();
    }

    // -------------------------------------------------------------------------------------

    private MultivariateVectorFunction getValueFunction() {
        return paramsS -> {
            double[] params = unscaleParameters(paramsS, parameterScales);
            System.out.println("OverallNonlinearOptimizer: pS = " + Matrix.toString(paramsS));
            System.out.println("OverallNonlinearOptimizer: pU = " + Matrix.toString(params));
            double[] a = Arrays.copyOfRange(params, 0, camParCount);
            Camera cam = initCam.fromParameters(a);
            double[] V = new double[2 * N + 1];     // extra row for gamma penalty
            int r = 0;
            for (int k = 0; k < M; k++) {
                int qk = camParCount + k * viewParCount;
                double[] wk = Arrays.copyOfRange(params, qk, qk + viewParCount);
                ViewTransform Vk = new ViewTransform(wk);
                for (int i = 0; i < modPts[k].length; i++) {
                    double[] uv = cam.project(Vk, modPts[k][i]);
                    V[r * 2 + 0] = uv[0];
                    V[r * 2 + 1] = uv[1];
                    r = r + 1;
                }
            }
            V[2 * N] = 100000 * a[2]; // set penalty for gamma
            double[] resid = Matrix.subtract(observed, V);
            // System.out.println("OverallNonlinearOptimizer: Y = " + Matrix.toString(Arrays.copyOf(Y, 20)));
            // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));
            System.out.println("OverallNonlinearOptimizer: |R1| = " + Matrix.normL2(resid));
            System.out.println("OverallNonlinearOptimizer: |Rgamma| = " + resid[resid.length-1]);
            return V;
        };
    }

    // ------------------------------------

    private MultivariateMatrixFunction getJacobianFunction() {
        return paramsS -> {
            double[] uvRef = valueFun.value(paramsS);      // values from undisturbed parameters
            double[] params = unscaleParameters(paramsS, parameterScales);

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
                    int q = camParCount + k * viewParCount;
                    double[] wk = Arrays.copyOfRange(params, q, q + viewParCount);
                    ViewTransform viewk = new ViewTransform(wk);
                    for (int j = 0; j < modPts[k].length; j++, r += 2) {    // for all model points: calculate disturbed value
                        Pnt2d Pj = modPts[k][j];
                        double[] uvMod = camMod.project(viewk, Pj);
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
            J[2 * N][2] = 0.5;  // Jacobian for parameter gamma

            for (int j = 0; j < 2; j++) {
                System.out.printf("    Ju[%d] = %s\n", j, Matrix.toString(J[j]));
            }

            // scale J back to optimizer scale
            for (int p = 0; p < params.length; p++) {
                // multiply column J[*][p] by scale[p]:
                double s = parameterScales[p];
                for (int j = 0; j < J.length; j++) {
                    J[j][p] = J[j][p] * s;
                }
            }

            for (int j = 0; j < 2; j++) {
                System.out.printf("    Js[%d] = %s\n", j, Matrix.toString(J[j]));
            }
            // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));


            System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
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


    // ---------------------------------------------------------------------------------------

    /**
     * Performs Levenberg-Marquardt non-linear optimization to get better estimates of the
     * parameters.
     */
    public void optimize() {
        // RealVector start = new ArrayRealVector(initialParameters, false);
        System.out.println("initialParameters = " + Matrix.toString(initialParameters));
        // double[] observed = makeObservedVector();
        // System.out.println("OverallNonlinearOptimizer: observed size = " + observed.getDimension() + " last item = " + observed.getEntry(2 * N));

        MultivariateJacobianFunction model = LeastSquaresFactory.model(valueFun, jacobianFun);
        // System.out.println("OverallNonlinearOptimizer: value size = " + valueFun.value(initialParameters).length);
        // System.out.println("OverallNonlinearOptimizer: jacob size = " + jacobianFun.value(initialParameters).length);

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                // .weight(new DiagonalMatrix(weights, false))
                .target(observed)
                .model(model)
                .start(scaleParameters(initialParameters, parameterScales))
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(1);
        System.out.println("LevenbergMarquardtOptimizer: InitialStepBoundFactor = " + lm.getInitialStepBoundFactor());
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
        double[] optParams = result.getPoint().toArray();
        updateEstimates(unscaleParameters(optParams, parameterScales));
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Returns non-scaled (original) parameters.
     * @return
     */
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

    /**
     * Creates and returns a vector of scales for all optimization parameters.
     * @param camScales the scale factors for
     * @param distScales scales for distortion parameters
     * @param viewScales scales for view transform parameters
     * @param viewCnt number of views
     * @return a vector scale values for all parameters
     */   // TODO: revise to use initial Camera to obtain default scale values
    static double[] makeParameterScales(double[] camScales, double[] distScales, double[] viewScales, int viewCnt) {
        int camParCount = camScales.length;
        int distParCount = distScales.length;
        int viewParCount = viewScales.length;
        double[] scales = new double[camParCount + distParCount + viewCnt * viewParCount];

        Arrays.fill(scales, 1); //GLOBAL_PARAMETER_SCALE);
        // scales[0] = 5000;
        // scales[1] = 5000;
        // return scales;

        int start = 0;
        System.arraycopy(camScales, 0, scales, start, camScales.length);
        start += camScales.length;
        System.arraycopy(distScales, 0, scales, start, distScales.length);
        start += distScales.length;
        for (int k = 0; k < viewCnt; k++) {
            System.arraycopy(viewScales, 0, scales, start, viewScales.length);
            start += viewScales.length;
        }
        return scales;
    }

    /**
     * Stack the observed image coordinates of the calibration pattern points into a vector.
     * @return the observed vector
     */
    double[] makeObservedVector() {
        double[] obs = new double[2 * N + 1];  // extra row with value 0 for gamma penalty
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

    private void updateEstimates(double[] params) {
        // double[] c = parameters;
        double[] s = Arrays.copyOfRange(params, 0, camParCount);
        finalCamera = initCam.fromParameters(s);
        finalViews = new ViewTransform[M];
        int start = s.length;
        for (int k = 0; k < M; k++) {
            double[] wk = Arrays.copyOfRange(params, start, start + viewParCount);
            finalViews[k] = new ViewTransform(wk);
            start = start + wk.length;
        }
    }

    // Parameter scaling --------------------------------------------------------------------


    /**
     * Sets the scales for the parameters of one view transform
     * @param scales
     * @param k view number
     */
    private void setViewParameterScales(double[] scales, int k) {

    }

    private double[] scaleParameters(double[] unscaledParams, double[] scales) {
        double[] sp = new double[unscaledParams.length];
        for (int i = 0; i < unscaledParams.length; i++) {
            sp[i] = unscaledParams[i] / scales[i];
        }
        return sp;
    }

    private double[] unscaleParameters(double[] scaledParams, double[] scales) {
        double[] usp = new double[scaledParams.length];
        for (int i = 0; i < scaledParams.length; i++) {
            usp[i] = scaledParams[i] * scales[i];
        }
        return usp;
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
