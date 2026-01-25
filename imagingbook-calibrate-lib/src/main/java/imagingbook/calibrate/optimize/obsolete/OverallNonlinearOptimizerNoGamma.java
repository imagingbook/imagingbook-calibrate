/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize.obsolete;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.AbstractCamera;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
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
public class OverallNonlinearOptimizerNoGamma implements NonlinearOptimizer {

    static int PARAMETER_SKIP_INDEX = 2;
    static double GLOBAL_PARAMETER_SCALE = 1.0;
    static double GAMMA_PENALTY = 100000;

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    final Pnt2d[][] modPts;
    private final Pnt2d[][] obsPts;
    final int M;                // number of views
    final int N;
    private final int K;
    final int camParCount;      // number of camera parameters (7+)
    final int viewParCount;     // number of view parameters (6)

    private final Camera initCam;
    private AbstractCamera finalCamera;
    private final ViewTransform[] initViews;
    private ViewTransform[] finalViews;
    private final double[] initialParameters;
    private final double[][] parameterScales;

    private final double[] observed;

    private final double[][] J;

    // private final MultivariateVectorFunction valueFun;
    // private final MultivariateMatrixFunction jacobianFun;

    private LeastSquaresOptimizer.Optimum result;


    public OverallNonlinearOptimizerNoGamma(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;
        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
        this.M = obsPntSet.size();
        this.N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); //getTotalPointCount();


        // double[] camScales = {10000, 10000, 1, 3000, 2000};   // alpha, beta, gamma, uc, vc
        double[] camScales = {10, 10, 15, 1, 1};   // alpha, beta, gamma, uc, vc
        double[] distScales = { 0.01, 0.2}; //new double[initCam.getDistortionParameters().length];

        double[] viewScales = {0.002, 0.002, 0.002, 1, 1, 1};
        // double[] viewScales = {1, 1, 1, 1, 1, 1};
        this.parameterScales = makeParameterScales(camScales, distScales, viewScales, M);
        System.out.println("parameterScales  = " + Matrix.toString(parameterScales[0]));
        System.out.println("parameterOffsets = " + Matrix.toString(parameterScales[1]));

        this.initialParameters = makeInitialParameters();
        this.K = initialParameters.length;
        // this.J = new double[2 * N + 1][K];  // extra row for gamma penalty
        this.J = new double[2 * N][K];  // no gamma penalty

        // this.valueFun = getValueFunction();
        // this.jacobianFun = getJacobianFunction();

        this.observed = makeObservedVector();
    }

    // -------------------------------------------------------------------------------------

    // private MultivariateVectorFunction getValueFunction() {
    //     return paramsS -> getValue(paramsS);
    // }

    double[] getValue(double[] paramsS) {
        double[] params = unscaleParameters(paramsS, parameterScales);
        System.out.println("OverallNonlinearOptimizer: pS = " + Matrix.toString(paramsS));
        System.out.println("OverallNonlinearOptimizer: pU = " + Matrix.toString(params));
        double[] a = Arrays.copyOfRange(params, 0, camParCount);
        AbstractCamera cam = initCam.withParameters(a);
        // double[] V = new double[2 * N + 1];     // extra row for gamma penalty
        double[] V = new double[2 * N];     // no gamma penalty
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
        // V[2 * N] = GAMMA_PENALTY * a[2]; // set penalty for gamma


        double[] resid = Matrix.subtract(observed, V);
        // System.out.println("OverallNonlinearOptimizer: Y = " + Matrix.toString(Arrays.copyOf(Y, 20)));
        // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));
        System.out.println("OverallNonlinearOptimizer: |R1| = " + Matrix.normL2(resid));
        // System.out.println("OverallNonlinearOptimizer: |Rgamma| = " + resid[resid.length-1]);
        return V;
    }

    // ------------------------------------

    // private MultivariateMatrixFunction getJacobianFunction() {
    //     return paramsS -> getJacobian(paramsS);
    // }

    double[][] getJacobian(double[] paramsS) {
        double[] uvRef = getValue(paramsS);      // values from undisturbed parameters
        // double[] uvRef = valueFun.value(paramsS);      // values from undisturbed parameters
        double[] params = unscaleParameters(paramsS, parameterScales);

        double[] a = Arrays.copyOfRange(params, 0, camParCount);    // camera parameters
        AbstractCamera camOrig = initCam.withParameters(a);

        for (int i = 0; i < J.length; i++) {        // clear recycled Jacobian matrix
            Arrays.fill(J[i], 0.0);
        }

        // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
        for (int p = 0; p < a.length; p++) {    // for all camera parameters
            double ap = a[p];                    // keep original parameter value
            double delta = estimateDelta(ap);
            a[p] = a[p] + delta;        // modify parameter p
            AbstractCamera camMod = camOrig.withParameters(a);    // modified camera

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
        // J[2 * N][2] = 1;  // Jacobian for parameter gamma

        for (int j = 0; j < 2; j++) {
            System.out.printf("    Ju[%d] = %s\n", j, Matrix.toString(J[j]));
        }

        // scale J back to optimizer scale
        for (int p = 0; p < params.length; p++) {
            // multiply column J[*][p] by scale[p]:
            double s = parameterScales[0][p];
            for (int j = 0; j < J.length; j++) {
                J[j][p] = J[j][p] * s;
            }
        }

        for (int j = 0; j < 2; j++) {
            System.out.printf("    Js[%d] = %s\n", j, Matrix.toString(J[j]));
        }
        // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));

        double[] colNorms = getMatrixColumnNorms(J);
        System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));

        System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
        return J;
    }

    private double[] getMatrixColumnNorms(double[][] J) {
        double[] colNorms = new double[J[0].length];
        RealMatrix JR = new Array2DRowRealMatrix(J, false);
        for (int j = 0; j < colNorms.length; j++) {
            colNorms[j] = JR.getColumnVector(j).getNorm();
        }

        return colNorms;
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
     *
     * @return
     */
    public boolean optimize() {
        // RealVector start = new ArrayRealVector(initialParameters, false);
        System.out.println("initialParameters = " + Matrix.toString(initialParameters));
        // double[] observed = makeObservedVector();
        // System.out.println("OverallNonlinearOptimizer: observed size = " + observed.getDimension() + " last item = " + observed.getEntry(2 * N));

        // MultivariateJacobianFunction model = LeastSquaresFactory.model(valueFun, jacobianFun);
        // MultivariateJacobianFunction model = new FullOptimizationModel();
        ReducedOptimizationModel model = new ReducedOptimizationModel(PARAMETER_SKIP_INDEX);

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(observed)
                .model(model)
                .start(skipItem(scaleParameters(initialParameters, parameterScales), PARAMETER_SKIP_INDEX))
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(1);
        System.out.println("LevenbergMarquardtOptimizer: InitialStepBoundFactor = " + lm.getInitialStepBoundFactor());
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        this.result = result;
        double[] optParams = result.getPoint().toArray();
        PrintPrecision.set(8);
        System.out.println("optimal parameters (reduced) = " + Matrix.toString(optParams));
        double[] fullParams = insertItem(optParams, PARAMETER_SKIP_INDEX, 0.0);
        System.out.println("optimal parameters (full)    = " + Matrix.toString(fullParams));
        double[] unscaledParams = unscaleParameters(fullParams, parameterScales);
        System.out.println("unscaled parameters (full)   = " + Matrix.toString(unscaledParams));
        updateEstimates(unscaledParams);

        PrintPrecision.set(3);
        System.out.println("Covariance Matrix: \n" + Matrix.toString(getCovarianceMatrix(result)));
        return true;
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
    static double[][] makeParameterScales(double[] camScales, double[] distScales, double[] viewScales, int viewCnt) {
        int camParCount = camScales.length;
        int distParCount = distScales.length;
        int viewParCount = viewScales.length;
        int P = camParCount + distParCount + viewCnt * viewParCount;
        double[] scale = new double[P];
        double[] offset = new double[P];

        Arrays.fill(scale, 1); //GLOBAL_PARAMETER_SCALE);
        Arrays.fill(scale, 0);

        int start = 0;
        System.arraycopy(camScales, 0, scale, start, camScales.length);
        start += camScales.length;
        System.arraycopy(distScales, 0, scale, start, distScales.length);
        start += distScales.length;
        for (int k = 0; k < viewCnt; k++) {
            System.arraycopy(viewScales, 0, scale, start, viewScales.length);
            start += viewScales.length;
        }

        // offset[0] = 10500;
        // offset[1] = 10500;
        //
        // offset[3] = 3000;
        // offset[4] = 2000;

        // Arrays.fill(scale, 1.0);
        Arrays.fill(offset, 0);
        return new double[][] {scale, offset};
    }

    /**
     * Stack the observed image coordinates of the calibration pattern points into a vector.
     * @return the observed vector
     */
    double[] makeObservedVector() {
        // double[] obs = new double[2 * N + 1];  // extra row with value 0 for gamma penalty
        double[] obs = new double[2 * N];  // no gamma penalty
        for (int k = 0, r = 0; k < M; k++) {
            for (int i = 0; i < obsPts[k].length; i++, r++) {
                obs[r * 2 + 0] = obsPts[k][i].getX();
                obs[r * 2 + 1] = obsPts[k][i].getY();
            }
        }
        // obs = [u_{0,0}, v_{0,0}, u_{0,1}, v_{0,1}, ..., u_{M-1,N-1}, v_{M-1,N-1}]
        return obs;
    }

    private void updateEstimates(double[] params) {
        // double[] c = parameters;
        double[] s = Arrays.copyOfRange(params, 0, camParCount);
        finalCamera = initCam.withParameters(s);
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

    /**
     * Scales original (physical) parameters p[i] to optimizer parameters ps[i] by
     * <pre>
     *     ps[i] = (p[i] - offset[i]) / scales[i] </pre>
     * @param p
     * @param scales = {scale, offset}
     * @return
     */
    private double[] scaleParameters(double[] p, double[][] scales) {
        double[] scale = scales[0];
        double[] offset = scales[1];
        double[] ps = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            ps[i] = (p[i] - scales[1][i]) / scales[0][i];
        }
        return ps;
    }

    /**
     * Scales optimizer parameters ps[i] back to original parameters p[i] by
     * <pre>
     *     p[i] = ps[i] * scales[i] + offset[i] </pre>
     * @param ps
     * @param scales = {scale, offset}
     * @return
     */
    private double[] unscaleParameters(double[] ps, double[][] scales) {
        // System.out.printf("unscaleParameters(): ps.length = %d, scales.length = %d\n", ps.length, scales[0].length);
        // double[] scale = scales[0];
        // double[] offset = scales[1];
        double[] p = new double[ps.length];
        for (int i = 0; i < ps.length; i++) {
            p[i] = ps[i] * scales[0][i] + scales[1][i];
        }
        return p;
    }


    // -------------------------------------------------------------------------------------

    /**
     * Returns the optimized camera parameters.
     * @return the optimized camera parameters
     */
    public AbstractCamera getFinalCamera() {
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

    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------

    private class FullOptimizationModel implements MultivariateJacobianFunction {

        @Override
        public Pair<RealVector, RealMatrix> value(RealVector realVector) {
            double[] paramsS = realVector.toArray();
            RealVector Y = new ArrayRealVector(getValue(paramsS), false);
            RealMatrix J = new Array2DRowRealMatrix(getJacobian(paramsS), false);
            return Pair.create(Y, J);
        }
    }

    // -------------------------------------------------------------------------------------

    private class ReducedOptimizationModel implements MultivariateJacobianFunction {
        private final MultivariateJacobianFunction originalModel;
        private final int skipIndex;

        public ReducedOptimizationModel(int skipIndex) {
            this(new FullOptimizationModel(), skipIndex);
        }

        public ReducedOptimizationModel(MultivariateJacobianFunction originalModel, int skipIndex) {
            this.originalModel = originalModel;
            this.skipIndex = skipIndex;
        }

        @Override
        public Pair<RealVector, RealMatrix> value(RealVector point) {
            // 'point' has length N-1. We expand it to N for the original model.
            // double[] fullParams = new double[point.getDimension() + 1];
            // System.out.printf("ReducedOptimizationModel: fullParams.length = %d\n", fullParams.length);
            //
            // // expand parameters by 1 to full parameters and replace skipped parameter by 0:
            // int j = 0;
            // for (int i = 0; i < fullParams.length; i++) {
            //     if (i == skipIndex) {
            //         fullParams[i] = 0.0; // Hardcoded to zero
            //     } else {
            //         fullParams[i] = point.getEntry(j++);
            //     }
            // }

            double[] fullParams = insertItem(point.toArray(), skipIndex, 0);

            // let original model calculate (value, jacobian) from full parameters:
            Pair<RealVector, RealMatrix> result =
                    originalModel.value(MatrixUtils.createRealVector(fullParams));

            // Strip the column corresponding to skipIndex from the Jacobian
            RealMatrix fullJac = result.getSecond();
            RealMatrix reducedJac = MatrixUtils.createRealMatrix(fullJac.getRowDimension(), point.getDimension());

            int colCount = 0;
            for (int i = 0; i < fullJac.getColumnDimension(); i++) {
                if (i != skipIndex) {
                    reducedJac.setColumn(colCount++, fullJac.getColumn(i));
                }
            }

            return new Pair<>(result.getFirst(), reducedJac);
        }

    }

    // remove 1 item from
    static double[] skipItem(double[] original, int skipindex) {
        if (skipindex < 0 ||skipindex >= original.length) {
            throw new IllegalArgumentException("skipindex out of range: " + skipindex);
        }
        double[] result = new double[original.length - 1];
        int j = 0;
        for (int i = 0; i < original.length; i++) {
            if (i != skipindex) {
                result[j] = original[i];
                j++;
            }
        }
        return result;
    }

    static double[] insertItem(double[] original, int insertIndex, double value) {
        double[] expanded = new double[original.length + 1];
        int j = 0;
        for (int i = 0; i < expanded.length; i++) {
            if (i == insertIndex) {
                expanded[i] = value;
            } else {
                expanded[i] = original[j++];
            }
        }
        return expanded;
    }

    // -----------------

    RealMatrix getCovarianceMatrix(LeastSquaresOptimizer.Optimum optimum) {
        // Get the covariance matrix (in scaled space)
        RealMatrix cov = optimum.getCovariances(1e-12);
        int dim = cov.getRowDimension();
        RealMatrix correlation = MatrixUtils.createRealMatrix(dim, dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                double corr = cov.getEntry(i, j) /
                        (Math.sqrt(cov.getEntry(i, i)) * Math.sqrt(cov.getEntry(j, j)));
                correlation.setEntry(i, j, corr);
            }
        }
        // Print the matrix - look for values like 0.999 or -0.999
        return correlation;
    }

}
