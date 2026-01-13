/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.testutils.DeterministicRandom;
import org.junit.Test;

import java.util.random.RandomGenerator;

import static org.junit.Assert.*;

public class Radial3TermDistortionTest {

    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05, k2 = 0.02;

    @Test
    public void constructorTest1() {
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double[] params = ldm.getParameters();
        assertEquals(3, params.length);
        assertEquals(k0, params[0], tol);
        assertEquals(k1, params[1], tol);
        assertEquals(k2, params[2], tol);
        assertEquals(3, ldm.getParameterCount());
        assertEquals(k0, ldm.getParameter(0), tol);
        assertEquals(k1, ldm.getParameter(1), tol);
        assertEquals(k2, ldm.getParameter(2), tol);
    }

    @Test
    public void constructorTest2() {
        DistortionModel ldm = new Radial3TermDistortionModel();
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
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {0.1});
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest2() {
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {0.1, 0.7});
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest3() {
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {0.1, 0.7, 0, -0.1});
    }

    // -------------------------------------------------------------------------

    @Test
    public void copyOfTest() {
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});

        DistortionModel ldm2 = ldm.from(ldm.getParameters());
        assertNotNull(ldm2);
        assertEquals(k0, ldm2.getParameter(0), tol);
        assertEquals(k1, ldm2.getParameter(1), tol);
        assertEquals(k2, ldm2.getParameter(2), tol);

        DistortionModel ldm3 = ldm.from(new double[] {0.4, -0.1, 0.5});
        assertEquals(0.4, ldm3.getParameter(0), tol);
        assertEquals(-0.1, ldm3.getParameter(1), tol);
        assertEquals(0.5, ldm3.getParameter(2), tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double r1 = 0.35;
        double r2 = ldm.fRad(r1);
        assertEquals(0.358325, r2, tol);
        double r3 = ldm.fRadInv(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double r0 = ldm.fRad(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void fRadInvTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double r1 = 0.35;
        double r2 = ldm.fRadInv(r1);
        assertEquals(0.3422086, r2, tol);
        double r3 = ldm.fRad(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadInvTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double r0 = ldm.fRadInv(0.0);
        assertEquals(0.0, r0, tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTestRandom() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        RandomGenerator rand = new DeterministicRandom(37);
        for (int i = 0; i < 100; i++) {
            double r1 = rand.nextDouble();
            double r2 = ldm.fRad(r1);
            double r3 = ldm.fRadInv(r2);
            assertEquals(r1, r3, tol);
        }
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadXyTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.warp(xy1);
        assertArrayEquals(new double[] {0.314156, -0.418875}, xy2, tol);
        double[] xy3 = ldm.unwarp(xy2);
        assertArrayEquals(xy1, xy3, tol);
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadInvXyTest() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.unwarp(xy1);
        assertArrayEquals(new double[] {0.287488, -0.383317}, xy2, tol);
        double[] xy3 = ldm.warp(xy2);
        assertArrayEquals(xy1, xy3, tol);
    }

    @Test
    public void fRadXyTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadInvXyTestZero() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double[] xy0 = ldm.unwarp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadXyTestRandom() {
        RadialDistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        RandomGenerator rand = new DeterministicRandom(37);
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
        DistortionModel ldm = new Radial3TermDistortionModel(new double[] {k0, k1, k2});
        double x = 0.3, y = -0.6, du = 210, dv = 19;
        double[][] rowsUV = ldm.getDMatrixRowsUV(x, y, du, dv);
        assertEquals(2, rowsUV.length);
        assertEquals(ldm.getParameterCount(), rowsUV[0].length);
        assertEquals(ldm.getParameterCount(), rowsUV[1].length);
        assertArrayEquals(new double[] {94.4999999, 42.5249999, 19.1362499}, rowsUV[0], tol);
        assertArrayEquals(new double[] {8.5499999, 3.8474999, 1.7313749}, rowsUV[1], tol);
    }
}