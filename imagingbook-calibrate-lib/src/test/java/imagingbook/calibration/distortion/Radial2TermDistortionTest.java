/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import imagingbook.testutils.DeterministicRandom;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class Radial2TermDistortionTest {
    
    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05;

    @Test
    public void constructorTest1() {
        LensDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double[] params = ldm.getParameters();
        assertEquals(2, params.length);
        assertEquals(k0, params[0], tol);
        assertEquals(k1, params[1], tol);
        assertEquals(2, ldm.getParameterCount());
        assertEquals(k0, ldm.getParameter(0), tol);
        assertEquals(k1, ldm.getParameter(1), tol);
    }

    @Test
    public void constructorTest2() {
        LensDistortion ldm = new Radial2TermDistortion();
        double[] params = ldm.getParameters();
        assertEquals(2, params.length);
        assertEquals(0, params[0], tol);
        assertEquals(0, params[1], tol);
        assertEquals(2, ldm.getParameterCount());
        assertEquals(0, ldm.getParameter(0), tol);
        assertEquals(0, ldm.getParameter(1), tol);
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest1() {
        LensDistortion ldm = new Radial2TermDistortion(new double[] {0.1});
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest3() {
        LensDistortion ldm = new Radial2TermDistortion(new double[] {0.1, 0.7, 0});
    }

    // -------------------------------------------------------------------------

    @Test
    public void copyOfTest() {
        LensDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});

        LensDistortion ldm2 = ldm.copyOf();
        assertNotNull(ldm2);
        assertEquals(k0, ldm2.getParameter(0), tol);
        assertEquals(k1, ldm2.getParameter(1), tol);

        LensDistortion ldm3 = ldm.copyOf(new double[] {0.4, -0.1});
        assertEquals(0.4, ldm3.getParameter(0), tol);
        assertEquals(-0.1, ldm3.getParameter(1), tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTest() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double r1 = 0.35;
        double r2 = ldm.fRad(r1);
        assertEquals(0.358312, r2, tol);
        double r3 = ldm.fRadInv(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadTestZero() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double r0 = ldm.fRad(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void fRadInvTest() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double r1 = 0.35;
        double r2 = ldm.fRadInv(r1);
        assertEquals(0.3422189, r2, tol);
        double r3 = ldm.fRad(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadInvTestZero() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double r0 = ldm.fRadInv(0.0);
        assertEquals(0.0, r0, tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTestRandom() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        Random rand = new DeterministicRandom(37);
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
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.warp(xy1);
        assertArrayEquals(new double[] {0.314062, -0.41875}, xy2, tol);
        double[] xy3 = ldm.unwarp(xy2);
        assertArrayEquals(xy1, xy3, tol);
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadInvXyTest() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.unwarp(xy1);
        assertArrayEquals(new double[] {0.287549, -0.383399}, xy2, tol);
        double[] xy3 = ldm.warp(xy2);
        assertArrayEquals(xy1, xy3, tol);
    }

    @Test
    public void fRadXyTestZero() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadInvXyTestZero() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double[] xy0 = ldm.unwarp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadXyTestRandom() {
        RadialDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
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
        LensDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double x = 0.3, y = -0.6, du = 210, dv = 19;
        double[] rowU = ldm.getDMatrixRowU(x, y, du, dv);
        assertEquals(ldm.getParameterCount(), rowU.length);
        // System.out.println(Arrays.toString(rowU));
        assertArrayEquals(new double[] {94.4999999, 42.5249999}, rowU, tol);
    }

    @Test
    public void getDMatrixRowVTest() {
        LensDistortion ldm = new Radial2TermDistortion(new double[] {k0, k1});
        double x = 0.3, y = -0.6, du = 210, dv = 19;
        double[] rowV = ldm.getDMatrixRowV(x, y, du, dv);
        assertEquals(ldm.getParameterCount(), rowV.length);
        // System.out.println(Arrays.toString(rowV));
        assertArrayEquals(new double[] {8.5499999, 3.8474999}, rowV, tol);
    }

}