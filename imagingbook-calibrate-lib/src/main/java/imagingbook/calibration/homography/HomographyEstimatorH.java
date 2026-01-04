/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * Homography estimator based on solving a homogeneous linear system.
 *
 * @author WB
 */
public class HomographyEstimatorH extends HomographyEstimator {

	// private final boolean normalizePoints;
	// private final boolean doRefinement;

	// ------------------------------------------------------------

	public HomographyEstimatorH() {
		this(true, true);
	}

	public HomographyEstimatorH(boolean normalizePoints, boolean doRefinement) {
		super(normalizePoints, doRefinement);
	}

	// ------------------------------------------------------------

	/**
	 * Estimates the homography (projective) transformation from two given 2D point sets. The correspondence between the
	 * points is assumed to be known.
	 *
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (3 x 3 matrix)
	 */
	@Override
	Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
		int n = ptsA.length;
		System.out.println("estimateHomography " + normalizePoints + " " + doRefinement);
		RealMatrix Na = (normalizePoints) ? getNormalisationMatrix(ptsA) : MatrixUtils.createRealIdentityMatrix(3);
		RealMatrix Nb = (normalizePoints) ? getNormalisationMatrix(ptsB) : MatrixUtils.createRealIdentityMatrix(3);
		RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);

		for (int j = 0, r = 0; j < ptsA.length; j++) {
			final double[] pA = transform(MathUtil.toArray(ptsA[j]), Na);
			final double[] pB = transform(MathUtil.toArray(ptsB[j]), Nb);
			final double xA = pA[0];
			final double yA = pA[1];
			final double xB = pB[0];
			final double yB = pB[1];
			M.setRow(r + 0, new double[]{xA, yA, 1, 0, 0, 0, -(xA * xB), -(yA * xB), -(xB)});
			M.setRow(r + 1, new double[]{0, 0, 0, xA, yA, 1, -(xA * yB), -(yA * yB), -(yB)});
			r = r + 2;
		}

		// find h, such that M . h = 0:
		double[] h = MathUtil.solveHomogeneousSystem(M).toArray();

		// assemble homography matrix H from h:
		RealMatrix H = MatrixUtils.createRealMatrix(new double[][]
				{{h[0], h[1], h[2]},
				{h[3], h[4], h[5]},
				{h[6], h[7], h[8]}});

		// de-normalize the homography
		H = MatrixUtils.inverse(Nb).multiply(H).multiply(Na);

		// rescale M such that H[2][2] = 1 (unless H[2][2] close to 0)
		if (Math.abs(H.getEntry(2, 2)) > 10e-8) {
			H = H.scalarMultiply(1.0 / H.getEntry(2, 2));
		}

		Homography hom = new Homography(H);
		// if (doRefinement) {
		// 	hom = refineHomography(hom, ptsA, ptsB);
		// }
		return hom;
	}

	static double[] transform(double[] p, RealMatrix M3x3) {
		if (p.length != 2) {
			throw new IllegalArgumentException("transform(): vector p must be of length 2 but is " + p.length);
		}
		double[] pA = MathUtil.toHomogeneous(p);
		double[] pAt = M3x3.operate(pA);
		return MathUtil.toCartesian(pAt); // need to de-homogenize, since pAt[2] == 1?
	}

	// private RealMatrix getNormalisationMatrix(Pnt2d[] pnts) {
	// 	final int N = pnts.length;
	// 	double[] x = new double[N];
	// 	double[] y = new double[N];
	//
	// 	for (int i = 0; i < N; i++) {
	// 		x[i] = pnts[i].getX();
	// 		y[i] = pnts[i].getY();
	// 	}
	//
	// 	// calculate the means in x/y
	// 	double meanx = MathUtil.mean(x);
	// 	double meany = MathUtil.mean(y);
	//
	// 	// calculate the variances in x/y
	// 	double varx = MathUtil.variance(x);
	// 	double vary = MathUtil.variance(y);
	//
	// 	double sx = Math.sqrt(2 / varx);
	// 	double sy = Math.sqrt(2 / vary);
	//
	// 	RealMatrix matrixA = MatrixUtils.createRealMatrix(new double[][]{
	// 			{sx, 0, -sx * meanx},
	// 			{0, sy, -sy * meany},
	// 			{0, 0, 1}});
	//
	// 	return matrixA;
	// }

}
