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

/**
 * Simplified radial distortion model used in Zhang's EasyCalib implementation.
 */
public class Radial2TermDistortion implements RadialDistortion {

    public static final int PARAM_COUNT = 2;
    public static final Radial2TermDistortion INSTANCE = new Radial2TermDistortion();
    private final double k0, k1;
    // private final double[] parameters; // lens distortion parameters

    /**
     * The only constructor. If no argument is supplied, an instance
     * with zero-valued parameters is constructed.
     * @param parameters vector of distortion parameters
     */
    public Radial2TermDistortion(double... parameters) {
        if (parameters.length == 0)
            parameters = new double[PARAM_COUNT];
        else if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
    }

    @Override
    public Radial2TermDistortion copyOf(double... params) {
        return (params.length == 0) ?
            new Radial2TermDistortion(this.getParameters()) :
            new Radial2TermDistortion(params);
    }

    // -----------------------------------------

    @Override
    public double[] getParameters() {
        return new double[] {k0, k1};
    }

    @Override
    public double[] getDMatrixRowU(double x, double y, double du, double dv) {
        final double r2 = x * x + y * y;
        final double r4 = r2 * r2;
        return new double[] {du * r2, du * r4};
    }

    @Override
    public double[] getDMatrixRowV(double x, double y, double du, double dv) {
        final double r2 = x * x + y * y;
        final double r4 = r2 * r2;
        return new double[] {dv * r2, dv * r4};
    }

    // -----------------------------------------

    // @Override
    // public double[] warp(double[] xy) {
    //     final double x = xy[0];
    //     final double y = xy[1];
    //     final double r = Math.sqrt(x * x + y * y);  // undistorted radius
    //     if (r < 1e-6)
    //         return new double[] {0, 0};
    //     // final double R = warp(r);        // distorted radius
    //     final double s = warp(r) / r;
    //     return new double[] {s * x, s* y};
    // }

    // @Override
    // public double[] unwarp(double[] xyd) {
    //     final double xd = xyd[0];
    //     final double yd = xyd[1];
    //     final double R = Math.sqrt(xd * xd + yd * yd);	// distorted radius
    //     if (R < 1e-6)
    //         return new double[] {0, 0};
    //     // final double r = unwarp(R);					// undistorted radius
    //     final double s = unwarp(R) / R;
    //     return new double[] {s * xd, s * yd};
    // }

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
     * <pre>p(r) = - R + r + k0 * r^3 + k1 * r^5,</pre>
     * where R is constant, by using a Newton-Raphson solver.
     *
     * @param R the distorted radius
     * @return the undistorted radius
     */
    @Override
    public double fRadInv(final double R) {
        double[] coefficients = {-R, 1, 0, k0, 0, k1};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        int maxEval = 20;
        double r = solver.solve(maxEval, p, R); // rInit = R
        // System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

    // ------------------------------------------------------------------------

    // /**
    //  * Radial distortion function, to be applied in the form
    //  * <pre>r' = r * (1 + D(r))</pre>
    //  * to points in the ideal projection plane. Distortion coefficients k0, k1.
    //  *
    //  * @param r the original radius of a point in the ideal projection plane
    //  * @return the pos/neg deviation for the given radius
    //  */
    // private double D(double r) {
    //     final double r2 = r * r;
    //     final double k0 = parameters[0];
    //     final double k1 = parameters[1];
    //     return (k0 + k1 * r2) * r2;		// D(r) = k0 * r^2 + k1 * r^4
    // }
}
