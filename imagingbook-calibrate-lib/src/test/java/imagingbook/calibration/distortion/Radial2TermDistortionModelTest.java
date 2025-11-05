/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class Radial2TermDistortionModelTest {
    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05;

    @Test
    public void copyOfTest1() {
        Radial2TermDistortionModel m1 = new Radial2TermDistortionModel();
        assertArrayEquals(new double[] {0, 0}, m1.getParameters(), tol);
        Radial2TermDistortionModel m2 = m1.copyOf(k0, k1);
        assertArrayEquals(new double[] {k0, k1}, m2.getParameters(), tol);
        assertNotEquals(m1, m2);
    }

    @Test
    public void getParametersTest() {
        Radial2TermDistortionModel m1 = new Radial2TermDistortionModel(k0, k1);
        assertArrayEquals(new double[] {k0, k1}, m1.getParameters(), tol);
        Radial2TermDistortionModel m2 = new Radial2TermDistortionModel();
        assertArrayEquals(new double[] {0, 0}, m2.getParameters(), tol);
    }

    @Test
    public void getDMatrixRowUTest() {
        double x = 0.4, y = -0.1, du = 10, dv = -5;
        LensDistortionModel m1 = new Radial2TermDistortionModel(k0, k1);
        double[] rowU = m1.getDMatrixRowU(x, y, du, dv);
        assertArrayEquals(new double[] {1.700000, 0.289000}, rowU, tol);
        // System.out.println(Arrays.toString(rowU));
    }

    @Test
    public void getDMatrixRowVTest() {
        double x = 0.4, y = -0.1, du = 10, dv = -5;
        LensDistortionModel m1 = new Radial2TermDistortionModel(k0, k1);
        double[] rowV = m1.getDMatrixRowV(x, y, du, dv);
        assertArrayEquals(new double[] {-0.850000, -0.144500}, rowV, tol);
        // System.out.println(Arrays.toString(rowV));
    }

    @Test
    public void warpRTest() {
        double r1 = 0.35;
        RadialDistortionModel dist = new Radial2TermDistortionModel(k0, k1);
        double r2 = dist.warp(r1);
        // System.out.println(r2);
        assertEquals(0.358312390625, r2, tol);
        double r3 = dist.unwarp(r2);
        assertEquals(r1, r3, tol);
        double r0 = dist.warp(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void unwarpRTest() {
        double r1 = 0.35;
        RadialDistortionModel dist = new Radial2TermDistortionModel(k0, k1);
        double r2 = dist.unwarp(r1);
        // System.out.println(r2);
        assertEquals(0.3422189, r2, tol);
        double r3 = dist.warp(r2);
        assertEquals(r1, r3, tol);
        double r0 = dist.unwarp(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void warpXYTest() {
        double[] xy1 = {0.4, -0.1};
        LensDistortionModel m1 = new Radial2TermDistortionModel(k0, k1);
        double[] xy2 = m1.warp(xy1);
        // System.out.println(Arrays.toString(xy2));
        assertArrayEquals(new double[] {0.413022, -0.103255}, xy2, tol);
        double[] xy3 = m1.unwarp(xy2);
        assertArrayEquals(xy1, xy3, tol);

        double[] xy0 = {0, 0};
        assertArrayEquals(xy0, m1.warp(xy0), tol);
    }

    @Test
    public void unwarpXYTest() {
        double[] xy1 = {0.4, -0.1};
        LensDistortionModel m1 = new Radial2TermDistortionModel(k0, k1);
        double[] xy2 = m1.unwarp(xy1);
        // System.out.println(Arrays.toString(xy2));
        assertArrayEquals(new double[] {0.388077, -0.097019}, xy2, tol);
        double[] xy3 = m1.warp(xy2);
        assertArrayEquals(xy1, xy3, tol);

        double[] xy0 = {0, 0};
        assertArrayEquals(xy0, m1.unwarp(xy0), tol);
    }
}