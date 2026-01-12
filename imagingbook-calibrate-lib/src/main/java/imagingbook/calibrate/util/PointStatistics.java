/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.util;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;

public abstract class PointStatistics {

    private PointStatistics() {}

    /**
     * Calculates and returns a normalization mapping (affine transformation)
     * for the specified 2D point set. Applying this matrix to the same point
     * set will create a new point set with mean = (0,0) and variance = 1 in x,y.
     * @param pnts the input point set
     * @return an affine 2D transformation (2x3)
     */
	public static AffineMapping2D getNormalisationMapping(Pnt2d[] pnts) {
		final int N = pnts.length;
		double[] x = new double[N];
		double[] y = new double[N];

		for (int i = 0; i < N; i++) {
			x[i] = pnts[i].getX();
			y[i] = pnts[i].getY();
		}

		// calculate the means in x/y
		double meanx = MathUtil.mean(x);
		double meany = MathUtil.mean(y);

		// calculate the variances in x/y
		double varx = MathUtil.variance(x);
		double vary = MathUtil.variance(y);

		double sx = Math.sqrt(2 / varx);
		double sy = Math.sqrt(2 / vary);

        if (!Double.isFinite(sx)) sx = 1;
        if (!Double.isFinite(sy)) sy = 1;

        return new AffineMapping2D(new double[][]{
                {sx, 0, -sx * meanx},
                {0, sy, -sy * meany},
                {0, 0, 1}});
	}
}
