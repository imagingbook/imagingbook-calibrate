/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.tmp;

public class MonotoneCubicInterpolator_Main {
     // Suppose we know f(x) = x + 0.2x^3 - 0.05x^5
    // and we want inverse x(y) ≈ g(y) using PCHIP

    // parameters
    static final double k0 = 0.2;
    static final double k1 = -0.05;

    // forward map f(x)
    static double f ( double x) {
        // prefer explicit multiplication instead of Math.pow for speed
        double x2 = x * x;
        double x3 = x2 * x;
        double x5 = x3 * x2;
        return x + k0 * x3 + k1 * x5;
    }

    public static void main (String[]args){
        double Xmax = 1.8;
        int n = 60; // number of nodes (tune as desired)
        double[] xNodes = new double[n];
        double[] yNodes = new double[n];

        // Option: uniform nodes in x
        for (int i = 0; i < n; i++) {
            xNodes[i] = Xmax * i / (n - 1);
            yNodes[i] = f(xNodes[i]);
        }

        // Build interpolator (y must be strictly increasing)
        MonotoneCubicInterpolator inv = new MonotoneCubicInterpolator(yNodes, xNodes);

        // Test evaluate some y values
        for (double y = 0.0; y <= f(Xmax); y += 0.2) {
            double xApprox = inv.evaluate(y);
            System.out.printf("y=%.6f -> x≈%.8f%n", y, xApprox);
        }
    }
}
