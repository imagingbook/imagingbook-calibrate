/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;

import imagingbook.common.math.Arithmetic;
import imagingbook.common.math.Matrix;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * This class represents a homography, i.e., a projective transformation
 * and methods for estimating such transformations from sets of 2D point pairs.
 * Implements RealMatrix, each instance being a 3x3 matrix.
 * Homographies are normalized (element (2, 2) is 1) and immutable.
 *
 * @author WB
 */
public class Homography  extends Array2DRowRealMatrix {
    /** Max. number of Levenberg-Marquardt evaluations. */
	public static int MaxLmEvaluations = 1000;
    /** Max. number of Levenberg-Marquardt iterations. */
	public static int MaxLmIterations = 1000;

	// ------------------------------------------------------------

    public Homography(double[][] H) {
        super(normalize(H));
    }

    public Homography(RealMatrix H) {
        this(H.getData());
    }

    public Homography() {
        this(Matrix.idMatrix(3));
    }

    // ------------------------------------------------------------

    /**
     * Scale all elements of H such that H(2,2) = 1.
     * Useful for comparing homography matrices.
     * @param H a 3 x 3 homography matrix
     * @return the normalized matrix
     */
    private static double[][] normalize(double[][] H) {
        if (H.length != 3 || H[0].length != 3)
            throw new IllegalArgumentException("homography matrix is not of size 3 x 3");
        double h22 = H[2][2];
        if (Arithmetic.isZero(h22, 1e-15))
            throw new IllegalArgumentException("zero homography matrix element H(2,2)");
        return Matrix.multiply(1.0 / h22, H);
    }

    // ------------------------------------------------------------

	/**
	 * Estimates the homography (projective) transformation from two given 2D point
     * sequences assumed to be in correspondence (and of same length).
     * By default, input point sets are statistically normalized and non-linear
     * refinement is applied to the initially estimated homography.
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (a normalized 3 x 3 matrix)
	 */
    public static Homography from(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        return from(ptsA, ptsB, true, true);
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D point sets. The correspondence between the
     * points is assumed to be known.
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 2nd sequence of 2D points
     * @param normalizePoints whether to perform statistic normalization on input point sets
     * @param doRefinement whether to perform non-linear refinement aof estimated homography
     * @return the estimated homography (a normalized 3 x 3 matrix)
     */
	public static Homography from(Pnt2d[] ptsA, Pnt2d[] ptsB, boolean normalizePoints, boolean doRefinement) {
        HomographyEstimator estimator = new HomographyEstimator(normalizePoints, doRefinement);
        return estimator.getHomography(ptsA, ptsB);
    }

    // ------------------------------------------------------------

    /**
     * Applies this homography the supplied 2D point in homogeneous space.
     * @param p a 2D point
     * @return the transformed point
     */
    public Pnt2d applyTo(Pnt2d p) {
        double[] pA = MathUtil.toHomogeneous(p.toDoubleArray());
        double[] pAt = this.operate(pA);
        return Pnt2d.from(MathUtil.toCartesian(pAt));
    }

    /**
     * Applies this homography the supplied array of 2D points in homogeneous space.
     * @param P an array of 2D points
     * @return the array of transformed points
     */
    public Pnt2d[] applyTo(Pnt2d[] P) {
        final int n = P.length;
        Pnt2d[] Q = new Pnt2d[n];
        for (int i = 0; i < n; i++) {
            Q[i] = applyTo(P[i]);
        }
        return Q;
    }

}
