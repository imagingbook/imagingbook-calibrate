/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HomographyEstimatorTest {

    private static double NOISE = 0.1;
    private static Random rand = new Random(17);

    @Test
    public void test1() {
        PrintPrecision.set(6);
        RealMatrix Hreal = MatrixUtils.createRealMatrix(new double[][] {
                {3, 2, -1},
                {5, 0, 2},
                {4, 4, 9}});
        double[][] HrealN = Hreal.scalarMultiply(1/Hreal.getEntry(2, 2)).getData();
        // System.out.println("H (real normalized) = \n" + Matrix.toString(HrealN));

        List<Pnt2d> pntlistA = new ArrayList<Pnt2d>();
        pntlistA.add(Pnt2d.from(10, 7));
        pntlistA.add(Pnt2d.from(3, -1));
        pntlistA.add(Pnt2d.from(5, 5));
        pntlistA.add(Pnt2d.from(-6, 13));
        pntlistA.add(Pnt2d.from(0, 1));
        pntlistA.add(Pnt2d.from(2, 3));

        List<Pnt2d> pntlistB = new ArrayList<Pnt2d>();
        for (Pnt2d a : pntlistA) {
            pntlistB.add(mapPointWithNoise(Hreal, a, NOISE));
        }

        Pnt2d[] pntsA = pntlistA.toArray(new Pnt2d[0]);
        Pnt2d[] pntsB = pntlistB.toArray(new Pnt2d[0]);

        // System.out.println("\nPoint correspondences:");
        // for (int i = 0; i < pntsA.length; i++) {
        //     Pnt2d a = pntsA[i];
        //     Pnt2d b = pntsB[i];
        //     System.out.format("(%.4f, %.4f) -> (%.4f, %.4f)\n", a.getX(), a.getY(), b.getX(), b.getY());
        // }
        // System.out.println();

        {
            // System.out.println("\n*************** WITHOUT NONLINEAR REFINEMENT *****************");
            HomographyEstimator he = new HomographyEstimator(true, false);
            double[][] Hexpd = {
                    {0.116124, 0.150375, 0.056940},
                    {0.333777, -0.097615, 0.255556},
                    {0.046961, 0.285810, 1.000000}};
            runTest(he, pntsA, pntsB, Hexpd, 0.603410, 0.519087);
        }
        {
            // System.out.println("\n*************** WITH NONLINEAR REFINEMENT *****************");
            HomographyEstimator he = new HomographyEstimator(true, true);
            double[][] Hexpd = {
                    {0.647555, 0.447761, -0.185451},
                    {1.157419, 0.194929, 0.049175},
                    {0.957998, 0.853874, 1.000000}};
            runTest(he, pntsA, pntsB, Hexpd, 0.183310, 0.131740);
        }
    }

    private static void runTest(HomographyEstimator he, Pnt2d[] pntsA, Pnt2d[] pntsB,
                                double[][] Hexpd, double errExpd, double maxErrExpd) {
        RealMatrix Hest = he.estimateHomography(pntsA, pntsB);
       // System.out.println("H (estim.) = ");
       // System.out.println(Matrix.toString(Hest.getData()));

        for (int i = 0; i < Hexpd.length; i++) {
            assertArrayEquals(Hexpd[i], Hest.getData()[i], 1e-4);
        }

        Pnt2d[] pntsC = new Pnt2d[pntsA.length];
        for (int i = 0; i < pntsA.length; i++) {
            pntsC[i] = mapPoint(Hest, pntsA[i]);
        }

        // System.out.println("\nPoints mapped:");
        double sumDist2 = 0;
        double maxDist2 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < pntsA.length; i++) {
            Pnt2d a = pntsA[i];
            Pnt2d b = pntsB[i];
            Pnt2d c = pntsC[i];
            double dist2 = b.distanceSq(c);
            sumDist2 += dist2;
            maxDist2 = Math.max(maxDist2, dist2);
            // System.out.format("(%.3f, %.3f) -> (%.3f, %.3f) d=%.4f\n", a.getX(), a.getY(), c.getX(), c.getY(), dist2);
        }
        // System.out.format("\nTotal error = %.6f\n", Math.sqrt(sumDist2));
        // System.out.format("Max. dist = %.6f\n", Math.sqrt(maxDist2));
        assertEquals(errExpd, Math.sqrt(sumDist2), 1e-4);
        assertEquals(maxErrExpd, Math.sqrt(maxDist2), 1e-4);
    }

    private static Pnt2d mapPoint(RealMatrix H, Pnt2d p) {
        double[] xa = {p.getX(), p.getY()};
        double[] xb = HomographyEstimator.transform(xa, H);
        return Pnt2d.from(xb[0], xb[1]);
    }

    private static Pnt2d mapPointWithNoise(RealMatrix H, Pnt2d p, double noise) {
        double[] xa = {p.getX(), p.getY()};
        double[] xb = HomographyEstimator.transform(xa, H);
        double xn = noise * rand.nextGaussian();
        double yn = noise * rand.nextGaussian();
        return Pnt2d.from(xb[0] + xn, xb[1] + yn);
    }
}