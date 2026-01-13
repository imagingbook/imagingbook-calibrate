/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import org.apache.commons.math4.legacy.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math4.legacy.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math4.legacy.analysis.solvers.UnivariateDifferentiableSolver;

/**
 * TODO: UNFINISHED CODE!
 */
public class PtLensDistortionModel extends RadialDistortionModel {

    private final double k0, k1, k2;

    @Override
    public int getParameterCount() {
        return 3;
    }

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public PtLensDistortionModel() {
        this(0, 0, 0);
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public PtLensDistortionModel(double... parameters) {
        super(parameters);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.k2 = parameters[2];
    }

    @Override
    public PtLensDistortionModel from(double... params) {
        return (params == null) ?
                new PtLensDistortionModel() :
                new PtLensDistortionModel(params);
    }

    // -------------------------------------------------------------------------

    @Override
    public double fRad(double r) {
        double r2 = r * r;
        double r4 = r2 * r2;
        double r6 = r4 * r2;
        double D = k0 * r2 + k1 * r4 + k2 * r6;		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        return r * (1 + D);
    }

    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre>p(r) = - R + r + k0 * r^3 + k1 * r^5 + k2 * r^7,</pre>
     * where R is constant, by using a Newton-Raphson solver.
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
        return r;
    }

    // -------------------------------------------------------------------------

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        double xx = x * x;
        double yy = y * y;
        double r2 = xx + yy;
        double r4 = r2 * r2;
        double r6 = r2 * r4;
        return new double[][] {
                {du * r2, du * r4, du * r6},
                {dv * r2, dv * r4, dv * r6}};
    }

    // -------------------------------------------------------------------------

}
