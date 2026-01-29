/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial2TermDistortion;
import imagingbook.calibrate.distortion.Radial3TermDistortion;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.SimpleCamera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.testutils.DeterministicRandom;
import org.junit.Test;

import java.util.Arrays;
import java.util.random.RandomGenerator;

import static org.junit.Assert.*;

public class ParameterVectorBuilderTest {

    @Test
    public void getParametersSimpleCameraTest() {
        int K = 7;
        double[] linParams = new double[] {500, 300, 200};
        double[] distParams = new double[] {0.2, -0.1};
        DistortionModel distortion = new Radial2TermDistortion(distParams);
        Camera cam = new SimpleCamera(linParams, distortion);
        ViewTransform[] views = makeRandomViews(K);

        ParameterVectorBuilder builder = new ParameterVectorBuilder(cam, K);
        double[] p = builder.getParameters(cam, Arrays.asList(views));
        assertEquals(cam.getParameterCount() + K * ViewTransform.PARAMETER_COUNT, p.length);

        double[] lp = builder.getLinearCameraParameters(p);
        assertArrayEquals(linParams, lp, 1e-6);

        double[] dp = builder.getDistortionParameters(p);
        assertArrayEquals(distParams, dp, 1e-6);

        for (int k = 0; k < K; k++) {
            assertArrayEquals(views[k].getParameters(), builder.getViewParameters(p, k), 1e-6);
        }
    }

    @Test
    public void getParametersStandardCameraTest() {
        int K = 11;
        double[] linParams = new double[] {500, 510, 0.2, 300, 200};
        double[] distParams = new double[] {0.2, -0.1, 0.4};
        DistortionModel distortion = new Radial3TermDistortion(distParams);
        Camera cam = new StandardCamera(linParams, distortion);
        ViewTransform[] views = makeRandomViews(K);

        ParameterVectorBuilder builder = new ParameterVectorBuilder(cam, K);
        double[] p = builder.getParameters(cam, Arrays.asList(views));
        assertEquals(cam.getParameterCount() + K * ViewTransform.PARAMETER_COUNT, p.length);

        double[] lp = builder.getLinearCameraParameters(p);
        assertArrayEquals(linParams, lp, 1e-6);

        double[] dp = builder.getDistortionParameters(p);
        assertArrayEquals(distParams, dp, 1e-6);

        for (int k = 0; k < K; k++) {
            assertArrayEquals(views[k].getParameters(), builder.getViewParameters(p, k), 1e-6);
        }
    }



    private static ViewTransform[] makeRandomViews(int K) {
        RandomGenerator rg = new DeterministicRandom(117);
        ViewTransform[] views = new ViewTransform[K];
        for (int k = 0; k < K; k++) {
            double[] V = new double[ViewTransform.PARAMETER_COUNT];
            for (int i = 0; i < V.length; i++) {
                V[i] = rg.nextDouble(10);
            }
            views[k] = new ViewTransform(V);
        }
        return views;
    }
}