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

public class LensDistortionModelTest {
    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05;

    @Test
    public void copyOfTest1() {
        LensDistortionModel m1 = new Radial2TermDistortionModel();
        LensDistortionModel m2 = m1.copyOf(new double[] {k0, k1}, 0);
        assertArrayEquals(new double[] {k0, k1}, m2.getParameters(), tol);
        assertEquals(Radial2TermDistortionModel.class, m1.getClass());
        assertEquals(Radial2TermDistortionModel.class, m2.getClass());
    }

    @Test (expected = IllegalArgumentException.class)
    public void copyOfTest2() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        LensDistortionModel m2 = m1.copyOf(new double[] {k0, k1, 0.1}, 0);    // too many arguments
    }

    @Test (expected = IllegalArgumentException.class)
    public void copyOfTest3() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        LensDistortionModel m2 = m1.copyOf(new double[] {0.1}, 0);    // too few arguments
    }

    @Test
    public void getParameterCountTest() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        assertEquals(2, m1.getParameterCount());
        LensDistortionModel m2 = new Radial2TermDistortionModel();
        assertEquals(2, m2.getParameterCount());
    }

    @Test
    public void getParametersTest() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        assertArrayEquals(new double[] {k0, k1}, m1.getParameters(), tol);
        LensDistortionModel m2 = new Radial2TermDistortionModel();
        assertArrayEquals(new double[] {0, 0}, m2.getParameters(), tol);
    }

    @Test
    public void getParameterTest1() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        assertEquals(k0, m1.getParameter(0), tol);
        assertEquals(k1, m1.getParameter(1), tol);
        LensDistortionModel m2 = new Radial2TermDistortionModel();
        assertEquals(0, m2.getParameter(0), tol);
        assertEquals(0, m2.getParameter(1), tol);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getParameterTest2() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        m1.getParameter(-1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getParameterTest3() {
        LensDistortionModel m1 = new Radial2TermDistortionModel(new double[] {k0, k1});
        m1.getParameter(2);
    }
}