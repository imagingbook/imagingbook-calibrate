/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import static imagingbook.common.math.Arithmetic.sqr;

public final class HomographyUtils {

    private HomographyUtils() {}


    /**
     * Calculates and returns an affine transformation matrix for the supplied 2D point set,
     * which, when applied to the point set, creates a new point set with centroid at the origin
     * and unit variance in x/y directions.
     * @param pnts the original point set to be normalized
     * @return the affine transformation matrix for normalizing the point set
     */
    public static RealMatrix getNormalisationMatrix(Pnt2d[] pnts) {
        final int n = pnts.length;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
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

        return new Array2DRowRealMatrix(new double[][]
                {{sx, 0, -sx * meanx},
                 {0, sy, -sy * meany},
                 {0, 0, 1}}, false);
    }

    /**
     * Applies a 3x3 transformation matrix to the given 2D point {@code p} in homogeneous
     * coordinate space.
     * @param p a 2D point
     * @param M3x3 the transformation matrix
     * @return the transformed point
     */
    public static double[] projectOnePoint(double[] p, RealMatrix M3x3) {
        if (p.length != 2) {
            throw new IllegalArgumentException("transform(): vector p must be of length 2 but is " + p.length);
        }
        double[] pA = MathUtil.toHomogeneous(p);
        double[] pAt = M3x3.operate(pA);
        return MathUtil.fromHomogeneous(pAt); // need to de-homogenize, since pAt[2] == 1?
    }

    /**
     * Applies a 3x3 transformation matrix to the given 2D point set {@code pts} in homogeneous
     * coordinate space.
     * @param pts a sequence of 2D points
     * @param M the transformation matrix
     * @return the transformed point set
     */
    public static Pnt2d[] projectPoints(Pnt2d[] pts, RealMatrix M) {
        return new LinearMapping2D(M).applyTo(pts);
        // LinearMapping2D mapping = new LinearMapping2D(M);
        // Pnt2d[] ptsN = new Pnt2d[pts.length];
        // for (int i = 0; i < pts.length; i++) {
        //     // ptsN[i] = projectOnePoint(pts[i], M3x3);
        //     ptsN[i] = mapping.applyTo(pts[i]);
        // }
        // return ptsN;
    }

    /**
     * Calculates and returns the total reprojection error || H*A - B ||.
     * @param A first point sequence (to be projected)
     * @param B second point sequence (reference)
     * @param H 3x3 projection matrix
     * @return the reprojection error
     */
    public static double getReprojectionError(Pnt2d[] A, Pnt2d[] B, RealMatrix H) {
        LinearMapping2D mapping = new LinearMapping2D(H);
        double sum = 0;
        for (int i = 0; i < A.length; i++) {
            Pnt2d AA = mapping.applyTo(A[i]);
            // double[] pa = projectOnePoint(A[i].toDoubleArray(), H);
            sum += sqr(B[i].getX() - AA.getX());
            sum += sqr(B[i].getY() - AA.getY());
        }
        return Math.sqrt(sum);
    }
}
