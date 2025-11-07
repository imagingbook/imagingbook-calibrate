/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.GaussNewtonOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;

public class RadialLateralDistortion implements LensDistortion {

    public static final int PARAM_COUNT = 5;
    public static final RadialLateralDistortion INSTANCE = new RadialLateralDistortion();
    private final double k0, k1, k2, p1, p2;
    private final double error; // estimation error

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public RadialLateralDistortion() {
        this(new double[] {0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public RadialLateralDistortion(double[] parameters) {
        this(parameters, 0.0);
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     * @param error average estimation error
     */
    public RadialLateralDistortion(double[] parameters, double error) {
        if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.k2 = parameters[2];
        this.p1 = parameters[3];
        this.p2 = parameters[4];
        this.error = error;
    }

    // ------------------------------------------------------------------------

    @Override
    public LensDistortion copyOf(double[] params, double error) {
        return new RadialLateralDistortion(params, error);
    }

    @Override
    public double[] getParameters() {
        return new double[] {k0, k1, k2, p1, p2};
    }

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        double xx = x * x;
        double yy = y * y;
        double xy = x * y;
        double r2 = xx + yy;
        double r4 = r2 * r2;
        double r6 = r2 * r4;
        return new double[][] {
                {du * r2, du * r4, du * r6, 2 * xy, r2 + 2 * xx},
                {dv * r2, dv * r4, dv * r6, r2 + 2 * yy, 2 * xy}};
    }
    // -------------------------------------------------------------------------

    @Override
    public double[] warp(double[] xyP) {
        double x = xyP[0];
        double y = xyP[1];
        double xx = x * x;
        double yy = y * y;
        double xy = x * y;
        double r2 = xx + yy;
        double r4 = r2 * r2;
        double r6 = r4 * r2;
        double Dr = k0 * r2 + k1 * r4 + k2 * r6;		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        double dx = 2 * p1 * xy + p2 * (r2 + 2 * xx);
        double dy = p1 * (r2 + 2 * yy) + 2 * p2 * xy;
        return new double[] {x * (1 + Dr) + dx, y * (1 + Dr) + dy};
    }

    @Override
    public double[] unwarp(double[] xyD) {
        // Target u,v
        double[] uv = xyD.clone();
        // Initial guess (x0, y0)
        double[] start = xyD.clone();
        // Define F(x,y): (u_calc, v_calc)

        MultivariateJacobianFunction model = point -> {
            double[] xy = point.toArray();
            double x = xy[0];
            double y = xy[1];

            // Example mapping
            double[] UV = this.warp(xy);
            double u = UV[0];
            double v = UV[1];

            double x2 = x * x;
            double x3 = x2 * x;
            double x4 = x2 * x2;
            double x5 = x4 * x;
            double x6 = x4 * x2;
            double y2 = y * y;
            double y3 = y2 * y;
            double y4 = y2 * y2;
            double y5 = y4 * y;
            double y6 = y4 * y2;

            // get partial derivatives for Jacobian:
            double dFXx =
                    1 + 6 * p2 * x + 3 * k0 * x2 + 5 * k1 * x4 + 7 * k2 * x6 + 2 * p1 * y + k0 * y2 + k1 * y4 + k2 * y6 + 6 * k1 * x2 * y2 + 9 * k2 * x2 * y4 + 15 * k2 * x4 * y2;
            double dFXy =
                    2 * p2 * y + 2 * p1 * x + 2 * k0 * x * y + 4 * k1 * x * y3 + 6 * k2 * x * y5 + 4 * k1 * x3 * y + 12 * k2 * x3 * y3 + 6 * k2 * x5 * y;   // much overlap with next!
            double dFYx =
                    2 * p1 * x + 2 * p2 * y + 2 * k0 * x * y + 4 * k1 * x * y3 + 6 * k2 * x * y5 + 4 * k1 * x3 * y + 12 * k2 * x3 * y3 + 6 * k2 * x5 * y;
            double dFYy =
                    1 + 6 * p1 * y + 3 * k0 * y2 + 5 * k1 * y4 + 7 * k2 * y6 + 2 * p2 * x + k0 * x2 + k1 * x4 + k2 * x6 + 6 * k1 * x2 * y2 + 9 * k2 * x4 * y2 + 15 * k2 * x2 * y4;

            // Jacobian matrix (2x2)
            double[][] J = {
                    { dFXx, dFXy },
                    { dFYx, dFYy }
            };

            return new Pair<>(
                    new ArrayRealVector(new double[]{u, v}),
                    new Array2DRowRealMatrix(J)
            );
        };

        // Build least squares problem
        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(start)
                .model(model)
                .target(uv)
                .lazyEvaluation(false)
                .maxEvaluations(1000)
                .maxIterations(1000)
                .build();

        // Use Levenberg–Marquardt optimizer
        // LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer optimizer = new GaussNewtonOptimizer();
        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

        System.out.println("Converged: " + optimum.getEvaluations());
        System.out.println("x = " + optimum.getPoint().getEntry(0));
        System.out.println("y = " + optimum.getPoint().getEntry(1));
        System.out.println("Residual: " + optimum.getResiduals().getNorm());

        return optimum.getPoint().toArray();
    }
}
