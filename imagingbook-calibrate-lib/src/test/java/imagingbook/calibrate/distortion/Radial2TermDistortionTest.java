/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.testutils.DeterministicRandom;
import org.apache.commons.math4.legacy.analysis.solvers.LaguerreSolver;
import org.apache.commons.numbers.complex.Complex;
import org.junit.Test;

import java.util.Arrays;
import java.util.random.RandomGenerator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Radial2TermDistortionTest {
    
    static final double tol = 1e-6;
    static final double k0 = 0.2, k1 = -0.05;

    @Test
    public void constructorTest1() {
        DistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
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
        DistortionModel ldm = new Radial2TermDistortionModel();
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
        DistortionModel ldm = new Radial2TermDistortionModel(new double[] {0.1});
    }

    @Test (expected = IllegalArgumentException.class)
    public void constructorExceptionTest3() {
        DistortionModel ldm = new Radial2TermDistortionModel(new double[] {0.1, 0.7, 0});
    }

    // -------------------------------------------------------------------------

    @Test
    public void copyOfTest() {
        DistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});

        // DistortionModel ldm2 = ldm.from();
        // assertNotNull(ldm2);
        // assertEquals(k0, ldm2.getParameter(0), tol);
        // assertEquals(k1, ldm2.getParameter(1), tol);

        DistortionModel ldm3 = ldm.from(new double[] {0.4, -0.1});
        assertEquals(0.4, ldm3.getParameter(0), tol);
        assertEquals(-0.1, ldm3.getParameter(1), tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTest() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double r1 = 0.35;
        double r2 = ldm.fRad(r1);
        assertEquals(0.358312, r2, tol);
        double r3 = ldm.fRadInv(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadTestZero() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double r0 = ldm.fRad(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void fRadInvTest() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double r1 = 0.35;
        double r2 = ldm.fRadInv(r1);
        assertEquals(0.3422189, r2, tol);
        double r3 = ldm.fRad(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadInvTestZero() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double r0 = ldm.fRadInv(0.0);
        assertEquals(0.0, r0, tol);
    }

    // -------------------------------------------------------------------------

    @Test
    public void fRadTestRandom() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
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
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
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
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double[] xy1 = {0.3, -0.4};
        double[] xy2 = ldm.unwarp(xy1);
        assertArrayEquals(new double[] {0.287549, -0.383399}, xy2, tol);
        double[] xy3 = ldm.warp(xy2);
        assertArrayEquals(xy1, xy3, tol);
    }

    @Test
    public void fRadXyTestZero() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double[] xy0 = ldm.warp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadInvXyTestZero() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double[] xy0 = ldm.unwarp(new double[] {0, 0});
        assertArrayEquals(new double[] {0, 0}, xy0, tol);
    }

    @Test
    public void fRadXyTestRandom() {
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
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
        DistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        double x = 0.3, y = -0.6, du = 210, dv = 19;
        double[][] rowsUV = ldm.getDMatrixRowsUV(x, y, du, dv);
        assertEquals(2, rowsUV.length);
        assertEquals(ldm.getParameterCount(), rowsUV[0].length);
        assertEquals(ldm.getParameterCount(), rowsUV[1].length);
        assertArrayEquals(new double[] {94.4999999, 42.5249999}, rowsUV[0], tol);
        assertArrayEquals(new double[] {8.5499999, 3.8474999}, rowsUV[1], tol);
    }

    // -------------------------------------------------------------------------
    //      EXPERIMENTAL!!!
    // -------------------------------------------------------------------------

    @Test
    public void estimateInverseFunction2Test2() {
        double[] k = new double[] {k0, k1};
        System.out.println("k = " + Arrays.toString(k));
        Radial2TermDistortionModel ldm = new Radial2TermDistortionModel(k);
        double[] q = ldm.estimateInverseFunction2();
        System.out.println("q = " + Arrays.toString(q));
    }

    @Test
    public void estimateInverseFunction2Test3() {
        double[] k = new double[] {k0, k1};
        System.out.println("k = " + Arrays.toString(k));
        Radial2TermDistortionModel ldm = new Radial2TermDistortionModel(k);
        double[] q = ldm.estimateInverseFunction3();
        System.out.println("q = " + Arrays.toString(q));
    }

    @Test
    public void checkPolynomialInverse() {
        // f^-1(r') = r' - k0 y^3 + (3 k0^2 - k1) r'^5 + higher order terms
        RadialDistortionModel ldm = new Radial2TermDistortionModel(new double[] {k0, k1});
        RandomGenerator rand = new DeterministicRandom(37);
        for (int i = 0; i < 10; i++) {
            double r1 = rand.nextDouble();
            // System.out.println("r1 = " + r1);
            double r2 = ldm.fRad(r1);
            // System.out.println("r2 = " + r2);
            double r3 = inverseRad(r2);
            // System.out.println("r3 = " + r3);
            System.out.format("r1=%4f r2=%4f r3=%4f \n", r1, r2, r3);

        }
    }

    static double inverseRad(double rr) {
        // r' - k0 y^3 + (3 k0^2 - k1) r'^5;
        double rr3 = rr * rr * rr;
        double rr5 = rr3 * rr * rr;
        return rr -k0 * rr3 + (3 * k0 * k0 - k1) * rr5;
    }

    // -------------------------------------------------------------------------

    //@Test // check if f(r) is invertible for r in [a, b]
    public void checkIsMonotonicPolynomial() {
        /*
        We want to check if polynomial f(r) = r + k0 * r^3 + k1 * r^5 is monotonic.
        First deriv. is f'(c) = 1 + 3 k0 r^2 + 5 k1 r^4
        for r in [0, rmax]
         */
        double a = 0, b = 2;
        double[] coefficients = {1, 0, 3 * k0, 0, 5 * k1 };
        double rmx = 2;
        // UnivariateDifferentiableSolver inverseSolver = new NewtonRaphsonSolver();
        LaguerreSolver solver =  new LaguerreSolver();
        Complex[] roots = solver.solveAllComplex(coefficients, 0);
        System.out.println("Complex roots found: " + roots.length);
        int count = 0;
        for (Complex c : roots) {
            if (c.getReal() > a && c.getReal() < b && c.getImaginary() < tol) {
                count++;
                System.out.printf("  found real root at r = %.4f\n", c.getReal());
            }
        }
        if (count == 0) {
            System.out.printf("  f(r) is invertible in [%.2f, %.2f]\n", a, b);
        }
        else {
            System.out.printf("  f(r) is NOT invertible in [%.2f, %.2f]\n", a, b);
        }

    }



}