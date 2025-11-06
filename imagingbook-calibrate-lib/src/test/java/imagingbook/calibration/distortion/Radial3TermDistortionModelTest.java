/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.testutils.DeterministicRandom;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class Radial3TermDistortionModelTest {

    static final double tol = 1e-6;
    static final double k1 = 0.2, k2 = -0.05, k3 = 0.02;

    @Test
    public void constructorTest1() {
        LensDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double[] params = ldm.getParameters();
        assertEquals(3, params.length);
        assertEquals(k1, params[0], tol);
        assertEquals(k2, params[1], tol);
        assertEquals(k3, params[2], tol);
        assertEquals(3, ldm.getParameterCount());
        assertEquals(k1, ldm.getParameter(0), tol);
        assertEquals(k2, ldm.getParameter(1), tol);
        assertEquals(k3, ldm.getParameter(2), tol);
    }

    @Test
    public void constructorTest2() {
        LensDistortionModel ldm = new Radial3TermDistortionModel();
        double[] params = ldm.getParameters();
        assertEquals(3, params.length);
        assertEquals(0, params[0], tol);
        assertEquals(0, params[1], tol);
        assertEquals(0, params[2], tol);
        assertEquals(3, ldm.getParameterCount());
        assertEquals(0, ldm.getParameter(0), tol);
        assertEquals(0, ldm.getParameter(1), tol);
        assertEquals(0, ldm.getParameter(2), tol);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest1() {
        LensDistortionModel ldm = new Radial3TermDistortionModel(0.1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest2() {
        LensDistortionModel ldm = new Radial3TermDistortionModel(0.1, 0.7);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest3() {
        LensDistortionModel ldm = new Radial3TermDistortionModel(0.1, 0.7, 0, -0.1);
    }

    // -------------------------------------------------------------------------

    @Test
    public void copyOfTest() {
        LensDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);

        LensDistortionModel ldm2 = ldm.copyOf();
        assertNotNull(ldm2);
        assertEquals(k1, ldm2.getParameter(0), tol);
        assertEquals(k2, ldm2.getParameter(1), tol);
        assertEquals(k3, ldm2.getParameter(2), tol);

        LensDistortionModel ldm3 = ldm.copyOf(0.4, -0.1, 0.5);
        assertEquals(0.4, ldm3.getParameter(0), tol);
        assertEquals(-0.1, ldm3.getParameter(1), tol);
        assertEquals(0.5, ldm3.getParameter(2), tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void warpTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double r1 = 0.35;
        double r2 = ldm.warp(r1);
        assertEquals(0.358325, r2, tol);
        double r3 = ldm.unwarp(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void warpTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double r0 = ldm.warp(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void unwarpTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double r1 = 0.35;
        double r2 = ldm.unwarp(r1);
        assertEquals(0.3422086, r2, tol);
        double r3 = ldm.warp(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void unwarpTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double r0 = ldm.unwarp(0.0);
        assertEquals(0.0, r0, tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void warpTestRandom() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        Random rand = new DeterministicRandom(37);
        for (int i = 0; i < 100; i++) {
            double r1 = rand.nextDouble();
            double r2 = ldm.warp(r1);
            double r3 = ldm.unwarp(r2);
            assertEquals(r1, r3, tol);
        }
    }

    // -------------------------------------------------------------------------

    @Test
    public void warpXyTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.warp(xy1);
        assertArrayEquals(new double[] {0.314156, -0.418875}, xy2, tol);
        double[] xy3 = ldm.unwarp(xy2);
        assertArrayEquals(xy1, xy3, tol);
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void unwarpXyTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.unwarp(xy1);
        assertArrayEquals(new double[] {0.287488, -0.383317}, xy2, tol);
        double[] xy3 = ldm.warp(xy2);
        assertArrayEquals(xy1, xy3, tol);
    }

    @Test
    public void warpXyTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void unwarpXyTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        double[] xy0 = ldm.unwarp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void warpXyTestRandom() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(k1, k2, k3);
        Random rand = new DeterministicRandom(37);
        for (int i = 0; i < 100; i++) {
            double[] xy1 = {2 * rand.nextDouble() - 1, 2 * rand.nextDouble() - 1};
            double[] xy2 = ldm.warp(xy1);
            double[] xy3 = ldm.unwarp(xy2);
            assertArrayEquals(xy1, xy3, tol);
        }
    }

    // -------------------------------------------------------------------------

    @Test
    public void getDMatrixRowUTest() {
    }

    @Test
    public void getDMatrixRowVTest() {
    }
}