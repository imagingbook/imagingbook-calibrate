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

public class ZhangDistortionModel implements LensDistortionModel {

    final double k0, k1;    // lens distortion parameters

    public ZhangDistortionModel(double k0, double k1) {
        this.k0 = k0;
        this.k1 = k1;
    }

    public ZhangDistortionModel() {
        this(0, 0);
    }

    // -----------------------------------------

    public double getK0() {
        return this.k0;
    }

    public double getK1() {
        return this.k1;
    }

    // -----------------------------------------

    @Override
    public double[] warp(double[] xy) {
        final double x = xy[0];
        final double y = xy[1];
        final double r = Math.sqrt(x * x + y * y);
        double d = (1 + D(r));
        return new double[] {d * x, d * y};
    }

    @Override
    public double[] unwarp(double[] xyd) {
        final double xd = xyd[0];
        final double yd = xyd[1];
        final double R = Math.sqrt(xd * xd + yd * yd);	// distorted radius
        final double r = unwarp(R);						// undistorted radius
        final double s = r / R;
        return new double[] {s * xd, s * yd};
    }

    // ------------------------------------------------------------------------

    /**
     * Radial distortion function, to be applied in the form
     * <pre>r' = r * (1 + D(r))</pre>
     * to points in the ideal projection plane. Distortion coefficients k0, k1.
     *
     * @param r the original radius of a point in the ideal projection plane
     * @return the pos/neg deviation for the given radius
     */
    public double D(double r) {
        final double r2 = r * r;
        return (this.k0 + this.k1 * r2) * r2;		// D(r) = k0 * r^2 + k1 * r^4
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
    public double unwarp(double R) {
        double[] coefficients = {-R, 1, 0, this.k0, 0, this.k1};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        double rInit = R;
        int maxEval = 20;
        double r = solver.solve(maxEval, p, rInit);
//		System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

}
