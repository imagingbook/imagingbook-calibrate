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
import imagingbook.testutils.DeterministicRandom;
import imagingbook.testutils.NumericTestUtils;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import java.util.random.RandomGenerator;

import static org.junit.Assert.assertTrue;

public class HomographyEstimatorSimpleTest {

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

    // check homography estimate from perfect match works regardless of options used:
    // NOTE: point normalization not implemented for HomographyEstimatorSimple!

    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    public void HomographyTestEstimate00() {
        HomographyEstimator hestmtr = new HomographyEstimatorSimple(false, false);
        RealMatrix Hestm = hestmtr.getHomography(POINTS_A, POINTS_B);
        // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
        NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    }

    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    public void HomographyTestEstimate10() {
        HomographyEstimator hestmtr = new HomographyEstimatorSimple(true, false);
        RealMatrix Hestm = hestmtr.getHomography(POINTS_A, POINTS_B);
        // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
        NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    }

    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    public void HomographyTestEstimate01() {
        HomographyEstimator hestmtr = new HomographyEstimatorSimple(false, true);
        RealMatrix Hestm = hestmtr.getHomography(POINTS_A, POINTS_B);
        // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
        NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    }

    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    public void HomographyTestEstimate11() {
        HomographyEstimator hestmtr = new HomographyEstimatorSimple(true, true);
        RealMatrix Hestm = hestmtr.getHomography(POINTS_A, POINTS_B);
        // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
        NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    }

    @Test   // check homography under noise added to POINTS_B
    public void HomographyTestEstimateNoisy() {
        double noise = 0.2;     // noise magnitude
        Pnt2d[] PBnoisy = new Pnt2d[POINTS_B.length];
        RandomGenerator rand = new DeterministicRandom(17);
        for (int i = 0; i < POINTS_B.length; i++) {
            PBnoisy[i] = POINTS_B[i].plus(noise * rand.nextDouble(), noise * rand.nextDouble());    // add uniform noise
        }

        HomographyEstimator hestmtr = new HomographyEstimatorSimple(true, true); // TODO: check without num. refinement!
        Homography Hest = hestmtr.getHomography(POINTS_A, PBnoisy);
        System.out.println("Hest = \n" + Matrix.toString(Hest));

        // check if points are unchanged:
        double dsum = 0;
        double dmax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < PBnoisy.length; i++) {
            Pnt2d pMapped = Hest.applyTo(POINTS_A[i]); // apply estimated homography
            double di = PBnoisy[i].distance(pMapped);
            dsum = dsum + di;
            dmax = Math.max(dmax, di);
        }
        System.out.println("dmax = " + dmax);
        System.out.println("dsum = " + dsum);

        double davg = dsum / PBnoisy.length;
        System.out.println("davg = " + davg);
        // System.out.println("davg = " + davg);
        // System.out.println("dmax = " + dmax);
        assertTrue("davg >= 0.5 * noise", davg < Math.max(0.5 * noise, 1e-6));
        assertTrue("dmax >= 0.5 * noise", dmax < Math.max(0.5 * noise, 1e-6));
    }

    @Test
    public void CharucoBoardHomographyTest() {
        Pnt2d[] boardPts = PntUtils.fromDoubleArray(HomographyCharucoTestData_DSC_2691.BOARD_POINTS);
        Pnt2d[] imagePts = PntUtils.fromDoubleArray(HomographyCharucoTestData_DSC_2691.IMAGE_POINTS);
        HomographyEstimator hestmtr = new HomographyEstimatorSimple(true, true);
        Homography Hest = hestmtr.getHomography(boardPts, imagePts);
        System.out.println("Hest = \n" + Matrix.toString(Hest));
    }

}