/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.common.math.Matrix;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class OverallNonlinearOptimizerTest {

    static final double tol = 1e-6;

    // @Test
    // public void makeParameterScalesTest() {
    //     int M = 4;
    //     double[] camScales = {1, 2, 3, 4, 5};
    //     double[] distScales = {11, 12, 13};
    //     double[] viewScales = {21, 22, 23, 24, 25, 26};
    //     // double[] scales = new double[camScales.length + distScales.length + M * viewScales.length];
    //
    //     double[] scales = OverallNonlinearOptimizer.makeParameterScales(camScales, distScales, viewScales, M);
    //     System.out.println(Matrix.toString(scales));
    //     assertEquals(camScales.length + distScales.length + M * viewScales.length, scales.length);
    //
    //     int start = 0;
    //     // check insertion of camera parameter scales
    //     assertArrayEquals(camScales, Arrays.copyOfRange(scales, start, camScales.length), tol);
    //     start += camScales.length;
    //     // check insertion of distortion parameter scales
    //     assertArrayEquals(distScales, Arrays.copyOfRange(scales, start, start + distScales.length), tol);
    //     start += distScales.length;
    //     // check insertion of view parameter scales
    //     for (int k = 0; k < M; k++) {
    //         assertArrayEquals(viewScales, Arrays.copyOfRange(scales, start, start + viewScales.length), tol);
    //         start += viewScales.length;
    //     }
    // }
}