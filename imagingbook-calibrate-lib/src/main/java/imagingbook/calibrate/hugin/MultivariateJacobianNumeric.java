/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.hugin;


import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

/**
 * An implementation of {@link MultivariateJacobianFunction} which only defines the
 * {@code value} part of the model, while the associated Jacobian part is calculated
 * by finite differences.
 */
public abstract class MultivariateJacobianNumeric implements MultivariateJacobianFunction {

    private final int M;    // number of Jacobian rows
    private final int N;    // number of Jacobian columns
    // private final double[] Y;       // value vector (allocated once and recycled)
    // private final double[][] J;     // Jacobian matrix (allocated once and recycled)

    public MultivariateJacobianNumeric(int rows, int cols) {
        this.M = rows;
        this.N = cols;
    }

    @Override
    public Pair<RealVector, RealMatrix> value(RealVector p) {
        double[] pp = p.toArray();
        double[] Y = getValues(pp);
        double[][] J = getJacobian(pp, Y);
        return new Pair<>(new ArrayRealVector(Y, false), new Array2DRowRealMatrix(J, false));
    }

    /**
     * Calculates the 'value' vector Y for the given parameter point.
     * To be implemented by inheriting classes.
     * @param p parameter point
     * @return the value vector
     */
    abstract double[] getValues(double[] p);

    /**
     * Calculates the Jacobian matrix by evaluating finite differences.
     * @param pp the current parameter point
     * @param Y the current value vector
     * @return
     */
    double[][] getJacobian(double[] pp, double[] Y) {
        if (pp.length != N) {
            throw new IllegalArgumentException("number of columns should be " + N);
        }
        if (Y.length != M) {
            throw new IllegalArgumentException("number of rows should be " + M);
        }

        double[] p = pp.clone();
        double[][] J = new double[M][N];

        for (int j = 0; j < N; j++) {   // for each parameter j
            double pj = p[j];  // keep current value for parameter j
            double delta = estimateDelta(pj);
            // nudge parameter j:
            p[j] = p[j] + delta;
            // re-calculate value vector with modified parameters j:
            double[] Ymod = getValues(p);
            // update column j of Jacobian J:
            for (int i = 0; i < M; i++) {
                J[i][j] = (Ymod[i] - Y[i]) / delta;
            }
            p[j] = pj;         // revert parameter j to original value
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

}
