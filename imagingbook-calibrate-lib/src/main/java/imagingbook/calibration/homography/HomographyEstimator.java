/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Arithmetic;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import static imagingbook.common.math.Arithmetic.sqr;

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
    public final RealMatrix getHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
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
            ptsAn = projectPoints(ptsA, Na);
            ptsBn = projectPoints(ptsB, Nb);
        }
        else {
            ptsAn = ptsA;
            ptsBn = ptsB;
        }

        // get an initial homography estimate (with normalized coordinates):
        RealMatrix Hinit = estimateHomography(ptsAn, ptsBn);    // implemented by subclasses

        // optionally refine this estimate (still with normalized coordinates):
        RealMatrix Href = (doRefinement) ? refineHomography(Hinit, ptsAn, ptsBn) : Hinit;

        // de-normalize the homography matrix:
        RealMatrix H = (normalizePoints) ? MatrixUtils.inverse(Nb).multiply(Href).multiply(Na) : Href;

        // if possible, rescale H to H(2,2) = 1:
        return (Arithmetic.isZero(H.getEntry(2, 2), 1e-15)) ?
                H : H.scalarMultiply(1.0 / H.getEntry(2, 2));
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
     * Applies a 3x3 transformation matrix to the given 2D point {@code p} in homogeneous
     * coordinate space.
     * @param p a 2D point
     * @param M3x3 the transformation matrix
     * @return the transformed point
     */
    public static Pnt2d projectOnePoint(Pnt2d p, RealMatrix M3x3) {
        return Pnt2d.from(projectOnePoint(p.toDoubleArray(), M3x3));
    }

    /**
     * Applies a 3x3 transformation matrix to the given 2D point set {@code pts} in homogeneous
     * coordinate space.
     * @param pts a sequence of 2D points
     * @param M3x3 the transformation matrix
     * @return the transformed point set
     */
    public static Pnt2d[] projectPoints(Pnt2d[] pts, RealMatrix M3x3) {
        Pnt2d[] ptsN = new Pnt2d[pts.length];
        for (int i = 0; i < pts.length; i++) {
            ptsN[i] = projectOnePoint(pts[i], M3x3);
        }
        return ptsN;
    }

    /**
     * Calculates and returns the total reprojection error || H*A - B ||.
     * @param A first point sequence (to be projected)
     * @param B second point sequence (reference)
     * @param H 3x3 projection matrix
     * @return the reprojection error
     */
    public static double getReprojectionError(Pnt2d[] A, Pnt2d[] B, RealMatrix H) {
        double sum = 0;
        for (int i = 0; i < A.length; i++) {
            double[] pa = projectOnePoint(A[i].toDoubleArray(), H);
            sum += sqr(B[i].getX() - pa[0]);
            sum += sqr(B[i].getY() - pa[1]);
        }
        return Math.sqrt(sum);
    }

    // ------------------------------------------------------------

    /**
     * Estimates the homographies between a fixed set of 2D model points and multiple observations (image point sets).
     * The correspondence between the points is assumed to be known.
     * @param modelPts a sequence of 2D points on the model (calibration target)
     * @param obsPoints a sequence 2D image point sets (one set per view).
     * @return the sequence of estimated homographies (3 x 3 matrices), one for each view
     */
    public RealMatrix[] getHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints) {
    	final int M = obsPoints.length;
        RealMatrix[] homographies = new RealMatrix[M];
    	for (int i = 0; i < M; i++) {
            RealMatrix H = getHomography(modelPts, obsPoints[i]);
            // Homography H = doNonlinearRefinement ?
    		// 		refineHomography(Hinit, modelPts, obsPoints[i]) : Hinit;
    		homographies[i] = H;
    	}
    	return homographies;
    }

}
