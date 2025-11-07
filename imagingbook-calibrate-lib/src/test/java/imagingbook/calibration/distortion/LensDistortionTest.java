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

public class LensDistortionTest {
    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05;

    @Test
    public void copyOfTest1() {
        LensDistortion m1 = new Radial2TermDistortion();
        LensDistortion m2 = m1.copyOf(new double[] {k0, k1});
        assertArrayEquals(new double[] {k0, k1}, m2.getParameters(), tol);
        assertEquals(Radial2TermDistortion.class, m1.getClass());
        assertEquals(Radial2TermDistortion.class, m2.getClass());
    }

    @Test (expected = IllegalArgumentException.class)
    public void copyOfTest2() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        LensDistortion m2 = m1.copyOf(new double[] {k0, k1, 0.1});    // too many arguments
    }

    @Test (expected = IllegalArgumentException.class)
    public void copyOfTest3() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        LensDistortion m2 = m1.copyOf(new double[] {0.1});    // too few arguments
    }

    @Test
    public void getParameterCountTest() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        assertEquals(2, m1.getParameterCount());
        LensDistortion m2 = new Radial2TermDistortion();
        assertEquals(2, m2.getParameterCount());
    }

    @Test
    public void getParametersTest() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        assertArrayEquals(new double[] {k0, k1}, m1.getParameters(), tol);
        LensDistortion m2 = new Radial2TermDistortion();
        assertArrayEquals(new double[] {0, 0}, m2.getParameters(), tol);
    }

    @Test
    public void getParameterTest1() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        assertEquals(k0, m1.getParameter(0), tol);
        assertEquals(k1, m1.getParameter(1), tol);
        LensDistortion m2 = new Radial2TermDistortion();
        assertEquals(0, m2.getParameter(0), tol);
        assertEquals(0, m2.getParameter(1), tol);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getParameterTest2() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        m1.getParameter(-1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getParameterTest3() {
        LensDistortion m1 = new Radial2TermDistortion(new double[] {k0, k1});
        m1.getParameter(2);
    }
}