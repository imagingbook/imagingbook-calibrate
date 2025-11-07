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

public class Radial3TermDistortion implements RadialDistortion {

    public static final int PARAM_COUNT = 3;
    public static final Radial3TermDistortion INSTANCE = new Radial3TermDistortion();
    //private final double[] parameters; // lens distortion parameters
    private final double k0, k1, k2;
    private final double error; // estimation error

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public Radial3TermDistortion() {
        this(new double[] {0, 0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public Radial3TermDistortion(double[] parameters) {
        this(parameters, 0.0);
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     * @param error average estimation error
     */
    public Radial3TermDistortion(double[] parameters, double error) {
        if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.k2 = parameters[2];
        this.error = error;
    }

    @Override
    public Radial3TermDistortion copyOf(double[] params, double error) {
        return new Radial3TermDistortion(params, error);
    }

    @Override
    public double[] getParameters() {
        return new double[] {k0, k1, k2};
    }

    @Override
    public double getError() {
        return this.error;
    }

    // -------------------------------------------------------------------------

    @Override
    public double fRad(double r) {
        final double r2 = r * r;
        final double r4 = r2 * r2;
        final double r6 = r4 * r2;
        double D = k0 * r2 + k1 * r4 + k2 * r6;		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        return r * (1 + D);
    }

    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre>p(r) = - R + r + k0 * r^3 + k1 * r^5 + k2 * r^7,</pre>
     * where R is constant, by using a Newton-Raphson solver.
     *
     * @param R the distorted radius
     * @return the undistorted radius
     */
    @Override
    public double fRadInv(double R) {
        double[] coefficients = {-R, 1, 0, k0, 0, k1, 0, k2};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        int maxEval = 20;
        double r = solver.solve(maxEval, p, R); // rInit = R
//		System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

    // -------------------------------------------------------------------------

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        final double xx = x * x;
        final double yy = y * y;
        final double r2 = xx + yy;
        final double r4 = r2 * r2;
        final double r6 = r2 * r4;
        return new double[][] {
                {du * r2, du * r4, du * r6},
                {dv * r2, dv * r4, dv * r6}};
    }

    // -------------------------------------------------------------------------

}
