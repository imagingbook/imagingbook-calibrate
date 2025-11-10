/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import org.apache.commons.math4.legacy.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math4.legacy.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math4.legacy.analysis.solvers.UnivariateDifferentiableSolver;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Simplified radial distortion model used in Zhang's EasyCalib implementation.
 * Distortion is modeled by
 * function r' = warp(r) = r * (1 + k0 * r^2 + k1 * r^4) .
 */
public class Radial2TermDistortion implements RadialDistortion {

    public static final int PARAM_COUNT = 2;
    public static final Radial2TermDistortion INSTANCE = new Radial2TermDistortion();
    private final double k0, k1;
    private final double error; // estimation error

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public Radial2TermDistortion() {
        this(new double[] {0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public Radial2TermDistortion(double[] parameters) {
        this(parameters, 0.0);
    }


    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     * @param error average estimation error
     */
    public Radial2TermDistortion(double[] parameters, double error) {
        if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.error = error;
    }



    @Override
    public Radial2TermDistortion copyOf(double[] params,double error) {
        return new Radial2TermDistortion(params, error);
    }

    // -----------------------------------------

    @Override
    public double[] getParameters() {
        return new double[] {k0, k1};
    }

    @Override
    public double getError() {
        return this.error;
    }

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        final double xx = x * x;
        final double yy = y * y;
        final double r2 = xx + yy;
        final double r4 = r2 * r2;
        return new double[][] {
                {du * r2, du * r4},
                {dv * r2, dv * r4}};
    }

    // -----------------------------------------

    /**
     * Forward radial distortion function.
     * @param r the original radius of a point in the ideal projection plane
     * @return the distorted radius
     */
    @Override
    public double fRad(final double r) {
        final double r2 = r * r;
        double D = r2 * (k0 + k1 * r2);		// D(r) = k0 * r^2 + k1 * r^4
        return r * (1 + D);
    }


    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre>p(r) = -R + r + k0 * r^3 + k1 * r^5,</pre>
     * where R is given and fixed, by using a Newton-Raphson solver.
     *
     * @param R the distorted radius
     * @return the undistorted radius
     */
    @Override
    public double fRadInv(final double R) {
        double[] coefficients = {-R, 1, 0, k0, 0, k1};
        UnivariateDifferentiableSolver inverseSolver = new NewtonRaphsonSolver();
        PolynomialFunction p = new PolynomialFunction(coefficients);
        int maxEval = 20;
        double r = inverseSolver.solve(maxEval, p, R); // rInit = R
        // System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

    // ------------------------------------------------------------------------

    /**
     * Find an approximate inverse function for
     *  fRad(r) = r' =r * (1 + k0 * r^2 + k1 * r^4)
     *  using the same form of polynomial
     *  fRadInv(r') = r = r' * (1 + q0 * r'^2 + q1 * r'^4)
     *
     * @return
     */
    public double[] estimateInverseFunction2() {
        int N = 1000;   // number of samples
        Random rand = new Random();
        RealMatrix A = MatrixUtils.createRealMatrix(N, 2);
        RealVector d = new ArrayRealVector(N);
        for (int i = 0; i < N; i++) {
            double r = rand.nextDouble();
            double rr = fRad(r);
            double rr3 = rr * rr * rr;
            double rr5 = rr3 * rr * rr;
            A.setEntry(i, 0, rr3);
            A.setEntry(i, 1, rr5);
            d.setEntry(i, r - rr);
        }

        DecompositionSolver solver = new QRDecomposition(A).getSolver();
        RealVector q = solver.solve(d);

        RealVector residual = d.subtract(A.operate(q));
        double maxres = residual.getMaxValue();
        double avgres = residual.getNorm() / N;
        System.out.println("max residual = " + maxres);
        System.out.println("avg residual = " + avgres);
        return q.toArray();
    }

    /**
     * Just an experiment. Find coefficients for a polynomial
     * to model the inverse function.
     * @return
     */
    public double[] estimateInverseFunction3() {
        int N = 1000;   // number of samples
        Random rand = new Random();
        RealMatrix A = MatrixUtils.createRealMatrix(N, 3);
        RealVector d = new ArrayRealVector(N);
        for (int i = 0; i < N; i++) {
            double r = rand.nextDouble();
            double rr = fRad(r);
            double rr3 = rr * rr * rr;
            double rr5 = rr3 * rr * rr;
            double rr7 = rr5 * rr * rr;
            A.setEntry(i, 0, rr3);
            A.setEntry(i, 1, rr5);
            A.setEntry(i, 2, rr7);
            d.setEntry(i, r - rr);
        }

        DecompositionSolver solver = new QRDecomposition(A).getSolver();
        RealVector q = solver.solve(d);

        RealVector residual = d.subtract(A.operate(q));
        double maxres = residual.getMaxValue();
        double avgres = residual.getNorm() / N;
        double rmserr = Math.sqrt(avgres);
        System.out.println("max residual = " + maxres);
        System.out.println("avg residual = " + avgres);
        System.out.println("rms residual = " + rmserr);
        return q.toArray();
    }
}
