/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.tmp;

import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.*;

public class InverseMapping_LevenbergM {

    public static void main(String[] args) {
        // Target u,v
        double[] uv = {1.2, 0.8};

        // Initial guess (x0, y0)
        double[] start = {0.0, 0.0};

        // Define F(x,y): (u_calc, v_calc)
        MultivariateJacobianFunction model = point -> {
            double x = point.getEntry(0);
            double y = point.getEntry(1);

            // Example mapping (forward)
            double u = Math.sin(x) + 0.1 * y * y;
            double v = Math.cos(y) + 0.1 * x * y;

            // Jacobian matrix (2x2)
            double[][] jacobian = {
                    { Math.cos(x), 0.2 * y },
                    { 0.1 * y, -Math.sin(y) + 0.1 * x }
            };

            return new Pair<>(
                    new ArrayRealVector(new double[]{u, v}),
                    new Array2DRowRealMatrix(jacobian)
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
        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

        System.out.println("Converged: " + optimum.getEvaluations());
        System.out.println("x = " + optimum.getPoint().getEntry(0));
        System.out.println("y = " + optimum.getPoint().getEntry(1));
        System.out.println("Residual: " + optimum.getResiduals().getNorm());
    }
}
