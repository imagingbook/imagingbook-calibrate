/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.exception.TooManyEvaluationsException;
import org.apache.commons.math4.legacy.exception.TooManyIterationsException;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.GaussNewtonOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;

public class RadialLateralDistortion extends DistortionModel {

    private final double k0, k1, k2, p1, p2;

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public RadialLateralDistortion() {
        this(new double[] {0, 0, 0, 0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public RadialLateralDistortion(double[] parameters) {
        super(parameters);
        if (parameters.length != 5) {
            throw new IllegalArgumentException("expected 5 parameters but received " +
                    parameters.length);
        }
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.k2 = parameters[2];
        this.p1 = parameters[3];
        this.p2 = parameters[4];
    }

    @Override
    public RadialLateralDistortion withParameters(double[] params) {
        return (params == null) ?
                new RadialLateralDistortion() :
                new RadialLateralDistortion(params);
    }

    // ------------------------------------------------------------------------

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
    public double[] warp(double[] xy) {
        double x = xy[0];
        double y = xy[1];
        double x2 = x * x;
        double y2 = y * y;
        double r2 = x2 + y2;
        double r4 = r2 * r2;
        double r6 = r4 * r2;
        double Dr = k0 * r2 + k1 * r4 + k2 * r6;		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        double dx = 2 * p1 * x * y + p2 * (r2 + 2 * x2);
        double dy = p1 * (r2 + 2 * y2) + 2 * p2 * x * y;
        // System.out.format("*** x=%.3f  x=%.3f | Dr=%.3f  dx=%.3f  dy=%.3f\n", x, y, Dr, dx, dy);
        return new double[] {x * (1 + Dr) + dx, y * (1 + Dr) + dy};
    }

    @Override   // this will fail for large p1, p2 coefficients!
    public double[] unwarp(double[] XY) {

        MultivariateJacobianFunction model = point -> {
            double[] xyp = point.toArray();
            double x = xyp[0];
            double y = xyp[1];
            double x2 = x * x;
            double y2 = y * y;
            double r2 = x2 + y2;
            double r4 = r2 * r2;
            double r6 = r4 * r2;

            // partial derivatives for Jacobian:
            double A = 1 + k0 * r2 + k1 * r4 + k2 * r6;
            double B = 2 * k0 + 4 * k1 * r2 + 6 * k2 * r4;
            double dFXx = A + x2 * B + 2 * p1 * y + 6 * p2 * x;
            double dFXy = x * y * B + 2 * p1 * x + 2 * p2 * y;
            double dFYx = dFXy;
            double dFYy = A + y2 * B + 6 * p1 * y + 2 * p2 * x;

            // Jacobian for point (x,y)
            double[][] Jp = {
                    { dFXx, dFXy },
                    { dFYx, dFYy }
            };

            // Value for point (x,y)
            double[] XYp = this.warp(xyp);

            return new Pair<>(
                    new ArrayRealVector(XYp),
                    new Array2DRowRealMatrix(Jp)
            );
        };

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(model)
                .target(XY)
                .start(XY)      // target = start = warped point XY
                .lazyEvaluation(false)
                .maxEvaluations(1000)
                .maxIterations(1000)
                .checker(new EvaluationRmsChecker(1e-8))
                .build();

        // LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();    // new GaussNewtonOptimizer() - alternatively
        LeastSquaresOptimizer optimizer = new GaussNewtonOptimizer();

        try {
            LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);
            return optimum.getPoint().toArray();
        } catch (TooManyEvaluationsException | TooManyIterationsException e) {
            System.err.printf("unwarp() inversion failed: %s (eval=%d, iter=%d)%n",
                    e.getClass().getSimpleName(),
                    problem.getEvaluationCounter().getCount(),
                    problem.getIterationCounter().getCount());
            return new double[]{Double.NaN, Double.NaN};
        }

        // Pair<RealVector, RealMatrix> pr = model.value(optimum.getPoint());
        // System.out.println("Iterations: " + optimum.getEvaluations());
        // System.out.println("Final point = " + Matrix.toString(optimum.getPoint().toArray()));
        // System.out.println("Residual: " + Arrays.toString(optimum.getResiduals().toArray()));
        // System.out.println("Value(x,y) = " + Matrix.toString(pr.getFirst()));
        // System.out.println("Jacobian(x,y) = \n" + Matrix.toString(pr.getSecond()));

    }
}
