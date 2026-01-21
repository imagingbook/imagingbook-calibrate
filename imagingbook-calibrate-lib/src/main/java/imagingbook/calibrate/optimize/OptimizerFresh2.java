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
 * TODO: add constraints to couple alpha/beta
 * add more constraints to avoid runaway from initial parameters
 * check J again, when views are included
 *
 * This version allows exclusion of parameters by reducing the system accordingly.
 * The model is still evaluated with ALL parameters, for those excluded the initial
 * values are kept fixed.
 *
 *
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
public class OptimizerFresh2 implements NonlinearOptimizer {
    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    private List<Integer> skipList = Arrays.asList(2);
    private boolean SKIP_CAMERA_PARAMS = false;
    private boolean SKIP_DISTORTION_PARAMS = false;
    private boolean SKIP_VIEW_PARAMS = true;
    final int effParameterCnt;                   // remaining parameters (non-skipped)

    static double ALMOST_ZERO = 1e-9;


    private final int[] paramSkipArray;             // parameter is skipped if skipArray[p] = -1
    // private int[] paramIndex;                   // [origParamIndex[q] = p (index in original parameters
    private final ArrayIndexMapper parameterIndexMapper;

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
    private final double[] initialParametersScaled;
    private final double[][] parameterScales;

    private final double[] observed;
    private final double[][] J;

    private LeastSquaresOptimizer.Optimum result;

    /**
     * The only constructor.
     * @param initCam initial {@link Camera} instance
     * @param viewList a list of M {@link ViewTransform} instances
     * @param modPntSet a list of M model point sets
     * @param obsPntSet a list of M sensor point sets
     */
    public OptimizerFresh2(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.M = obsPntSet.size();
        this.N = checkInputData(viewList, modPntSet, obsPntSet);

        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);

        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;

        // this.N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); //getTotalPointCount();

        // double[] camScales = {10000, 10000, 1, 3000, 2000};   // alpha, beta, gamma, uc, vc
        double alpha = initCam.getAlpha();
        double beta = initCam.getBeta();
        double uc = initCam.getUc();
        double vc = initCam.getVc();
        double[][] camScales = {    // alpha, beta, gamma, uc, vc
                {1, 1, 1, 0.1, 0.1}, // {2, 2, 2, 0.2, 0.2},     // scale
                {0, 0, 0, 0, 0}  // {alpha, beta,  0, uc, vc }     // offset
        };

        double[][] distScales = {
                { 0.002, 0.005}, //{ 0.005, 0.05},       // scale
                { 0, 0}        // offset
        }; //new double[initCam.getDistortionParameters().length];

        double[][] viewScales = {
                {.0005, .0005, .0005, .01, .01, .01}, //{.0005, .0005, .0005, .01, .01, .01},     // scale
                {0, 0, 0, 0, 0, 0}                  // offset
        };
        // double[] viewScales = {1, 1, 1, 1, 1, 1};
        this.parameterScales = makeParameterScales(camScales, distScales, viewScales, M);
        System.out.println("parameterScales  = " + Matrix.toString(parameterScales[0]));
        System.out.println("parameterOffsets = " + Matrix.toString(parameterScales[1]));

        this.initialParameters = makeInitialParameters();
        this.initialParametersScaled = scaleParameters(initialParameters, parameterScales);
        this.K = initialParameters.length;

        this.paramSkipArray = makeParamSkipArray();
        this.parameterIndexMapper = new ArrayIndexMapper(paramSkipArray);
        this.effParameterCnt = parameterIndexMapper.getReducedLength();// countEffParameters(paramSkipArray);
        // this.paramIndex = makeParamIndex(paramSkipArray);



        this.J = new double[2 * N][effParameterCnt];
        this.observed = makeObservedVector();

        System.out.println("skipArray  = " + Arrays.toString(paramSkipArray));
        System.out.println("effective parameters  = " + effParameterCnt);
        System.out.println(" estimateDelta(1) = " +estimateDelta(1));
    }

    // -------------------------------------------------------------------------------------------

    private int checkInputData(List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        int M = viewList.size();
        if (modPntSet.size() != M) {
            throw new IllegalArgumentException("modPntSet.size() != " + M);
        }
        if (obsPntSet.size() != M) {
            throw new IllegalArgumentException("obsPntSet.size() != " + M);
        }
        int pntCnt = 0;
        for (int k = 0; k < M; k++) {
            int nk = modPntSet.get(k).length;
            if (obsPntSet.get(k).length != nk) {
                throw new IllegalArgumentException("modPntSet.get(k).length != obsPntSet.get(k).length for k=" + k);
            }
            pntCnt += nk;
        }
        return pntCnt;
    }

    private int[] makeParamSkipArray() {
        int[] sa = new int[K];
        for (int i = 0; i < K; i++) {
            sa[i] = i;
        }

        int Pa = initCam.getLinearParameters().length;
        int Pd = initCam.getDistortionParameters().length;
        if (SKIP_CAMERA_PARAMS) {
            Arrays.fill(sa, 0, Pa, -1);
        }
        if (SKIP_DISTORTION_PARAMS) {
            int start = Pa;
            Arrays.fill(sa, start, start+Pd, -1);
        }
        if (SKIP_VIEW_PARAMS) {
            int start = Pa + Pd;
            Arrays.fill(sa, start, sa.length, -1);
        }

        for (int i :  skipList) {
            sa[i] = -1;
        }

        for (int i = 0, j = 0; i < K; i++) {
            if (sa[i] != -1) {
                sa[i] = j;
                j++;
            }
        }

        return sa;
    }

    // -------------------------------------------------------------------------------------

    /**
     * Extracts and returns the parameters for the camera (including distortion parameters)
     * from the full parameter vector.
     * @param parameters the full parameter vector
     * @return the camera parameters
     */
    double[] getCameraParameters(double[] parameters) {
        return Arrays.copyOfRange(parameters, 0, camParCount);
    }

    /**
     * Extracts and returns the (6) parameters for view transformation {@code k}
     * from the full parameter vector.
     * @param parameters the full paraneter vector
     * @param k the view number
     * @return the view parameters
     */
    double[] getViewParameters(double[] parameters, int k) {
        int startPos = getViewParameterPos(k, 0);
        return Arrays.copyOfRange(parameters, startPos, startPos + viewParCount);
    }

    int getViewParameterPos(int k, int i) {
        return camParCount + k * viewParCount + i;
    }

    // -------------------------------------------------------------------------------------

    /**
     * Calculates values Y predicted by the model.
     * Called by the optimizer with reduced and scaled(!) parameters.
     * @param paramsRS reduced and scaled parameters
     * @return
     */
    @Deprecated
    double[] getValue(double[] paramsRS) {
        // expand to scaled parameters:
        double[] paramsS = expandParams(paramsRS, initialParametersScaled);
        // convert to unscaled parameters:
        double[] params = unscaleParameters(paramsS, parameterScales);
        // System.out.println("getValue(): paramsRS = " + Matrix.toString(paramsRS));
        // System.out.println("getValue(): pRS = " + Matrix.toString(paramsRS));
        // System.out.println("getValue(): pS  = " + Matrix.toString(paramsS));
        // System.out.println("getValue(): pU  = " + Matrix.toString(params));

        // create a new Camera instance:
        Camera cam = initCam.fromParameters(getCameraParameters(params));
        double[] V = new double[2 * N];             // new value vector
        int r = 0;
        // process each view
        for (int k = 0; k < M; k++) {
            // create transform for view k
            ViewTransform Vk = new ViewTransform(getViewParameters(params, k));
            // project each point of view k to sensor:
            for (int i = 0; i < modPts[k].length; i++, r+=2) {
                double[] uv = cam.project(Vk, modPts[k][i]);
                V[r + 0] = uv[0];
                V[r + 1] = uv[1];
            }
        }

        double[] resid = Matrix.subtract(observed, V);
        // System.out.println("OverallNonlinearOptimizer: Y = " + Matrix.toString(Arrays.copyOf(Y, 20)));
        // System.out.println("OverallNonlinearOptimizer: R = " + Matrix.toString(Arrays.copyOf(resid, 20)));
        // System.out.println("OverallNonlinearOptimizer: |R1| = " + Matrix.normL2(resid));
        // System.out.println("OverallNonlinearOptimizer: |Rgamma| = " + resid[resid.length-1]);
        return V;
    }

    // ------------------------------------

    /**
     * Calculates the Jacobian for the model at the current point.
     * Called by the optimizer with reduced and scaled(!) parameters.
     * The column dimension of J equals the number of non-skipped parameters.
     * Skipped parameters are ignored.
     * @param paramsRS reduced and scaled parameters
     * @return
     */
    @Deprecated
    double[][] getJacobian(double[] paramsRS) {
        // get full/scaled parameters
        double[] Yref = getValue(paramsRS);                 // values Y from undisturbed parameters
        // expand to scaled parameters:
        double[] paramsS = expandParams(paramsRS, initialParametersScaled);
        // convert to unscaled parameters:
        double[] params = unscaleParameters(paramsS, parameterScales);

        // create a camera for the current parameter point
        double[] pc = getCameraParameters(params);
        Camera camCur = initCam.fromParameters(pc);

        // clear recycled Jacobian matrix
        for (int i = 0; i < J.length; i++) {
            Arrays.fill(J[i], 0.0);
        }

        // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
        for (int p = 0; p < pc.length; p++) {    // for all camera parameters
            int col = parameterIndexMapper.getReducedPos(p);
            if (col >= 0) {
                // update J for non-skipped parameter p
                double pcp = pc[p];                               // keep current parameter value
                double delta = estimateDelta(pcp);
                pc[p] = pc[p] + delta;        // modify parameter p
                Camera camMod = camCur.fromParameters(pc);    // modified camera
                // project all model points through the modified camera:
                for (int k = 0, r = 0; k < M; k++) {            // for all views k, r = row
//                    int q = camParCount + k * viewParCount;     // q = start in parameter vector
//                    double[] wk = Arrays.copyOfRange(params, q, q + viewParCount);
//                    ViewTransform Vk = new ViewTransform(wk);
                    ViewTransform Vk = new ViewTransform(getViewParameters(params, k));
                    for (int j = 0; j < modPts[k].length; j++, r += 2) {
                        // project all points of view k to sensor:
                        double[] Ymod = camMod.project(Vk, modPts[k][j]);
                        J[r + 0][col] = (Ymod[0] - Yref[r + 0]) / delta;   // dX
                        J[r + 1][col] = (Ymod[1] - Yref[r + 1]) / delta;   // dY
                    }
                }
                pc[p] = pcp;    // revert parameter a[p] to original value
            }
        }

        // Step 2: calculate the diagonal blocks, one for each view
        int startRow = 0;
        for (int k = 0; k < M; k++) {    // for all views/blocks
            // int ps = camParCount + k * viewParCount;    // start of view parameters = start column in J
            // double[] w = Arrays.copyOfRange(params, ps, ps + viewParCount);
            double[] w = getViewParameters(params, k);
            // int c = a.length + k * w.length;        // leftmost matrix column of block i
            for (int i = 0; i < w.length; i++) {    // for all parameters in w
                //int p = ps + i;
                int p = getViewParameterPos(k, i);
                int col = parameterIndexMapper.getReducedPos(p);
                if (col >= 0) {                             // don't skip this parameter
                    double wi = w[i];                       // keep current parameter value w[i]
                    double delta = estimateDelta(wi);
                    w[i] = w[i] + delta;                    // modify parameter w[i]
                    ViewTransform Vk = new ViewTransform(w);
                    // fill column of J in diagonal block k:
                    for (int j = 0, r = startRow; j < modPts[k].length; j++, r+=2) {        // for all model points: calculate disturbed value
                        double[] Ymod = camCur.project(Vk, modPts[k][j]);
                        J[r + 0][col] = (Ymod[0] - Yref[r + 0]) / delta;   // dX
                        J[r + 1][col] = (Ymod[1] - Yref[r + 1]) / delta;   // dY
                    }
                    w[i] = wi; // revert parameter w[i] to its original value
                }
            }
            startRow = startRow + 2 * modPts[k].length;
        }

        printJacobian(J);

        // scale J back to optimizer scale
        for (int p = 0; p < params.length; p++) {
            int col = parameterIndexMapper.getReducedPos(p);
            if (col >= 0) { // non-skipped parameter
                // multiply column J[*][p] by scale[p]:
                double s = parameterScales[0][p];
                // System.out.printf("  --- scaling J[%d] by %.4f\n", col, s);
                for (int j = 0; j < J.length; j++) {
                    J[j][col] *= s;
                }
            }
        }

        // printJacobian(J);

        double[] colNorms = getMatrixColumnNorms(J);
        System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));
        System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
        System.out.println("    J rank = " + getMatrixRank(J));
        System.out.println("    JTJ condition number = " + getJtJconditionNumber(J));
        return J;
    }


    void printJacobian(double[][] JAC) {
        // print first rows of each Jacobian block --------------------------
        int rowsToPrint = 4;
        int startR = 0;
        int m = modPts.length;
        System.out.println("JACOBIAN with " + JAC.length + " + rows, " + JAC[0].length + " columns");
        PrintPrecision.set(3);
        for (int k = 0; k < m; k++) {
            System.out.println("J block " + k + " with rows " + 2*modPts[k].length + " ----------------------------");
            for (int j = 0; j < rowsToPrint; j++) {
                int r = startR + j;
                System.out.printf("    Js[%d] = %s\n", r, Matrix.toString(JAC[r]));
            }
            System.out.println("    ...");
            for (int j = 0; j < rowsToPrint; j++) {
                int r = startR + 2 * modPts[k].length - rowsToPrint + j;
                System.out.printf("    Js[%d] = %s\n", r, Matrix.toString(JAC[r]));
            }
            startR = startR + 2 * modPts[k].length;
        }
        // -------------------------------------------------------------------
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
        System.out.println("initialParameters (unscaled) = " + Matrix.toString(initialParameters));
        double[] ips = scaleParameters(initialParameters, parameterScales);
        System.out.println("initialParameters (scaled)   = " + Matrix.toString(ips));
        double[] ipu = unscaleParameters(ips, parameterScales);
        System.out.println("initialParameters (unscaled) = " + Matrix.toString(ipu));

        // MultivariateJacobianFunction model = new FullOptimizationModel();
        MultivariateJacobianFunction model = new CombinedModel();

        double[] pStart = reduceParams(scaleParameters(initialParameters, parameterScales));
        System.out.println("start Parameters = " + Matrix.toString(pStart));

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(observed)
                .model(model)
                .start(pStart)
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(100);
        // System.out.println("LevenbergMarquardtOptimizer: InitialStepBoundFactor = " + lm.getInitialStepBoundFactor());
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        this.result = result;
        double[] optParams = result.getPoint().toArray();
        PrintPrecision.set(8);
        System.out.println("optimal parameters (reduced) = " + Matrix.toString(optParams));
        // double[] fullParams = insertItem(optParams, PARAMETER_SKIP_INDEX, 0.0);
        double[] fullParams = expandParams(optParams, initialParameters);
        System.out.println("scaled optimal parameters (full)   = " + Matrix.toString(fullParams));
        double[] unscaledParams = unscaleParameters(fullParams, parameterScales);
        System.out.println("unscaled optimal parameters (full) = " + Matrix.toString(unscaledParams));
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
            // double[] w = initViews[k].getParameters();  // use intial values as offsets
            // System.arraycopy(w, 0, offsets, start, viewParCount);
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

    @Deprecated
    private class FullOptimizationModel implements MultivariateJacobianFunction {

        @Override
        public Pair<RealVector, RealMatrix> value(RealVector realVector) {
            double[] paramsS = realVector.toArray();
            RealVector Y = new ArrayRealVector(getValue(paramsS), false);
            RealMatrix J = new Array2DRowRealMatrix(getJacobian(paramsS), false);
            return Pair.create(Y, J);
        }
    }

    // ----------------------------

    class CombinedModel implements MultivariateJacobianFunction {

        @Override
        public Pair<RealVector, RealMatrix> value(RealVector point) {
            double[] paramsRS = point.toArray();
            // expand to scaled parameters:
            double[] paramsS = expandParams(paramsRS, initialParametersScaled);
            // convert to unscaled parameters:
            double[] params = unscaleParameters(paramsS, parameterScales);

            // create a new Camera instance:
            double[] pc = getCameraParameters(params);
            Camera cam = initCam.fromParameters(pc);

            // calculate value vector (V) ----------------------------------------------

            double[] Y = new double[2 * N];             // new value vector
            // process each view
            for (int k = 0, r = 0; k < M; k++) {
                // create transform for view k
                ViewTransform Vk = new ViewTransform(getViewParameters(params, k));
                // project each point of view k to sensor:
                for (int i = 0; i < modPts[k].length; i++, r+=2) {
                    double[] uv = cam.project(Vk, modPts[k][i]);
                    Y[r + 0] = uv[0];
                    Y[r + 1] = uv[1];
                }
            }

            // calculate Jacobian matrix (J) ----------------------------------------------

            // clear recycled Jacobian matrix (probably not needed)
            for (int i = 0; i < J.length; i++) {
                Arrays.fill(J[i], 0.0);
            }

            // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
            for (int p = 0; p < camParCount; p++) {                     // for all camera parameters
                int col = parameterIndexMapper.getReducedPos(p);
                if (col >= 0) {
                    // update J for non-skipped parameter p
                    double pcp = pc[p];                               // keep current parameter value
                    double delta = estimateDelta(pcp);
                    pc[p] = pc[p] + delta;        // modify parameter p
                    Camera camMod = cam.fromParameters(pc);    // modified camera
                    // project all model points through the modified camera:
                    for (int k = 0, r = 0; k < M; k++) {            // for all views k, r = row
                        ViewTransform Vk = new ViewTransform(getViewParameters(params, k));
                        for (int j = 0; j < modPts[k].length; j++, r += 2) {
                            // project all points of view k to sensor:
                            double[] Ymod = camMod.project(Vk, modPts[k][j]);
                            J[r + 0][col] = (Ymod[0] - Y[r + 0]) / delta;   // dX
                            J[r + 1][col] = (Ymod[1] - Y[r + 1]) / delta;   // dY
                        }
                    }
                    pc[p] = pcp;    // revert parameter a[p] to original value
                }
            }

            // Step 2: calculate the diagonal blocks, one for each view
            for (int k = 0, startRow = 0; k < M; k++) {    // for all views/blocks
                double[] w = getViewParameters(params, k);
                // nudge each parameter in w
                for (int i = 0; i < w.length; i++) {
                    int p = getViewParameterPos(k, i);
                    int col = parameterIndexMapper.getReducedPos(p);
                    if (col >= 0) {                             // don't skip this parameter
                        double wi = w[i];                       // keep current parameter value w[i]
                        double delta = estimateDelta(wi);
                        w[i] = w[i] + delta;                    // modify parameter w[i]
                        ViewTransform Vi = new ViewTransform(w);
                        // in diagonal block k of J, fill column col:
                        // for all model points: calculate disturbed output  value
                        for (int j = 0, r = startRow; j < modPts[k].length; j++, r+=2) {
                            double[] Ymod= cam.project(Vi, modPts[k][j]);
                            J[r + 0][col] = (Ymod[0] - Y[r + 0]) / delta;   // dX
                            J[r + 1][col] = (Ymod[1] - Y[r + 1]) / delta;   // dY
                        }
                        w[i] = wi; // revert parameter w[i] to its original value
                    }
                }
                startRow += 2 * modPts[k].length;
            }

            printJacobian(J);

            // scale J back to optimizer scale
            for (int p = 0; p < params.length; p++) {
                int col = parameterIndexMapper.getReducedPos(p);
                if (col >= 0) { // non-skipped parameter
                    // multiply column J[*][p] by scale[p]:
                    double s = parameterScales[0][p];
                    // System.out.printf("  --- scaling J[%d] by %.4f\n", col, s);
                    for (int j = 0; j < J.length; j++) {
                        J[j][col] *= s;
                    }
                }
            }

            double[] colNorms = getMatrixColumnNorms(J);
            System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));
            System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
            System.out.println("    J rank = " + getMatrixRank(J));
            System.out.println("    JTJ condition number = " + getJtJconditionNumber(J));

            // prepare return values
            RealVector YY = new ArrayRealVector(Y, false);
            RealMatrix JJ = new  Array2DRowRealMatrix(J, false);
            return Pair.create(YY, JJ);
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
            // System.out.println("JTJ:\n" + Matrix.toString(JTJ));
            // showOnce = false;
        }
        RealMatrix JTJD = JTJ.add(D);
        return Matrix.getConditionNumber(JTJD);
    }

    // ----------------------------------------------------------------------

    double[] reduceParams(double[] fullParams) {
        if (fullParams.length != parameterIndexMapper.getFullLength()) {
            throw new IllegalArgumentException("Full parameters: wrong length " + fullParams.length);
        }
        double[] rp = new double[parameterIndexMapper.getReducedLength()];
        for (int q = 0; q < rp.length; q++) {
            int p = parameterIndexMapper.getFullPos(q);
            rp[q] = fullParams[p];
        }
        return rp;
    }

    double[] expandParams(double[] redParams, double[] fullParams) {
        if (redParams.length != parameterIndexMapper.getReducedLength()) {
            throw new IllegalArgumentException("Reduced parameters: wrong length " + redParams.length);
        }
        if (fullParams.length != parameterIndexMapper.getFullLength()) {
            throw new IllegalArgumentException("Full parameters: wrong length " + fullParams.length);
        }
        double[] fp = fullParams.clone();
        // insert from reduced parameters:
        for (int i = 0; i < redParams.length; i++) {
            int j = parameterIndexMapper.getFullPos(i);
            fp[j] = redParams[i];
        }
        return fp;
    }

    // ----------------------------------------------------------------------

}
