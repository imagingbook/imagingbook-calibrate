/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize.obsolete;

import imagingbook.calibrate.extrinsics.ViewTransform;
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
import org.apache.commons.math4.legacy.linear.DiagonalMatrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;

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
public class OptimizerFresh1 implements NonlinearOptimizer {

    private boolean SKIP_CAMERA_PARAMS = false;
    private boolean SKIP_DISTORTION_PARAMS = false;
    private boolean SKIP_VIEW_PARAMS = false;
    static double ALMOST_ZERO = 1e-9;

    private List<Integer> skipList = Arrays.asList(2);
    private final boolean[] skipArray;

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    final Pnt2d[][] modPts;
    private final Pnt2d[][] obsPts;
    final int M;                // number of views
    final int N;                // total number of observed points
    private final int K;        // total number of parameters
    final int camParCount;      // number of camera parameters (7+)
    final int viewParCount;     // number of view parameters (6)

    private final Camera initCam;
    private Camera finalCamera;
    private final ViewTransform[] initViews;
    private ViewTransform[] finalViews;
    private final double[] initialParameters;
    private final double[][] parameterScales;

    private final double[] observed;
    private final double[][] J;


    private LeastSquaresOptimizer.Optimum result;


    public OptimizerFresh1(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;
        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
        this.M = obsPntSet.size();
        this.N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); //getTotalPointCount();
        System.out.println("OptimizerFresh1: N = " + N);
        System.out.println("OptimizerFresh1: 2N = " + 2*N);

        // double[] camScales = {10000, 10000, 1, 3000, 2000};   // alpha, beta, gamma, uc, vc
        double alpha = initCam.getAlpha();
        double beta = initCam.getBeta();
        double uc = initCam.getUc();
        double vc = initCam.getVc();
        double[][] camScales = {    // alpha, beta, gamma, uc, vc
                {2, 2, 2, 0.2, 0.2},     // scale
                {alpha, beta,  0, uc, vc }     // offset
        };

        double[][] distScales = {
                { 0.005, 0.05},       // scale
                {   0,   0 }        // offset
        }; //new double[initCam.getDistortionParameters().length];

        double[][] viewScales = {
                {0.0002, 0.0002, 0.0002, 0.05, 0.05, 0.05},     // scale
                {0, 0, 0, 0, 0, 0}                  // offset
        };
        // double[] viewScales = {1, 1, 1, 1, 1, 1};
        this.parameterScales = makeParameterScales(camScales, distScales, viewScales, M);

        System.out.println("parameterScales  = " + Matrix.toString(parameterScales[0]));
        System.out.println("parameterOffsets = " + Matrix.toString(parameterScales[1]));

        this.initialParameters = makeInitialParameters();
        this.K = initialParameters.length;
        // this.J = new double[2 * N + 1][K];  // extra row for gamma penalty
        this.J = new double[2 * N][K];  // no gamma penalty

        this.observed = makeObservedVector();
        this.skipArray = makeSkipArray();
        System.out.println("skipArray  = " + Arrays.toString(skipArray));
    }

    private boolean[] makeSkipArray() {
        boolean[] sa = new boolean[K];
        int Pa = initCam.getLinearParameters().length;
        int Pd = initCam.getDistortionParameters().length;
        if (SKIP_CAMERA_PARAMS) {
            Arrays.fill(sa, 0, Pa, true);
        }
        if (SKIP_DISTORTION_PARAMS) {
            int start = Pa;
            Arrays.fill(sa, start, start+Pd, true);
        }
        if (SKIP_VIEW_PARAMS) {
            int start = Pa + Pd;
            Arrays.fill(sa, start, sa.length, true);
        }

        for (int i :  skipList) {
            sa[i] = true;
        }
        return sa;
    }

    // -------------------------------------------------------------------------------------

    double[] getValue(double[] paramsS) {
        double[] params = unscaleParameters(paramsS, parameterScales);
        System.out.println("OverallNonlinearOptimizer: pS = " + Matrix.toString(paramsS));
        System.out.println("OverallNonlinearOptimizer: pU = " + Matrix.toString(params));
        double[] a = Arrays.copyOfRange(params, 0, camParCount);
        Camera cam = initCam.fromParameters(a);
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


        double[] resid = Matrix.subtract(observed, V);
        // System.out.println("OverallNonlinearOptimizer: Y = " + Matrix.toString(Arrays.copyOf(Y, 20)));
        // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));
        System.out.println("OverallNonlinearOptimizer: |R1| = " + Matrix.normL2(resid));
        // System.out.println("OverallNonlinearOptimizer: |Rgamma| = " + resid[resid.length-1]);
        return V;
    }

    // ------------------------------------

    double[][] getJacobian(double[] paramsS) {
        // double[] uvRef = valueFun.value(paramsS);      // values from undisturbed parameters
        double[] params = unscaleParameters(paramsS, parameterScales);
        double[] uvRef = getValue(paramsS);      // values from undisturbed parameters

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
                for (int j = 0; j < modPts[k].length; j++, r+=2) {    // for all model points: calculate disturbed value
                    Pnt2d Pj = modPts[k][j];
                    double[] uvMod = camMod.project(viewk, Pj);
                    J[r + 0][p] = (uvMod[0] - uvRef[r + 0]) / delta;   // dX
                    J[r + 1][p] = (uvMod[1] - uvRef[r + 1]) / delta;   // dY
                }
            }
            a[p] = ap;    // revert parameter a[p] to original value
        }

        // Step 2: calculate the diagonal blocks, one for each view
        int startRow = 0;
        for (int k = 0; k < M; k++) {    // for all views/blocks
            final int start = camParCount + k * viewParCount;
            double[] w = Arrays.copyOfRange(params, start, start + viewParCount);
            final int c = a.length + k * w.length;        // leftmost matrix column of block i
            for (int p = 0; p < w.length; p++) {    // for all parameters in w
                double wp = w[p];                    // keep original parameter value w[p]
                double delta = estimateDelta(wp);
                w[p] = w[p] + delta;                // modify parameter w_k
                ViewTransform view = new ViewTransform(w);
                // fill column of J in diagonal block k:
                for (int j = 0, r = startRow; j < modPts[k].length; j++, r+=2) {        // for all model points: calculate disturbed value
                    System.out.println("     OptimizerFresh1: k=" + k + " p=" + p + " j=" + j + " r=" + r);
                    Pnt2d Pj = modPts[k][j];
                    double[] uvMod = camOrig.project(view, Pj);
                    J[r + 0][c + p] = (uvMod[0] - uvRef[r + 0]) / delta;   // dX
                    J[r + 1][c + p] = (uvMod[1] - uvRef[r + 1]) / delta;   // dY
                }
                w[p] = wp; // revert parameter w[p] to its original value
            }
            startRow = startRow + modPts[k].length;
        }

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

        // zero columns for parameters being skipped
        for (int p = 0; p < skipArray.length; p++) {
            if (skipArray[p]) {
                for (int j = 0; j < J.length; j++) {
                    J[j][p] = ALMOST_ZERO;
                }
            }
        }

        // print first two rows of Jacobian
        for (int j = 0; j < 2; j++) {
            System.out.printf("    Js[%d] = %s\n", j, Matrix.toString(J[j]));
        }
        // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));

        double[] colNorms = getMatrixColumnNorms(J);
        System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));
        // System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
        System.out.println("    J rank = " + getMatrixRank(J));
        System.out.println("    JTJ condition number = " + getJtJconditionNumber(J));
        return J;
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
        MultivariateJacobianFunction model = new FullOptimizationModel();

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(observed)
                .model(model)
                .start(scaleParameters(initialParameters, parameterScales))
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(100);
        System.out.println("LevenbergMarquardtOptimizer: InitialStepBoundFactor = " + lm.getInitialStepBoundFactor());
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        this.result = result;
        double[] optParams = result.getPoint().toArray();
        PrintPrecision.set(8);
        System.out.println("optimal parameters (reduced) = " + Matrix.toString(optParams));
        // double[] fullParams = insertItem(optParams, PARAMETER_SKIP_INDEX, 0.0);
        double[] fullParams = optParams;
        System.out.println("optimal parameters (full)    = " + Matrix.toString(fullParams));
        double[] unscaledParams = unscaleParameters(fullParams, parameterScales);
        System.out.println("unscaled parameters (full)   = " + Matrix.toString(unscaledParams));
        updateEstimates(unscaledParams);

        // PrintPrecision.set(3);
        // System.out.println("Covariance Matrix: \n" + Matrix.toString(getCovarianceMatrix(result)));
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
    double[][] makeParameterScales(double[][] camScales, double[][] distScales, double[][] viewScales, int viewCnt) {
        if (camScales[0].length != camScales[1].length) {
            throw new IllegalArgumentException("camScales not rectangular");
        }
        if (distScales[0].length != distScales[1].length) {
            throw new IllegalArgumentException("distScales not rectangular");
        }
        if (viewScales[0].length != viewScales[1].length) {
            throw new IllegalArgumentException("viewScales not rectangular");
        }

        int camParCount =  camScales[0].length;
        int distParCount = distScales[0].length;
        int viewParCount = viewScales[0].length;
        int P = camParCount + distParCount + viewCnt * viewParCount;

        double[] scales = new double[P];
        double[] offsets = new double[P];

        Arrays.fill(scales, 1); //GLOBAL_PARAMETER_SCALE);
        Arrays.fill(scales, 0);

        int start = 0;
        System.arraycopy(camScales[0], 0, scales, start, camParCount);
        System.arraycopy(camScales[1], 0, offsets, start, camParCount);
        start += camParCount;
        System.arraycopy(distScales[0], 0, scales, start, distParCount);
        System.arraycopy(distScales[1], 0, offsets, start, distParCount);
        start += distParCount;
        for (int k = 0; k < viewCnt; k++) {
            System.arraycopy(viewScales[0], 0, scales, start, viewParCount);
            double[] w = initViews[k].getParameters();  // use intial values as offsets
            System.arraycopy(w, 0, offsets, start, viewParCount);
            // System.arraycopy(viewScales[1], 0, offsets, start, viewParCount);
            start += viewParCount;
        }

        // Arrays.fill(scale, 1.0);
        // Arrays.fill(offsets, 0);
        return new double[][] {scales, offsets};
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
        // return cov;
    }

    int getMatrixRank(double[][] data) {
        RealMatrix matrix = new Array2DRowRealMatrix(data);

        // 2. Perform SVD
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

        // 3. Get the rank
        return svd.getRank();
    }

    double[] getMatrixColumnNorms(double[][] J) {
        double[] colNorms = new double[J[0].length];
        RealMatrix JR = new Array2DRowRealMatrix(J, false);
        for (int j = 0; j < colNorms.length; j++) {
            colNorms[j] = JR.getColumnVector(j).getNorm();
        }
        return colNorms;
    }

    boolean showOnce = true;

    double getJtJconditionNumber(double[][] data) {
        double[] colNorms = getMatrixColumnNorms(data);
        RealMatrix D = new DiagonalMatrix(colNorms);
        RealMatrix J = new Array2DRowRealMatrix(data);
        RealMatrix JTJ = J.transpose().multiply(J);
        if (showOnce) {
            System.out.println("JTJ:\n" + Matrix.toString(JTJ));
            showOnce = false;
        }
        RealMatrix JTJD = JTJ.add(D);
        return Matrix.getConditionNumber(JTJD);
    }
}
