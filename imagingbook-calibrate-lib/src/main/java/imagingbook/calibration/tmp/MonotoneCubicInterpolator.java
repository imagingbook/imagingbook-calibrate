/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.tmp;

import java.util.Arrays;

/**
 * Piecewise Cubic Hermite Interpolating Polynomial (PCHIP)
 * implementation for monotone data (shape-preserving).
 *
 * Given strictly increasing y[i] and corresponding x[i],
 * builds a monotone cubic spline x(y).
 *
 * Kahaner, David, Cleve Moler, Stephen Nash. Numerical Methods and Software (1989)
 * SIAM Numerical Analysis: Theory and Experiments (Chapter 7: Interpolation on a piecewise-uniform grid)
 * https://matthodges.com/posts/2024-08-08-spline-pchip/?utm_source=chatgpt.com
 * https://jacobwilliams.github.io/PCHIP/
 *
 *
 * https://www.cs.usask.ca/~spiteri/M211/notes/chapter3.pdf
 * https://chatgpt.com/share/690f7f22-53cc-8006-876c-d2f0c65a733c
 *
 * https://de.mathworks.com/help/matlab/ref/pchip.html
 *
 *
 */
public class MonotoneCubicInterpolator {
    private final double[] y;
    private final double[] x;
    private final double[] d;   // derivatives dx/dy at each node

    public MonotoneCubicInterpolator(double[] y, double[] x) {
        if (y.length != x.length || y.length < 2)
            throw new IllegalArgumentException("Arrays must have equal length >= 2");

        int n = y.length;
        this.y = y.clone();
        this.x = x.clone();
        this.d = new double[n];

        // Compute secant slopes m[i]
        double[] m = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            double dy = y[i + 1] - y[i];
            if (dy <= 0) throw new IllegalArgumentException("y[] must be strictly increasing");
            m[i] = (x[i + 1] - x[i]) / dy;
        }

        // Compute derivatives d[i] using the PCHIP rule (Fritsch–Carlson)
        d[0] = m[0];
        d[n - 1] = m[n - 2];
        for (int i = 1; i < n - 1; i++) {
            if (m[i - 1] * m[i] > 0) {
                double w1 = 2 * (y[i + 1] - y[i]);
                double w2 = 2 * (y[i] - y[i - 1]);
                d[i] = (w1 + w2) / (w1 / m[i - 1] + w2 / m[i]);
            } else {
                d[i] = 0.0;
            }
        }
    }

    /** Evaluate x(yq) at query point yq. */
    public double evaluate(double yq) {
        int n = y.length;
        if (yq <= y[0]) return x[0];
        if (yq >= y[n - 1]) return x[n - 1];

        // Binary search for interval index i such that y[i] <= yq < y[i+1]
        int i = Arrays.binarySearch(y, yq);
        if (i < 0) i = -i - 2;
        if (i < 0) i = 0;
        if (i >= n - 1) i = n - 2;

        double h = y[i + 1] - y[i];
        double t = (yq - y[i]) / h;

        // Hermite basis functions
        double h00 = 2 * t * t * t - 3 * t * t + 1;
        double h10 = -2 * t * t * t + 3 * t * t;
        double h01 = t * t * t - 2 * t * t + t;
        double h11 = t * t * t - t * t;

        return x[i] * h00 + x[i + 1] * h10 + h * (d[i] * h01 + d[i + 1] * h11);
    }

    /** Optionally: derivative dx/dy at yq */
    public double derivative(double yq) {
        int n = y.length;
        if (yq <= y[0]) return d[0];
        if (yq >= y[n - 1]) return d[n - 1];

        int i = Arrays.binarySearch(y, yq);
        if (i < 0) i = -i - 2;
        if (i < 0) i = 0;
        if (i >= n - 1) i = n - 2;

        double h = y[i + 1] - y[i];
        double t = (yq - y[i]) / h;

        // Derivatives of Hermite basis functions
        double dh00 = 6 * t * t - 6 * t;
        double dh10 = -dh00;
        double dh01 = 3 * t * t - 4 * t + 1;
        double dh11 = 3 * t * t - 2 * t;

        return (x[i] * dh00 + x[i + 1] * dh10) / h + d[i] * dh01 + d[i + 1] * dh11;
    }

    public int size() {
        return y.length;
    }
}
