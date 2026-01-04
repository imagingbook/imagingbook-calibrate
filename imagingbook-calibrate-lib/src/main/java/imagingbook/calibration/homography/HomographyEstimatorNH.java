/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.points.ProjectiveFit2d;
import org.apache.commons.math4.legacy.linear.RealMatrix;



/**
 * Homography estimator based on solving a non-homogeneous linear system for fitting.
 */
class HomographyEstimatorNH extends HomographyEstimator {

    // private final boolean normalizePoints; // TODO: check implementation of normalizePoints!
    // private final boolean doRefinement;

    /**
     * The only constructor.
     * @param normalizePoints
     * @param doRefinement
     */
    public HomographyEstimatorNH(boolean normalizePoints, boolean doRefinement) {
        super(normalizePoints, doRefinement);
    }

    @Override
    Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        if (ptsA.length != ptsB.length)
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        if (ptsA.length < 4)
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");

        double[][] Ha = new ProjectiveFit2d(ptsA, ptsB).getTransformationMatrix();
        Homography hom = new Homography(Ha);                 // this does normalization
        // System.out.println("   HomographyEstimatorH: initial = \n" + hom);

        // if (doRefinement) {
        //     hom = refineHomography(hom, ptsA, ptsB);
        // }
        return hom;
    }

    // -------------------------------------------------------------------------

    // helper method (may be used in tests too)
    protected static double[] mapPoint(RealMatrix M3x3, double[] p) {
        if (p.length != 2) {
            throw new IllegalArgumentException("vector p must be of length 2 but is " + p.length);
        }
        double[] pA = MathUtil.toHomogeneous(p);
        double[] pAt = M3x3.operate(pA);
        return MathUtil.toCartesian(pAt); // need to de-homogenize, since pAt[2] == 1?
    }


}
