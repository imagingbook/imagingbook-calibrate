/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;

import imagingbook.testutils.DeterministicRandom;
import imagingbook.testutils.NumericTestUtils;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HomographyEstimateTest {

    // static Random rand = new Random(17);  // use fixed random seed!
    // static Random rand = new DeterministicRandom(17);

    static final double tol = 1e-6;
    static double NOISE = 0.1;

    static final RealMatrix Hreal = MatrixUtils.createRealMatrix(new double[][] {
            {3, 2, -1},
            {5, -3, 2},
            {4, 4, 1}});
    static final double Dreal = Matrix.determinant(Hreal);

    // static List<Pnt2d> pntlistA = new ArrayList<>();
    // static List<Pnt2d> pntlistB = new ArrayList<>();
    //
    // static {
    //     PrintPrecision.set(6);
    //     pntlistA.add(Pnt2d.from(10, 7));
    //     pntlistA.add(Pnt2d.from(3, -1));
    //     pntlistA.add(Pnt2d.from(5, 5));
    //     pntlistA.add(Pnt2d.from(-6, 13));
    //     pntlistA.add(Pnt2d.from(0, 1));
    //     pntlistA.add(Pnt2d.from(2, 3));
    //
    //     for (Pnt2d a : pntlistA) {
    //         pntlistB.add(mapPointWithNoise(Hreal, a, NOISE));
    //     }
    // }

    static final Pnt2d[] POINTS_A = new Pnt2d[] {
            Pnt2d.from(10, 7),
            Pnt2d.from(3, -1),
            Pnt2d.from(5, 5),
            Pnt2d.from(-6, 13),
            Pnt2d.from(0, 1),
            Pnt2d.from(2, 3)
    };
    static final Pnt2d[] POINTS_B = mapPoints(Hreal, POINTS_A);

    // static Pnt2d[] pntsA = pntlistA.toArray(new Pnt2d[0]);
    // static Pnt2d[] pntsB = pntlistB.toArray(new Pnt2d[0]);

    // -----------------------------------------------------------------------

    @Test
    public void normalizeHomographyNormalizeTest() {
        RealMatrix Hnorm = HomographyEstimate.normalizeHomography(Hreal);
        assertEquals(1, Hnorm.getEntry(2, 2), tol);
        // System.out.println("Matrix.determinant(Hnorm) =" + Matrix.determinant(Hnorm));
        System.out.println("Matrix.determinant(Hreal) =" + Dreal);

    }

    @Test   // scaled homographies perform the same mapping
    public void normalizeHomographyTestNormalized() {
        RealMatrix Hnorm = HomographyEstimate.normalizeHomography(Hreal);
        for (Pnt2d p : POINTS_A) {
            double[] q1 = HomographyEstimate.transform(p.toDoubleArray(), Hreal);
            double[] q2 = HomographyEstimate.transform(p.toDoubleArray(), Hnorm);
            assertArrayEquals(q1, q2, tol);
        }
        for (Pnt2d p : POINTS_B) {
            double[] q1 = HomographyEstimate.transform(p.toDoubleArray(), Hreal);
            double[] q2 = HomographyEstimate.transform(p.toDoubleArray(), Hnorm);
            assertArrayEquals(q1, q2, tol);
        }
    }

    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal (with optimization)
    public void HomographyEstimatorTestEstimate00() {
        RealMatrix Hestm = HomographyEstimate.from(POINTS_A, POINTS_B, false, false).getHomography();
        Matrix.determinant(Hestm);
        // System.out.println("He = \n" + Matrix.toString(Hestm));
        // estimated homography must have the same determinant as the original
        // assertEquals(Matrix.determinant(Hreal), Matrix.determinant(Hestm), tol);
        // normalize both and check if both are the same (after normalizing)
        RealMatrix HrealN = HomographyEstimate.normalizeHomography(Hreal);
        RealMatrix HestmN = HomographyEstimate.normalizeHomography(Hestm);
        NumericTestUtils.assert2dArrayEquals(HrealN.getData(), HestmN.getData(), tol);
    }

//    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal (with optimization)
//    public void HomographyEstimatorTestEstimate01() {
//        HomographyEstimate he = new HomographyEstimate(false, true);
//        RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_B);
//        // System.out.println("He = \n" + Matrix.toString(Hestm));
//        // estimated homography must have the same determinant as the original
//        assertEquals(Matrix.determinant(Hreal), Matrix.determinant(Hestm), tol);
//        // normalize both and check if both are the same (after normalizing)
//        RealMatrix HrealN = HomographyEstimate.normalizeHomography(Hreal);
//        RealMatrix HestmN = HomographyEstimate.normalizeHomography(Hestm);
//        NumericTestUtils.assert2dArrayEquals(HrealN.getData(), HestmN.getData(), tol);
//    }
//
//    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal (no optimization)
//    public void HomographyEstimatorTestEstimate10() {
//        HomographyEstimate he = new HomographyEstimate(true, false);
//        RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_B);
//        // System.out.println("He = \n" + Matrix.toString(Hestm));
//        // estimated homography must have the same determinant as the original
//        assertEquals(Matrix.determinant(Hreal), Matrix.determinant(Hestm), tol);
//        // normalize both and check if both are the same (after normalizing)
//        RealMatrix HrealN = HomographyEstimate.normalizeHomography(Hreal);
//        RealMatrix HestmN = HomographyEstimate.normalizeHomography(Hestm);
//        NumericTestUtils.assert2dArrayEquals(HrealN.getData(), HestmN.getData(), tol);
//    }
//
//    @Test   // check if the estimated homography between POINTS_A, POINTS_B is the same as Hreal (with optimization)
//    public void HomographyEstimatorTestEstimate11() {
//        HomographyEstimate he = new HomographyEstimate(true, true);
//        RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_B);
//        // System.out.println("He = \n" + Matrix.toString(Hestm));
//        // estimated homography must have the same determinant as the original
//        assertEquals(Matrix.determinant(Hreal), Matrix.determinant(Hestm), tol);
//        // normalize both and check if both are the same (after normalizing)
//        RealMatrix HrealN = HomographyEstimate.normalizeHomography(Hreal);
//        RealMatrix HestmN = HomographyEstimate.normalizeHomography(Hestm);
//        NumericTestUtils.assert2dArrayEquals(HrealN.getData(), HestmN.getData(), tol);
//    }
//
//    // -------------------------------------------------------------------------
//
//    @Test   // estimate homography from a noisy point set
//    public void HomographyEstimatorTestNoise10() {
//        // System.out.println("\n*************** WITHOUT NONLINEAR REFINEMENT *****************");
//        HomographyEstimate he = new HomographyEstimate(true, false);
//        Pnt2d[] POINTS_Bn = mapPointsNoisy(Hreal, POINTS_A, 0.01);
//        RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_Bn);
//        PrintPrecision.set(6); System.out.println("Hestm = \n" + Matrix.toString(Hestm));
//        System.out.println("Matrix.determinant(Hestm) =" + Matrix.determinant(Hestm));
//        assertTrue(Math.abs(Matrix.determinant(Hestm)) > 15);
//    }
//
//    @Test   // estimate homography from a noisy point set
//    public void HomographyEstimatorTestNoise11() {
//        // System.out.println("\n************** WITH NONLINEAR REFINEMENT *****************");
//        HomographyEstimate he = new HomographyEstimate(true, true);
//        Pnt2d[] POINTS_Bn = mapPointsNoisy(Hreal, POINTS_A, 0.01);
//        RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_Bn);
//        PrintPrecision.set(6); System.out.println("Hestm = \n" + Matrix.toString(Hestm));
//        System.out.println("Matrix.determinant(Hestm) =" + Matrix.determinant(Hestm));
//        assertTrue(Math.abs(Matrix.determinant(Hestm)) > 50);
//    }


    // @Test
    // public void HomographyEstimatorTestNois00() {
    //     // System.out.println("\n*************** WITHOUT NONLINEAR REFINEMENT *****************");
    //     HomographyEstimate he = new HomographyEstimate(false, false);
    //     Pnt2d[] POINTS_Bn = mapPointsNoisy(Hreal, POINTS_A, 0.5);
    //     RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_Bn);
    //     PrintPrecision.set(6); System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     double[][] Hexpd = {
    //             {-0.057000, 0.011172, 0.534589},
    //             {0.021708, -0.105175, 0.702193},
    //             {-0.105258, 0.016770, 1.000000}};
    //     NumericTestUtils.assert2dArrayEquals(Hexpd, Hestm.getData(), tol);
    // }
    //
    // @Test
    // public void HomographyEstimatorTestNoise11() {
    //     // System.out.println("\n*************** WITHOUT NONLINEAR REFINEMENT *****************");
    //     HomographyEstimate he = new HomographyEstimate(true, true);
    //     Pnt2d[] POINTS_Bn = mapPointsNoisy(Hreal, POINTS_A, 0.5);
    //     RealMatrix Hestm = he.estimateHomography(POINTS_A, POINTS_Bn);
    //     PrintPrecision.set(6); System.out.println("Hestm = \n" + Matrix.toString(Hestm));
    //     double[][] Hexpd = {
    //             {0.371007, 0.445814, 0.196152},
    //             {0.853196, 0.185271, 0.133509},
    //             {0.462759, 0.611521, 1.000000}};
    //     NumericTestUtils.assert2dArrayEquals(Hexpd, Hestm.getData(), tol);
    // }

    // @Test
    // public void HomographyEstimatorTestRefined() {
    //     // System.out.println("\n*************** WITH NONLINEAR REFINEMENT *****************");
    //     HomographyEstimate he = new HomographyEstimate(true, true);
    //     double[][] Hexpd = {
    //             {0.647555, 0.447761, -0.185451},
    //             {1.157419, 0.194929, 0.049175},
    //             {0.957998, 0.853874, 1.000000}};
    //     runHomographyTest(he, pntsA, pntsB, Hexpd, 0.183310, 0.131740);
    // }
    //
    // private static void runHomographyTest(HomographyEstimate he,
    //                                       Pnt2d[] pntsA, Pnt2d[] pntsB,
    //                                       double[][] Hexpd, double errExpd, double maxErrExpd) {
    //
    //     // estimate homography from pntsA -> pntsB
    //     RealMatrix Hest = he.estimateHomography(pntsA, pntsB);
    //    // System.out.println("H (estim.) = ");
    //    // System.out.println(Matrix.toString(Hest.getData()));
    //
    //     // check if estimated Hest matrix agrees with expected Hexpd
    //     for (int i = 0; i < Hexpd.length; i++) {
    //         assertArrayEquals(Hexpd[i], Hest.getData()[i], 1e-4);
    //     }
    //
    //     // apply Hest to point set pntsA -> pntsC
    //     Pnt2d[] pntsC = new Pnt2d[pntsA.length];
    //     for (int i = 0; i < pntsA.length; i++) {
    //         pntsC[i] = mapPointWithNoise(Hest, pntsA[i], 0.0);
    //     }
    //
    //     // analyze distances between pntsB and pntsC
    //     double sumDist2 = 0;
    //     double maxDist2 = Double.NEGATIVE_INFINITY;
    //     for (int i = 0; i < pntsA.length; i++) {
    //         Pnt2d b = pntsB[i];
    //         Pnt2d c = pntsC[i];
    //         double dist2 = b.distanceSq(c);
    //         sumDist2 += dist2;
    //         maxDist2 = Math.max(maxDist2, dist2);
    //         // System.out.format("(%.3f, %.3f) -> (%.3f, %.3f) d=%.4f\n", a.getX(), a.getY(), c.getX(), c.getY(), dist2);
    //     }
    //     // System.out.format("\nTotal error = %.6f\n", Math.sqrt(sumDist2));
    //     // System.out.format("Max. dist = %.6f\n", Math.sqrt(maxDist2));
    //     assertEquals(errExpd, Math.sqrt(sumDist2), 1e-4);
    //     assertEquals(maxErrExpd, Math.sqrt(maxDist2), 1e-4);
    // }

    // --------------------------------------------------

    private static Pnt2d[] mapPoints(RealMatrix H, Pnt2d[] P) {
        Pnt2d[] Q = new Pnt2d[P.length];
        for (int i = 0; i < P.length; i++) {
            double[] xa = P[i].toDoubleArray();
            double[] xb = HomographyEstimate.transform(xa, H);
            Q[i] = Pnt2d.from(xb);
        }
        return Q;
    }

    private static Pnt2d[] mapPointsNoisy(RealMatrix H, Pnt2d[] P, double noise) {
        Random rand = new DeterministicRandom(17);
        Pnt2d[] Q = new Pnt2d[P.length];
        for (int i = 0; i < P.length; i++) {
            double[] xa = P[i].toDoubleArray();
            double[] xb = HomographyEstimate.transform(xa, H);
            System.out.format("xb = %.6f -> ", xb[0]);
            xb[0] += noise * rand.nextDouble(1.0);
            System.out.format(" %.6f\n", xb[0]);
            xb[1] += noise * rand.nextDouble(1.0);
            Q[i] = Pnt2d.from(xb);
        }
        return Q;
    }

    private static Pnt2d mapPointWithNoise(RealMatrix H, Pnt2d p, double noise) {
        Random rand = new DeterministicRandom(17);
        //Random rand = new Random(17);  // use fixed random seed!
        double[] xa = {p.getX(), p.getY()};
        double[] xb = HomographyEstimate.transform(xa, H);
        double xn = noise * rand.nextGaussian();
        double yn = noise * rand.nextGaussian();
        return Pnt2d.from(xb[0] + xn, xb[1] + yn);
    }


    // private static Pnt2d mapPointsWithNoise(RealMatrix H, Pnt2d[] p, double noise) {
    //     Random rand = new DeterministicRandom(17);
    //     //Random rand = new Random(17);  // use fixed random seed!
    //     double[] xa = {p.getX(), p.getY()};
    //     double[] xb = HomographyEstimate.transform(xa, H);
    //     double xn = noise * rand.nextGaussian();
    //     double yn = noise * rand.nextGaussian();
    //     return Pnt2d.from(xb[0] + xn, xb[1] + yn);
    // }


}