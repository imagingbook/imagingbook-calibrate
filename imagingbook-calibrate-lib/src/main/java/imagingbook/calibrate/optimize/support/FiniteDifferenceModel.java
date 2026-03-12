/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize.support;


import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import imagingbook.common.util.SubsequenceMapping;
import imagingbook.common.util.bits.BitVector;
import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DiagonalMatrix;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;

import java.util.Arrays;

/**
 * An implementation of {@link MultivariateJacobianFunction} which only defines the
 * {@code value} part of the model, while the associated Jacobian part is calculated
 * by finite differences.
 */
public abstract class FiniteDifferenceModel implements MultivariateJacobianFunction {

    protected int iterationCounter = -1;    // for debugging only

    private final double[] initialParams;    // the initial (full) parameters
    private final double[] fullScales;

    private final SubsequenceMapping parameterMapping;


    private final boolean useCentralDifferences = true;

    /**
     * Creates a {@link FiniteDifferenceModel} instance with the specified number of parameters,
     * all of them marked as free (no fixed parameters).
     */
    public FiniteDifferenceModel(double[] initialFullParams) {
        this(initialFullParams, new BitVector(initialFullParams.length, true));
    }

    /**
     * Creates a {@link FiniteDifferenceModel} instance with the total number of parameters
     * and the free parameters specified by the supplied {@link BitVector}.
     * @param freeParamFlags a {@link BitVector} marking the free parameters (free = true).
     */
    public FiniteDifferenceModel(double[] initialFullParams, BitVector freeParamFlags) {
        this.initialParams = initialFullParams;
        this.parameterMapping = new SubsequenceMapping(freeParamFlags);

        // set up a initial scale coefficients for all parameters
        this.fullScales = new double[initialFullParams.length];
        Arrays.fill(fullScales, 1);
    }

    // -------------------------------------------------------------------------------

    /**
     * Extracts the vector of free parameters (used for calculating the Jacobian) from the full
     * parameter vector. The parameter values are scaled by the current {@code scale} values
     * (see {@link #setParameterScales(double[])}):
     * <pre>{@code freeParams[j] = fullParams[j] / scale[j]}</pre>
     * An exception is thrown if the length of the supplied vector differs from the original
     * parameter vector specified for this model.
     * @param fullParams the full parameter vector
     * @return the vector of scaled free parameters
     */
    public double[] getFreeParameters(double[] fullParams) {
        if (fullParams.length != parameterMapping.getOrigSequenceLength()) {
            throw new IllegalArgumentException("number of parameters does not match length of full parameter vector");
        }
        double[] freeParams = parameterMapping.getSubSequence(fullParams);
        double[] freeScales = parameterMapping.getSubSequence(fullScales);
        for (int i = 0; i < freeParams.length; i++) {
            freeParams[i] = freeParams[i] / freeScales[i];
        }
        return freeParams;
    }

    /**
     * Combines a vector of free parameters (typically obtained from the optimizer) with a full
     * parameter vectors (typically the initial parameters). Free parameters replace the corresponding
     * values in the full parameter vector.
     * Parameter values are unscaled using the current {@code scale} values
     * (see {@link #setParameterScales(double[])}):
     *  <pre>{@code fullParams[j] = freeParams[j] * scale[j]}</pre>
     * @param freeP the vector of free model parameters
     * @return a full parameter vector with unscaled free parameter values inserted
     */
    public double[] getFullParameters(double[] freeP) {
        double[] freeParams = freeP.clone();
        double[] freeScales = parameterMapping.getSubSequence(fullScales);
        System.out.println("freeParams.length: " + freeParams.length);
        for (int i = 0; i < freeParams.length; i++) {
            freeParams[i] =
                    freeParams[i]
                            * freeScales[i];
        }
        return parameterMapping.merge(freeParams, initialParams);
    }

    // -------------------------------------------------------------------------------

    /**
     * Updates the parameter scale values.
     * @param scales a vector of non-zero scale values, one for each full parameter
     */
    public void setParameterScales(double[] scales) {
        if (scales.length != fullScales.length) {
            throw new IllegalArgumentException("scales vector size does not match full parameter vector");
        }
        for (int i = 0; i < fullScales.length; i++) {
            if (Math.abs(scales[i]) < 1e-12) {
                throw new IllegalArgumentException("scale value < 1e-12 at pos " + i);
            }
            fullScales[i] = scales[i];
        }
    }

    /**
     * TODO: Revise to calculate autoscales only on free parameters?
     * Returns auto-scale values for all parameters (including all fixed parameters).
     * @return a vector of auto-scale values
     */
    public double[] getAutoScales() {
        RealMatrix Jac = this.value(new ArrayRealVector(initialParams)).getSecond();
        double[] autoScales = new double[initialParams.length];
        for (int j = 0; j < initialParams.length; j++) {
            // get norm of Jacobian column j
            double norm = Jac.getColumnVector(j).getNorm();
            autoScales[j] = (norm < 1e-12) ? 1.0 : (1.0 / norm);
        }
        return autoScales;
    }

    // -------------------------------------------------------------------------------

    @Override
    public Pair<RealVector, RealMatrix> value(RealVector p) {
        iterationCounter++;
        double[] pp = getFullParameters(p.toArray()); // p.toArray();
        double[] Y = getValues(pp);
        double[][] J = getJacobian(pp, Y);

        if (iterationCounter == 1) {
            try (var prec = PrintPrecision.set(8)) {
                // PrintPrecision.set(8);
                System.out.println(" p = " + Matrix.toString(p));
                System.out.println(" Y = \n" + Matrix.toString(Y));
                double[] colNorms = getMatrixColumnNorms(J);
                System.out.println(" J = \n" + Matrix.toString(J));
                System.out.println("\n***** |J| column norms = " + Matrix.toString(colNorms));
                System.out.println("    J condition No = " + Matrix.getConditionNumber(J));
                // System.out.println("    J rank = " + getMatrixRank(J));
                System.out.println("    JTJ condition number = " + getJtJconditionNumber(J));
            }
        }

        return new Pair<>(new ArrayRealVector(Y, false), new Array2DRowRealMatrix(J, false));
    }

    /**
     * Calculates the 'value' vector Y for the given parameter point.
     * To be implemented by inheriting classes.
     * @param p the current (full) parameter vector
     * @return the value vector for the current parameters
     */
    public abstract double[] getValues(double[] p);

    /**
     * Calculates the Jacobian matrix for free parameters only by evaluating finite differences using
     * {@link #getValues(double[])}.
     * This method may be overridden by inheriting classes.
     * @param p the current (full) parameter vector
     * @param Y the value vector for the current parameters (to save one evaluation)
     * @return
     */
    public double[][] getJacobian(double[] p, double[] Y) {
        double[] pp = getFreeParameters(p);     // p.clone();
        final int M = pp.length;
        final int N = Y.length;

        double[][] J = new double[N][M];    // N x M Jacobian matrix

        for (int j = 0; j < M; j++) {       // for each free parameter j
            final double pj = pp[j];        // keep current value for parameter j
            double delta = estimateDelta(pj);
            if (iterationCounter < 1) {
                System.out.println("delta = " + delta);
            }

            // nudge parameter j and re-calculate value vector with modified parameters:
            pp[j] = pj + delta;
            double[] Ypos = getValues(getFullParameters(pp));
            pp[j] = pj - delta;
            double[] Yneg = getValues(getFullParameters(pp));

            // update column j of Jacobian J:
            for (int i = 0; i < N; i++) {
                J[i][j] = (useCentralDifferences) ?
                        (Ypos[i] - Yneg[i]) / (2 * delta):
                        (Ypos[i] - Y[i]) / delta;
            }

            pp[j] = pj;         // revert parameter j to original value
        }

        return J;
    };

    // ---------------------------------------------------------------------------

    private static final double EPS = 1.5e-8; 	// = sqrt(2.2 * 10^{-16})

    private static double estimateDelta(double x) {
        double dx = EPS * Math.max(Math.abs(x), 1); // dx >= eps
        // avoid numerical truncation problems (add and subtract again) -
        // not sure this survives the compiler !?
        double tmp = x + dx;
        return tmp - x;
    }

    private static double[] getMatrixColumnNorms(double[][] J) {
        double[] colNorms = new double[J[0].length];
        RealMatrix JR = new Array2DRowRealMatrix(J, false);
        for (int j = 0; j < colNorms.length; j++) {
            colNorms[j] = JR.getColumnVector(j).getNorm();
        }
        return colNorms;
    }

    private static int getMatrixRank(double[][] data) {
        RealMatrix matrix = new Array2DRowRealMatrix(data);
        // 2. Perform SVD
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        // 3. Get the rank
        return svd.getRank();
    }

    public static double getJtJconditionNumber(double[][] data) {
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
