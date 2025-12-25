/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import imagingbook.testutils.DeterministicRandom;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.Assert.*;

public class RadialLateralDistortionTest {

    static final double tol = 1e-6;
    // static final double[] demoParams = {0.2, -0.1, 0.15, -0.2, 0.2 };   // p1, p2 should not be greater than 0.2 for unwarp() convergence!
    static final double[] demoParams = {0.2, -0.05, 0.02, -0.2, 0.2 };   // p1, p2 should not be greater than 0.2 for unwarp() convergence!

    @Test
    public void copyOf() {
    }

    @Test
    public void getParameters1() {
        RadialLateralDistortion distortion = new RadialLateralDistortion();
        assertEquals(RadialLateralDistortion.PARAM_COUNT, distortion.getParameterCount());
        assertArrayEquals(new double[] {0,0,0,0,0}, distortion.getParameters(), tol);
    }
    @Test

    public void getParameters2() {
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        assertEquals(RadialLateralDistortion.PARAM_COUNT, distortion.getParameterCount());
        assertArrayEquals(demoParams, distortion.getParameters(), tol);
    }


    @Test
    public void getDMatrixRowsUV() {
    }

    @Test
    public void warpTest1() {   // zero distortion
        RadialLateralDistortion distortion = new RadialLateralDistortion();
        double[] p1 = {0.4, 0.7};
        double[] p2 = distortion.warp(p1);
        // System.out.println("p2 = " + Arrays.toString(p2));
        assertArrayEquals(p1, p2, tol);
    }

    @Test
    public void warpTest2() {
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        double[] p1 = {0.4, 0.7};
        double[] p2 = distortion.warp(p1);
        //System.out.println("p2 = " + Arrays.toString(p2));
        assertArrayEquals(new double[] {0.527747, 0.56605725}, p2, tol);
    }

    @Test
    public void warpTest3() {
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        double[] p1 = {-1, -1};
        // System.out.println("p1 = " + Arrays.toString(p1));
        double[] p2 = distortion.warp(p1);
        // System.out.println("p2 = " + Arrays.toString(p2));
        assertArrayEquals(new double[] {-0.95999999, -1.7599999999}, p2, tol);
    }

    @Test
    public void unwarpTest1() {
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        double[] p1 = {0.3, -0.4};
        // System.out.println("p1 = " + Arrays.toString(p1));
        double[] p2 = distortion.unwarp(p1);
        // System.out.println("p2 = " + Arrays.toString(p2));
        assertArrayEquals(new double[] {0.22027127324820892, -0.3012084856025464}, p2, tol);
    }

    @Test
    public void unwarpTestUnitPoints() {
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        double[][] points = {
                {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, -1}
        };
        for (double[] p1 : points) {
            System.out.println("p1 = " + Arrays.toString(p1));
            double[] p2 = distortion.warp(p1);
            System.out.println("p2 = " + Arrays.toString(p2));
            double[] p3 = distortion.unwarp(p2);
            // System.out.println("p3 = " + Arrays.toString(p3));
            assertArrayEquals(p1, p3, tol);
        }
    }

    @Test   // shaky!! fails for x,y in [-1, +1]
    public void unwarpTestRandom() {
        int N = 100;
        RandomGenerator rand = new DeterministicRandom(17);
        RadialLateralDistortion distortion = new RadialLateralDistortion(demoParams);
        for (int i = 0; i < N; i++) {
            double[] p1 = {1 * rand.nextDouble() - 0.5, 1 * rand.nextDouble() - 0.5};
            double[] p2 = distortion.warp(p1);
            double[] p3 = distortion.unwarp(p2);
            // System.out.println("p3 = " + Arrays.toString(p3));
            assertArrayEquals(p1, p3, tol);
        }
    }
}