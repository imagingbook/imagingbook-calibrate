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

    private final double[] parameters; // lens distortion parameters

    public ZhangDistortionModel(double k0, double k1) {
        this.parameters = new double[] {k0, k1};
    }

    public ZhangDistortionModel(double[] p) {
        // TODO: check length of p!
        this(p[0], p[1]);
    }

    // -----------------------------------------

    @Override
    public int getParameterCount() {
        return parameters.length;
    }

    @Override
    public double[] getParameters() {
        return parameters;  // TODO: clone?
    }

    @Override
    public double getParameter(int i) {
        // TODO: check i
        return parameters[i];
    }

    // D.setEntry(l2 + 0, 0, du * r2);
	// D.setEntry(l2 + 0, 1, du * r4);
	// D.setEntry(l2 + 1, 0, dv * r2);
	// D.setEntry(l2 + 1, 1, dv * r4);

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

    @Deprecated
    public double warp(double r) {
        return r * (1 + D(r));
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
        final double k0 = parameters[0];
        final double k1 = parameters[1];
        return (k0 + k1 * r2) * r2;		// D(r) = k0 * r^2 + k1 * r^4
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
        final double k0 = parameters[0];
        final double k1 = parameters[1];
        double[] coefficients = {-R, 1, 0, k0, 0, k1};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        double rInit = R;
        int maxEval = 20;
        double r = solver.solve(maxEval, p, rInit);
//		System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

}
