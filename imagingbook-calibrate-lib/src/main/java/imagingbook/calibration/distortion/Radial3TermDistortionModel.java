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

public class Radial3TermDistortionModel  implements RadialDistortionModel {

    public static final int PARAM_COUNT = 3;
    public static final Radial3TermDistortionModel INSTANCE = new Radial3TermDistortionModel();
    //private final double[] parameters; // lens distortion parameters
    private final double k1, k2, k3;

    /**
     * The only constructor. If no argument is supplied, an instance
     * with zero-valued parameters is constructed.
     * @param parameters vector of distortion parameters
     */
    public Radial3TermDistortionModel(double... parameters) {
        if (parameters.length == 0)
            parameters = new double[PARAM_COUNT];
        else if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k1 = parameters[0];
        this.k2 = parameters[1];
        this.k3 = parameters[2];
    }

    @Override
    public Radial3TermDistortionModel copyOf(double... params) {
        return new Radial3TermDistortionModel(params);
    }

    @Override
    public double[] getParameters() {
        return new double[] {k1, k2, k3};
    }

    // -------------------------------------------------------------------------

    @Override
    public double warp(double r) {
        final double r2 = r * r;
        final double r4 = r2 * r2;
        final double r6 = r4 * r2;
        double D = k1 * r2 + k2 * r4 + k3 * r6;		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        return r * (1 + D);
    }

    @Override
    public double unwarp(double R) {
        double[] coefficients = {-R, 1, 0, k1, 0, k2, 0, k3};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        int maxEval = 20;
        double r = solver.solve(maxEval, p, R); // rInit = R
//		System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

    // -------------------------------------------------------------------------

    @Override
    public double[] getDMatrixRowU(double x, double y, double du, double dv) {
        return new double[0];
    }

    @Override
    public double[] getDMatrixRowV(double x, double y, double du, double dv) {
        return new double[0];
    }

    // -------------------------------------------------------------------------

}
