/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HomographyEstimatorFourCornersTest {

    static final double tol = 1e-6;
    static double NOISE = 0.1;

    static final RealMatrix Hreal = MatrixUtils.createRealMatrix(new double[][] {
            {3,  2, -1},
            {5, -3,  2},
            {4,  4,  1}});

    static final Pnt2d[] POINTS_A = new Pnt2d[] {
            Pnt2d.from(10, 7),
            Pnt2d.from(3, -1),
            Pnt2d.from(5, 5),
            Pnt2d.from(-6, 13),
            Pnt2d.from(0, 1),
            Pnt2d.from(2, 3)
    };

    static final Pnt2d[] POINTS_B = new Homography(Hreal).applyTo(POINTS_A);

    // TODO: more tests!

    @Test
    public void CharucoBoardHomographyTest() {
        Pnt2d[] boardPts = PntUtils.fromDoubleArray(HomographyCharucoTestData_DSC_2691.BOARD_POINTS);
        Pnt2d[] imagePts = PntUtils.fromDoubleArray(HomographyCharucoTestData_DSC_2691.IMAGE_POINTS);
        HomographyEstimator hestmtr = new HomographyEstimatorFourCorners(true, true, 1000, 100);
        Homography Hfinal = hestmtr.getHomography(boardPts, imagePts);
        // System.out.println("Hfinal = \n" + Matrix.toString(Hfinal));
        // System.out.println("final error = " + HomographyEstimator.getReprojectionError(boardPts, imagePts, Hfinal));
        assertTrue("reprojection error exceeded", HomographyEstimator.getReprojectionError(boardPts, imagePts, Hfinal) < 12);
    }

}