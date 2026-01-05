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

public abstract class AbstractHomographyEstimator {

    final boolean normalizePoints;
    final boolean doRefinement;

    AbstractHomographyEstimator(boolean normalizePoints, boolean doRefinement) {
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

        return estimateHomography(ptsA, ptsB);    // implemented by subclasses
    }

    abstract Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB);

    // -------------------------------------------------------------------------------------------

    static RealMatrix getNormalisationMatrix(Pnt2d[] pnts) {
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

    static double[] map2dHomogeneous(double[] p, RealMatrix M3x3) {
        if (p.length != 2) {
            throw new IllegalArgumentException("transform(): vector p must be of length 2 but is " + p.length);
        }
        double[] pA = MathUtil.toHomogeneous(p);
        double[] pAt = M3x3.operate(pA);
        return MathUtil.fromHomogeneous(pAt); // need to de-homogenize, since pAt[2] == 1?
    }

    // ------------------------------------------------------------

    // public Homography refineHomography(Homography Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {
    //     return new HomographyRefinement().refineHomography(Hinit, pntsA, pntsB);
    // }

    /**
     * Estimates the homographies between a fixed set of 2D model points and multiple observations (image point sets).
     * The correspondence between the points is assumed to be known.
     *
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
