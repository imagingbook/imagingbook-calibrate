/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.optimize.obsolete.NonlinearOptimizer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import imagingbook.common.util.SubsequenceMapping;
import imagingbook.common.util.bits.BitVector;
import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.exception.TooManyEvaluationsException;
import org.apache.commons.math4.legacy.exception.TooManyIterationsException;
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
public class OverallOptimizer implements NonlinearOptimizer {
    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    private static boolean FIX_LINCAMERA_PARAMS = false;
    private static boolean FIX_DISTORTION_PARAMS = false;
    private static boolean FIX_VIEW_PARAMS = false;
    private static List<Integer> FIX_SINGLE_PARAMS = Arrays.asList();

    private final BitVector variableParamFlags;
    private int variableParamCnt;

    private final Camera initCam;
    private final Pnt2d[][] modPts;
    private final Pnt2d[][] obsPts;
    private final ViewTransform[] initViews;
    private final double[] observed;

    private final int M;                // number of views
    private final int N;                // total number of observed points
    private final int K;                // total number of parameters

    private final double[] initialParameters;
    //private final double[] parameterScales;

    private final ParameterAssembler assembler;
    private ParameterAdapter adapter;

    // optimization results:
    private LeastSquaresOptimizer.Optimum result;
    private Camera finalCamera;
    private ViewTransform[] finalViews;

    private String failureReason;

    /**
     * The only constructor.
     * @param initCam initial {@link Camera} instance
     * @param viewList a list of M {@link ViewTransform} instances
     * @param modPntSet a list of M model point sets
     * @param obsPntSet a list of M sensor point sets
     */
    public OverallOptimizer(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.M = obsPntSet.size();
        this.N = checkAndCount(viewList, modPntSet, obsPntSet);
        this.initViews = viewList.toArray(new ViewTransform[0]);
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
        this.observed = makeObservedVector();

        this.assembler = new ParameterAssembler(initCam, M);
        this.initialParameters =  assembler.assembleParameters(initCam, viewList);     // makeInitialParameters();
        this.K = initialParameters.length;
        // System.out.println("initialParameters  = " + Matrix.toString(initialParameters));
        // ---------------------------
        this.variableParamFlags = new BitVector(K);   // initially all parameters are active (non-fixed)
        this.variableParamFlags.setAll();             // to be modified by subsequent fixParameters() calls
    }

    // -------------------------------------------------------------------------------------

    /**
     * Called by optimize. Performs all remaining setup tasks after optimizer has been
     * fully configures, e.g. fixing certain parameters.
     * Finishes all setups not completed by constructor.
     * It is an error if setup() is called more than once.
     */
    private void setup() {
        // System.out.println("activeParamFlags = " + activeParamFlags);
        this.adapter = new ParameterAdapter(variableParamFlags, null);    // all scales = 1
        this.variableParamCnt = variableParamFlags.cardinality();     // adapter.getSubsequenceLength();

        double[] autoScales = getAutoScales(initialParameters);
        PrintPrecision.set(6);
        // System.out.println("autoScales = " + Matrix.toString(autoScales));
        this.adapter.setScales(autoScales);

        // System.out.printf("problem size = %d x %d\n", 2*N, activeParameterCnt);
    }

    // -------------------------------------------------------------------------------------------

    private static int checkAndCount(List<ViewTransform> viewList, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
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

    // -------------------------------------------------------------------------------------

    public void fixParameter(int p) {
        this.variableParamFlags.unsetBit(p);
    }

    public void fixParameters(List<Integer> params) {
        for (int p : params) {
            fixParameter(p);
        }
    }

    public void fixGamma() {
        fixParameter(2);
    }

    public void fixPrincipalPoint() {
        fixParameter(3);
        fixParameter(4);
    }

    public void fixLinearCameraParameters() {
        for (int p = 0; p < 5; p++) {       // TODO
            fixParameter(p);
        }
    }

    public void fixDistortionParameters() {
        int n = initCam.getDistortionParameters().length;
        for (int p = 5; p < 5 + n; p++) {
            fixParameter(p);
        }
    }

    public void fixViewParameters(int k) {
        for (int p = 0; p < 6; p++) {
            fixParameter(assembler.getViewParameterPos(k, p));
        }
    }



    public void fixViewParameters() {
        for (int k = 0; k < modPts.length; k++) {
            fixViewParameters(k);
        }
    }


    // -------------------------------------------------------------------------------------

    /**
     * Performs Levenberg-Marquardt non-linear optimization to get better estimates of the
     * parameters.
     *
     * @return
     */
    public boolean optimize() {
        setup();
        MultivariateJacobianFunction model = new CombinedModel(2 * N, variableParamCnt);
        double[] pStart = adapter.getModelParameters(initialParameters);
        // System.out.println("start Parameters = " + Matrix.toString(pStart));

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(observed)
                .model(model)
                .start(pStart)
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();
        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(100);
        // System.out.println("LevenbergMarquardtOptimizer: InitialStepBoundFactor = " + lm.getInitialStepBoundFactor());

        LeastSquaresOptimizer.Optimum result;
        try {
            result = lm.optimize(problem);
        }
        catch (TooManyIterationsException | TooManyEvaluationsException e) {
            failureReason = "maximum number of iterations or evaluations exceeded";
            return false;
        }

        this.result = result;
        double[] optParams = result.getPoint().toArray();
        PrintPrecision.set(8);

        double[] finalParams = adapter.getFullParameters(optParams, initialParameters);
        // System.out.println("unscaled optimal parameters (full) = " + Matrix.toString(finalParams));
        updateEstimates(finalParams);

        // PrintPrecision.set(3);
        // System.out.println("Covariance Matrix: \n" + Matrix.toString(getCovarianceMatrix(result)));
        return true;
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Creates and returns a vector of scales for all optimization parameters.
     * @param camScales the scale factors for
     * @param distScales scales for distortion parameters
     * @param viewScales scales for view transform parameters
     * @param viewCnt number of views
     * @return a vector scale values for all parameters
     */   // TODO: revise to use initial Camera to obtain default scale values
    double[] makeParameterScales(double[] camScales, double[] distScales, double[] viewScales, int viewCnt) {
        int camLinParCount =  camScales.length;
        int camDistParCount = distScales.length;
        int viewParCount = viewScales.length;
        int P = camLinParCount + camDistParCount + viewCnt * viewParCount;

        double[] scales = new double[P];
        double[] offsets = new double[P];

        Arrays.fill(scales, 1); //GLOBAL_PARAMETER_SCALE);
        Arrays.fill(scales, 0);

        int start = 0;
        System.arraycopy(camScales, 0, scales, start, camLinParCount);
        start += camLinParCount;
        System.arraycopy(distScales, 0, scales, start, camDistParCount);
        start += camDistParCount;
        for (int k = 0; k < viewCnt; k++) {
            System.arraycopy(viewScales, 0, scales, start, viewParCount);
            // double[] w = initViews[k].getParameters();  // use intial values as offsets
            // System.arraycopy(w, 0, offsets, start, viewParCount);
            // System.arraycopy(viewScales[1], 0, offsets, start, viewParCount);
            start += viewParCount;
        }

        // Arrays.fill(scale, 1.0);
        // Arrays.fill(offsets, 0);
        return scales;
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
        finalCamera = initCam.withParameters(
                assembler.getLinearCameraParameters(params),
                assembler.getDistortionParameters(params));
        finalViews = new ViewTransform[M];

        for (int k = 0; k < M; k++) {
            double[] wk = assembler.getViewParameters(params, k);
            finalViews[k] = new ViewTransform(wk);
        }
    }

    // Parameter scaling --------------------------------------------------------------------

    double[] getAutoScales(double[] allParams) {
        MultivariateJacobianFunction model = new CombinedModel(2 * N, variableParamCnt);
        double[] pStart = adapter.getModelParameters(allParams);
        RealMatrix Jac = model.value(new ArrayRealVector(pStart, false)).getSecond();
        double[] scales = new double[allParams.length];
        Arrays.fill(scales, 1.0);

        for (int q = 0; q < variableParamCnt; q++) {
            double norm = Jac.getColumnVector(q).getNorm();
            double s = (norm < 1e-12) ? 1.0 : (1.0 / norm);
            int p = adapter.getFullParamIdx(q);
            scales[p] = s;
        }
        return scales;
    }


    /**
     * Sets the scales for the parameters of one view transform
     * @param scales
     * @param k view number
     */
    private void setViewParameterScales(double[] scales, int k) {

    }

    // ----------------------------------------------------------------------------------------

    /**
     * Helps to assemble and disassemble parameter vectors.
     */
    static class ParameterAssembler {

        private final Camera cam;
        private final int viewCount;
        private final int camLinParamCount = 5;
        private final int viewParamCount = ViewTransform.PARAMETER_COUNT;
        private final int camDistParamCount;
        private final int paramCnt;

        ParameterAssembler(Camera cam, int viewCount) {
            this.cam = cam;
            this.viewCount = viewCount;
            this.camDistParamCount = cam.getDistortion().getParameterCount();
            this.paramCnt = camLinParamCount + camDistParamCount + viewCount * viewParamCount;
        }

        double[] assembleParameters(Camera cam, List<ViewTransform> views) {
            if (viewCount != views.size()) {
                throw new IllegalArgumentException("view count does not match list size: " + views.size());
            }
            double[] params = new double[paramCnt];
            double[] cp = cam.getParameters();
            System.arraycopy(cp, 0, params, 0, cp.length);
            // insert M view parameters:
            int start = cp.length;
            for (ViewTransform V : views) {
                double[] w = V.getParameters();
                System.arraycopy(w, 0, params, start, w.length);
                start = start + w.length;
            }
            return params;
        }

        int getCameraParamCount() {
            return camLinParamCount + camDistParamCount;
        }

        double[] getLinearCameraParameters(double[] params) {
            return Arrays.copyOfRange(params, 0, 5);
        }

        double[] getDistortionParameters(double[] params) {
            int n = cam.getDistortion().getParameterCount();
            return Arrays.copyOfRange(params, 5, 5 + n);
        }

        double[] getCameraParameters(double[] parameters) {
            return Matrix.join(getLinearCameraParameters(parameters), getDistortionParameters(parameters));
        }

        double[] getViewParameters(double[] parameters, int k) {
            int startPos = getViewParameterPos(k, 0);
            return Arrays.copyOfRange(parameters, startPos, startPos + viewParamCount);
        }

        int getViewParameterPos(int k, int i) {
            return camLinParamCount + camDistParamCount + k * viewParamCount + i;
        }
    }

    // ----------------------------------------------------------------------------------------

    /**
     * Maps between <em>full</em> (original) parameters (pF) and <em>model</em> parameters (pM),
     * which are a subset of the full parameters. Converts parameter indexes and scales.
     * Let i, j be the indexes for the same parameter in the full and model parameter vectors,
     * respectively, s[i] the scale for parameter i:
     * <pre>{@code
     *     pM[j] <- pF[i] / s[i]     // pM = scale(pF)
     *     pF[i] <- pM[j] * s[i]     // pF = unscale(pM)
     * }</pre>
     * Scale values must be non-zero but may be positve or negative.
     */
    static class ParameterAdapter {

        private final SubsequenceMapping sMap;     // maps full parameter to model indexes (and back)
        private final double[] scales;                  // scale vales (for full parameter vector)

        /**
         * Constructor. Throws an exception if {@code subset} and {@code scales} are not of the
         * same length or if {@code scales} contains zero values.
         * @param subset a {@link BitVector} flagging the model parameters
         * @param scales a vector of non-zero scale values, one for each full parameter (pass
         * {@code null} to set all scales to 1.0)
         */
        ParameterAdapter(BitVector subset, double[] scales) {
            this.sMap = new SubsequenceMapping(subset);
            if (scales == null) {
                this.scales = new double[subset.length()];
                Arrays.fill(this.scales, 1.0);
            }
            else {
                this.scales = scales.clone();
            }
        }

        /**
         * Updates the parameter scale values.
         * @param scales a vector of non-zero scale values, one for each full parameter
         */
        void setScales(double[] scales) {
            if (scales.length != sMap.getOrigSequenceLength()) {
                throw new IllegalArgumentException("scales.length != skipArray.length");
            }
            for (int i = 0; i < scales.length; i++) {
                if (Math.abs(scales[i]) < 1e-12) {
                    throw new IllegalArgumentException("scale value < 1e-12 at pos " + i);
                }
                this.scales[i] = scales[i];
            }
        }

        /**
         * Maps unscaled, full parameters {@code pFull} to reduced and scaled model parameters.
         * @param pFull unscaled full parameters
         * @return reduced and scaled model parameters
         */
        double[] getModelParameters(double[] pFull) {
            if (pFull.length != scales.length) {
                throw new IllegalArgumentException("pp.length != scales.length");
            }
            // reduce and scale by omitting skipped
            double[] pModel = new double[sMap.getSubSequenceLength()];
            for (int j = 0; j < pModel.length; j++) {
                int i = sMap.getOrigPosition(j);
                pModel[j] = pFull[i] / scales[i];
            }
            return pModel;
        }

        /**
         * Maps scaled model parameters {@code pModel} to full and unscaled physical parameters.
         * Parameters from {@code pModel} are merged into a copy of the full parameter vector
         * {@code pFull}. Parameters missing from {@code pModel} are carried over from {@code pFull}.
         * @param pModel reduced, scaled model parameters
         * @param pFull expanded, unscaled full parameters (template)
         * @return expanded, unscaled full parameters
         */
        double[] getFullParameters(double[] pModel, double[] pFull) {
            double[] pF = pFull.clone();
            // insert from reduced parameters:
            for (int j = 0; j < pModel.length; j++) {
                int i = sMap.getOrigPosition(j);
                pF[i] = pModel[j] * scales[i];
            }
            return pF;
        }

        /**
         * Returns the scale value for the specified parameter.
         * @param p parameter index (in full parameter vector)
         * @return the corresponding scale value
         */
        double getParameterScale(int p) {
            return scales[p];
        }

        /**
         * Returns the index in the full parameter vector for the given model parameter index.
         * @param modelIdx index in model parameter vector
         * @return the corresponding full parameter index
         */
        public int getFullParamIdx(int modelIdx) {
            return sMap.getOrigPosition(modelIdx);
        }

        /**
         * Returns the model parameter index for a given full parameter index. Returns -1 if
         * the corresponding parameter is not contained in the model parameters.
         * @param fullIdx index in full parameter vector
         * @return the model parameter index or -1 if not conteined in the model
         */
        public int getModelParamIdx(int fullIdx) {
            return sMap.getSubPosition(fullIdx);
        }

        /**
         * Returns the length of the full parameter vector.
         * @return the length of the full parameter vector
         */
        public int getFullParamLength() {
            return sMap.getOrigSequenceLength();
        }

        /**
         * Returns the length of the model parameter vector.
         * @return the length of the model parameter vector
         */
        public int getModelParamLength() {
            return sMap.getSubSequenceLength();
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

    public double getRmsError() {
        return result.getResiduals().getNorm() / Math.sqrt(N);
    }

    public String getFailureReason() {
        return (failureReason != null) ? failureReason : "";
    }

    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------

    /**
     * Represents the 'model' for the Levenberg-Marquart optimizer.
     * The required value vector Y and the Jacobian matrix J are calculated and returned by a common
     * method ({@link #value(RealVector)}).
     */
    class CombinedModel implements MultivariateJacobianFunction {

        private final double[] Y;       // value vector (allocated once and recycled)
        private final double[][] J;     // Jacobian matrix (allocated once and recycled)

        CombinedModel(int rows, int cols) {
            this.Y = new double[rows];          // [2 * N]
            this.J = new double[rows][cols];    // [2 * N][effParameterCnt]
        }

        @Override
        public Pair<RealVector, RealMatrix> value(RealVector point) {
            double[] params = adapter.getFullParameters(point.toArray(), initialParameters);
            // create a new Camera instance:
            double[] pc = assembler.getCameraParameters(params);
            Camera cam = initCam.withParameters(pc);

            // populate value vector (Y) ----------------------------------------------

            Arrays.fill(Y, 0.0);    // clear recycled value vector (probably not needed)
            // process each view
            for (int k = 0, row = 0; k < M; k++) {
                // create transform for view k
                ViewTransform Vk = new ViewTransform(assembler.getViewParameters(params, k));
                // ViewTransform Vk = new ViewTransform(getViewParameters(params, k));
                // project each point of view k to sensor:
                for (int i = 0; i < modPts[k].length; i++, row+=2) {
                    double[] uv = cam.project(Vk, modPts[k][i]);
                    Y[row + 0] = uv[0];
                    Y[row + 1] = uv[1];
                }
            }

            // populate Jacobian matrix (J) ----------------------------------------------

            for (int i = 0; i < J.length; i++) {    // clear recycled Jacobian matrix (probably not needed)
                Arrays.fill(J[i], 0.0);
            }

            // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
            for (int p = 0; p < assembler.getCameraParamCount(); p++) {                     // for all camera parameters
                int col = adapter.getModelParamIdx(p);                 // column index for matrix J
                if (col >= 0) {
                    // update J for non-skipped parameter p
                    double pcp = pc[p];                                 // keep current parameter value
                    double delta = estimateDelta(pcp);
                    pc[p] = pc[p] + delta;                              // nudge camera parameter p
                    Camera camMod = cam.withParameters(pc);             // modified camera
                    // project all model points through the modified camera:
                    for (int k = 0, row = 0; k < M; k++) {              // for all views k
                        ViewTransform Vk = new ViewTransform(assembler.getViewParameters(params, k));
                        for (int j = 0; j < modPts[k].length; j++, row+=2) {
                            // project all points of view k to sensor:
                            double[] Ymod = camMod.project(Vk, modPts[k][j]);   // [ux, uy]
                            J[row + 0][col] = (Ymod[0] - Y[row + 0]) / delta;   // dX
                            J[row + 1][col] = (Ymod[1] - Y[row + 1]) / delta;   // dY
                        }
                    }
                    pc[p] = pcp;                        // revert parameter pc[p] to original value
                }
            }

            // Step 2: calculate the diagonal blocks, one for each view k
            for (int k = 0, startRow = 0; k < M; k++) {    // for all views/blocks
                double[] w = assembler.getViewParameters(params, k);
                // nudge each view parameter in w
                for (int i = 0; i < w.length; i++) {
                    int p = assembler.getViewParameterPos(k, i);
                    int col = adapter.getModelParamIdx(p);
                    // int col = parameterIndexMapper.getReducedPos(p);
                    if (col >= 0) {                             // don't skip this parameter
                        double wi = w[i];                       // keep current parameter value w[i]
                        double delta = estimateDelta(wi);
                        w[i] = w[i] + delta;                    // nudge parameter w[i]
                        ViewTransform Vi = new ViewTransform(w);
                        // in diagonal block k of J, fill column col:
                        // for all model points: calculate disturbed output  value
                        for (int j = 0, row = startRow; j < modPts[k].length; j++, row+=2) {
                            double[] Ymod= cam.project(Vi, modPts[k][j]);       // [ux, uy]
                            J[row + 0][col] = (Ymod[0] - Y[row + 0]) / delta;   // dX
                            J[row + 1][col] = (Ymod[1] - Y[row + 1]) / delta;   // dY
                        }
                        w[i] = wi; // revert parameter w[i] to its original value
                    }
                }
                startRow += 2 * modPts[k].length;
            }

            // printJacobian(J);

            // scale J back to optimizer scale
            for (int p = 0; p < params.length; p++) {
                int col = adapter.getModelParamIdx(p);
                // int col = parameterIndexMapper.getReducedPos(p);
                if (col >= 0) { // non-skipped parameter
                    // multiply column J[*][p] by scale[p]:
                    double s = adapter.getParameterScale(p);     //parameterScales[p];
                    // System.out.printf("  --- scaling J[%d] by %.4f\n", col, s);
                    for (int j = 0; j < J.length; j++) {
                        J[j][col] *= s;
                    }
                }
            }

            // printJacobian(J);

            double[] colNorms = getMatrixColumnNorms(J);
            // System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));
            // System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
            // System.out.println("    J rank = " + getMatrixRank(J));
            // System.out.println("    JTJ condition number = " + getJtJconditionNumber(J));

            // prepare return values
            RealVector YY = new ArrayRealVector(Y, false);
            RealMatrix JJ = new  Array2DRowRealMatrix(J, false);
            return Pair.create(YY, JJ);
        }

        private static final double EPS = 1.5e-8; 	// = sqrt(2.2 * 10^{-16})

        private static double estimateDelta(double x) {
            double dx = EPS * Math.max(Math.abs(x), 1); // dx >= eps
            // avoid numerical truncation problems (add and subtract again) -
            // not sure this survives the compiler !?
            double tmp = x + dx;
            return tmp - x;
        }
    }

    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------

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
    }

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
        // if (showOnce) {
        //     System.out.println("JTJ:\n" + Matrix.toString(JTJ));
        //     showOnce = false;
        // }
        RealMatrix JTJD = JTJ.add(D);
        return Matrix.getConditionNumber(JTJD);
    }
}
