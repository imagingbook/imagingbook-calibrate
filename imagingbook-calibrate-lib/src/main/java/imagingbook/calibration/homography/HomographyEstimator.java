/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

public abstract class HomographyEstimator {

    final boolean normalizePoints;
    final boolean doRefinement;

    /**
     * Constructor for abstract {@link HomographyEstimator}.
     * @param normalizePoints normalize point sets for homography estimation
     * @param doRefinement perform numerical refinement (if implemented)
     */
    HomographyEstimator(boolean normalizePoints, boolean doRefinement) {
        this.normalizePoints = normalizePoints;
        this.doRefinement = doRefinement;
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D
     * point sequences assumed to be in correspondence (and of same length).
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 1st sequence of 2D points
     * @return the estimated homography
     */
    public final Homography getHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        if (ptsA.length != ptsB.length) {
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        }
        if (ptsA.length < 4) {
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");
        }

        RealMatrix Na = null, Nb = null;
        Pnt2d[] ptsAn, ptsBn;

        if (normalizePoints) {
            Na = getNormalisationMatrix(ptsA);
            Nb = getNormalisationMatrix(ptsB);
            ptsAn = transformPoints(ptsA, Na);
            ptsBn = transformPoints(ptsB, Nb);
        }
        else {
            ptsAn = ptsA;
            ptsBn = ptsB;
        }

        // get an initial homography estimate (with normalized coordinates):
        RealMatrix Hinit = estimateHomography(ptsAn, ptsBn);    // implemented by subclasses

        // if (normalizePoints) {
        //     RealMatrix HinitDen = MatrixUtils.inverse(Nb).multiply(Hinit).multiply(Na);
        //     System.out.println("   FourPoint: Hinit denormalized = \n" + Matrix.toString(new Homography(HinitDen)));
        //     System.out.println("   initial reproj. error (denormalized) = " + getReprojectionError(ptsA, ptsB, HinitDen));
        // }

        // optionally refine this estimate (still with normalized coordinates):
        RealMatrix Hn = (doRefinement) ? refineHomography(Hinit, ptsAn, ptsBn) : Hinit;
        // de-normalize the homography matrix:
        RealMatrix H = (normalizePoints) ? MatrixUtils.inverse(Nb).multiply(Hn).multiply(Na) : Hn;

        return new Homography(H);
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D point sets,
     * which are normalized if the {@code normalizePoints} flag is set in the constructor.
     * The correspondence between the points is assumed to be known.
     * The estimate is obtained by solving a homogeneous linear system over all 9 elements
     * of the homography matrix.
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 2nd sequence of 2D points
     * @return the estimated homography (3 x 3 matrix)
     */
    abstract RealMatrix estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB);

    /**
     * Refines the initial homography by non-linear optimization.
     * If {@code normalizePoints} is set, the initial homography and the two point sets
     * are supposed to be normalized, as is the resulting homography
     * @param Hinit the initial (estimated) homography
     * @param pntsA the 1st sequence of 2D points (the model points)
     * @param pntsB the 2nd sequence of 2D points (the observed image points)
     * @return the refined homography
     */
    abstract RealMatrix refineHomography(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB);

    // -------------------------------------------------------------------------------------------

    /**
     * Calculates and returns an affine transformation matrix for the supplied 2D point set,
     * which, when applied to the point set, creates a new point set with centroid at the origin
     * and unit variance in x/y directions.
     * @param pnts the original point set to be normalized
     * @return the affine transformation matrix for normalizing the point set
     */
    public static RealMatrix getNormalisationMatrix(Pnt2d[] pnts) {
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

        RealMatrix matrixA = MatrixUtils.createRealMatrix(new double[][]{
                {sx, 0, -sx * meanx},
                {0, sy, -sy * meany},
                {0, 0, 1}});

        return matrixA;
    }


    /**
     * Applies a 3x3 transformation matrix to the given 2D point {@code p} in homogeneous
     * coordinate space.
     * @param p a 2D point
     * @param M3x3 the transformation matrix
     * @return the transformed point
     */
    public static double[] map2dHomogeneous(double[] p, RealMatrix M3x3) {
        if (p.length != 2) {
            throw new IllegalArgumentException("transform(): vector p must be of length 2 but is " + p.length);
        }
        double[] pA = MathUtil.toHomogeneous(p);
        double[] pAt = M3x3.operate(pA);
        return MathUtil.fromHomogeneous(pAt); // need to de-homogenize, since pAt[2] == 1?
    }

    /**
     * Applies a 3x3 transformation matrix to the given 2D point {@code p} in homogeneous
     * coordinate space.
     * @param p a 2D point
     * @param M3x3 the transformation matrix
     * @return the transformed point
     */
    public static Pnt2d map2dHomogeneous(Pnt2d p, RealMatrix M3x3) {
        return Pnt2d.from(map2dHomogeneous(p.toDoubleArray(), M3x3));
    }


    /**
     * Applies a 3x3 transformation matrix to the given 2D point set {@code pts} in homogeneous
     * coordinate space.
     * @param pts a sequence of 2D points
     * @param M3x3 the transformation matrix
     * @return the transformed point set
     */
    public static Pnt2d[] transformPoints(Pnt2d[] pts, RealMatrix M3x3) {
        Pnt2d[] ptsN = new Pnt2d[pts.length];
        for (int i = 0; i < pts.length; i++) {
            ptsN[i] = map2dHomogeneous(pts[i], M3x3);
        }
        return ptsN;
    }


    public static double getReprojectionError(Pnt2d[] ptsA, Pnt2d[] ptsB, RealMatrix H) {
        int n = ptsA.length;
        double[] Ya = new double[2 * n];
        double[] Yb = new double[2 * n];
        for (int i = 0; i < n; i++) {
            double[] A = map2dHomogeneous(ptsA[i].toDoubleArray(), H);
            // System.out.printf("    A=%s -> %s : B=%s\n", ptsA[i], Pnt2d.from(A), ptsB[i]);
            Ya[2*i + 0] = A[0];
            Ya[2*i + 1] = A[1];
            Yb[2*i + 0] = ptsB[i].getX();
            Yb[2*i + 1] = ptsB[i].getY();
        }
        double[] R = Matrix.subtract(Yb, Ya);
        return Matrix.normL2(R);
    }


    // ------------------------------------------------------------

    /**
     * Estimates the homographies between a fixed set of 2D model points and multiple observations (image point sets).
     * The correspondence between the points is assumed to be known.
     * @param modelPts a sequence of 2D points on the model (calibration target)
     * @param obsPoints a sequence 2D image point sets (one set per view).
     * @return the sequence of estimated homographies (3 x 3 matrices), one for each view
     */
    public Homography[] getHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints) {
    	final int M = obsPoints.length;
    	Homography[] homographies = new Homography[M];
    	for (int i = 0; i < M; i++) {
            Homography Hinit = getHomography(modelPts, obsPoints[i]);
            // Homography H = doNonlinearRefinement ?
    		// 		refineHomography(Hinit, modelPts, obsPoints[i]) : Hinit;
    		homographies[i] = Hinit;
    	}
    	return homographies;
    }

}
