/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.util;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import imagingbook.testutils.DeterministicRandom;
import imagingbook.testutils.NumericTestUtils;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class PointStatisticsTest {

    @Test
    public void getNormalisationMappingTest() {
        Random rand = new DeterministicRandom(61);
        int xOffset = 100, yOffset = 70;
        int N = 100;
        Pnt2d[] pntsA = new Pnt2d[N];
        for (int i = 0; i < N; i++) {
            pntsA[i] = Pnt2d.from(rand.nextInt(600) + xOffset, rand.nextInt(400) + yOffset);
        }
        // calculate normalization mapping
        AffineMapping2D mapA = PointStatistics.getNormalisationMapping(pntsA);
        PrintPrecision.set(7);
        // System.out.println(Matrix.toString(mapA.getTransformationMatrix()));
        double[][] matR = {
                {0.0078230, 0.0000000, -3.0617705},
                {0.0000000, 0.0124156, -3.3979145},
                {0.0000000, 0.0000000, 1.0000000}};
        NumericTestUtils.assert2dArrayEquals(matR, mapA.getTransformationMatrix(), 1e-6);

        // normalise pntsA -> pntsB
        Pnt2d[] pntsB = mapA.applyTo(pntsA);

        // denormalize pntsB -> pntsC
        AffineMapping2D mapAi = mapA.getInverse();
        Pnt2d[] pntsC = mapAi.applyTo(pntsB);

        // check if pntsA == pntsC
        for (int i = 0; i < N; i++) {
            assertArrayEquals(pntsA[i].toDoubleArray(), pntsC[i].toDoubleArray(), 1e-6);
        }

        // check mean and variance:

        double[] x = new double[N];
        double[] y = new double[N];

        for (int i = 0; i < N; i++) {
            x[i] = pntsB[i].getX();
            y[i] = pntsB[i].getY();
        }

        // calculate the means in x/y
        double meanx = MathUtil.mean(x);
        double meany = MathUtil.mean(y);
        assertEquals(0, meanx, 1e-6);
        assertEquals(0, meany, 1e-6);

        // calculate the variances in x/y
        double varx = MathUtil.variance(x);
        double vary = MathUtil.variance(y);
        assertEquals(1, Math.sqrt(2 / varx), 1e-6);
        assertEquals(1, Math.sqrt(2 / vary), 1e-6);
    }
}