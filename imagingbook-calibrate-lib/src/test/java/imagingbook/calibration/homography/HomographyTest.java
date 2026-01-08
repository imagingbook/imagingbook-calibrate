/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HomographyTest {

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

    // static final Pnt2d[] POINTS_B = new Homography(Hreal).applyTo(POINTS_A);
    static final Pnt2d[] POINTS_B = HomographyUtils.projectPoints(POINTS_A, Hreal);

    // -----------------------------------------------------------------------

    // @Test // assumes Hral[2,2] = 1 (normalized)
    // public void normalizeHomographyTest1() {
    //     Homography H1 = new Homography(Hreal);
    //     Homography H2 = new Homography(Hreal.scalarMultiply(2.5));
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), H1.getData(), tol);
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), H2.getData(), tol);
    //     NumericTestUtils.assert2dArrayEquals(H1.getData(), H2.getData(), tol);
    // }

    // @Test
    // public void normalizeHomographyTest2() {
    //     Homography H = new Homography();
    //     NumericTestUtils.assert2dArrayEquals(Matrix.idMatrix(3), H.getData(), tol);
    // }

    // @Test   // trivial test checking identity map
    // public void homographyTestIdentity() {
    //     Homography H = new Homography();
    //     Pnt2d[] PB = H.applyTo(POINTS_A);
    //     assertEquals(POINTS_A.length, PB.length);
    //     // check if points are unchanged:
    //     for (int i = 0; i < PB.length; i++) {
    //         assertArrayEquals(POINTS_A[i].toDoubleArray(), PB[i].toDoubleArray(), tol);
    //     }
    // }

    // check homography estimate from perfect match works regardless of options used:

    // @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    // public void HomographyTestEstimate00() {
    //     RealMatrix Hestm = Homography.from(POINTS_A, POINTS_B, false, false);
    //     // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    // }
    //
    // @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    // public void HomographyTestEstimate10() {
    //     RealMatrix Hestm = Homography.from(POINTS_A, POINTS_B, true, false);
    //     // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    // }
    //
    // @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    // public void HomographyTestEstimate01() {
    //     RealMatrix Hestm = Homography.from(POINTS_A, POINTS_B, false, true);
    //     // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    // }
    //
    // @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal
    // public void HomographyTestEstimate11() {
    //     RealMatrix Hestm = Homography.from(POINTS_A, POINTS_B, true, true);
    //     // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     NumericTestUtils.assert2dArrayEquals(Hreal.getData(), Hestm.getData(), tol);
    // }

    // ----------------------------------------------------------------------------------

    // @Test   // check homography under noise added to POINTS_B
    // public void HomographyTestEstimateNoisy() {
    //     double noise = 0.2;     // noise magnitude
    //     Pnt2d[] PBnoisy = new Pnt2d[POINTS_B.length];
    //     RandomGenerator rand = new DeterministicRandom(17);
    //     for (int i = 0; i < POINTS_B.length; i++) {
    //         PBnoisy[i] = POINTS_B[i].plus(noise * rand.nextDouble(), noise * rand.nextDouble());    // add uniform noise
    //     }
    //     Homography Hest = Homography.from(POINTS_A, PBnoisy, true, true);  // TODO: check without num. refinement!
    //     // System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //
    //     // check if points are unchanged:
    //     double dsum = 0;
    //     double dmax = Double.NEGATIVE_INFINITY;
    //     for (int i = 0; i < PBnoisy.length; i++) {
    //         // Pnt2d pMapped = Hest.applyTo(POINTS_A[i]); // apply estimated homography
    //         Pnt2d pMapped = HomographyEstimator.projectOnePoint(POINTS_A[i], Hest); // apply estimated homography
    //         double di = PBnoisy[i].distance(pMapped);
    //         dsum = dsum + di;
    //         dmax = Math.max(dmax, di);
    //     }
    //     double davg = dsum / PBnoisy.length;
    //     // System.out.println("davg = " + davg);
    //     // System.out.println("dmax = " + dmax);
    //     assertTrue(davg < 1 * noise);
    //     assertTrue(dmax < 1 * noise);
    // }

}