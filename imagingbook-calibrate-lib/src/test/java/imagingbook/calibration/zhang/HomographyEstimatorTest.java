/*
 *  This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge.
 * All rights reserved. Visit https://imagingbook.com for additional details.
 */
package imagingbook.calibration.zhang;

import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

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

        List<Point2D> pntlistA = new ArrayList<Point2D>();
        pntlistA.add(new Point2D.Double(10, 7));
        pntlistA.add(new Point2D.Double(3, -1));
        pntlistA.add(new Point2D.Double(5, 5));
        pntlistA.add(new Point2D.Double(-6, 13));
        pntlistA.add(new Point2D.Double(0, 1));
        pntlistA.add(new Point2D.Double(2, 3));

        List<Point2D> pntlistB = new ArrayList<Point2D>();
        for (Point2D a : pntlistA) {
            pntlistB.add(mapPointWithNoise(Hreal, a, NOISE));
        }

        Point2D[] pntsA = pntlistA.toArray(new Point2D[0]);
        Point2D[] pntsB = pntlistB.toArray(new Point2D[0]);

        // System.out.println("\nPoint correspondences:");
        // for (int i = 0; i < pntsA.length; i++) {
        //     Point2D a = pntsA[i];
        //     Point2D b = pntsB[i];
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

    private static void runTest(HomographyEstimator he, Point2D[] pntsA, Point2D[] pntsB,
                                double[][] Hexpd, double errExpd, double maxErrExpd) {
        RealMatrix Hest = he.estimateHomography(pntsA, pntsB);
       // System.out.println("H (estim.) = ");
       // System.out.println(Matrix.toString(Hest.getData()));

        for (int i = 0; i < Hexpd.length; i++) {
            assertArrayEquals(Hexpd[i], Hest.getData()[i], 1e-4);
        }

        Point2D[] pntsC = new Point2D[pntsA.length];
        for (int i = 0; i < pntsA.length; i++) {
            pntsC[i] = mapPoint(Hest, pntsA[i]);
        }

        // System.out.println("\nPoints mapped:");
        double sumDist2 = 0;
        double maxDist2 = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < pntsA.length; i++) {
            Point2D a = pntsA[i];
            Point2D b = pntsB[i];
            Point2D c = pntsC[i];
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

    private static Point2D mapPoint(RealMatrix H, Point2D p) {
        double[] xa = {p.getX(), p.getY()};
        double[] xb = HomographyEstimator.transform(xa, H);
        return new Point2D.Double(xb[0], xb[1]);
    }

    private static Point2D mapPointWithNoise(RealMatrix H, Point2D p, double noise) {
        double[] xa = {p.getX(), p.getY()};
        double[] xb = HomographyEstimator.transform(xa, H);
        double xn = noise * rand.nextGaussian();
        double yn = noise * rand.nextGaussian();
        return new Point2D.Double(xb[0] + xn, xb[1] + yn);
    }
}