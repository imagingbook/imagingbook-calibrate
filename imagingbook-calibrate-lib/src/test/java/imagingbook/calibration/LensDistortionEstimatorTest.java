/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.distortion.LensDistortion;
import imagingbook.calibration.distortion.Radial2TermDistortion;
import imagingbook.calibration.distortion.Radial3TermDistortion;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class LensDistortionEstimatorTest {

    static Pnt2d[] modelPts = ZhangData.getModelPoints();
    static Pnt2d[][] obsPts = ZhangData.getAllObservedPoints();
    static ViewTransform[] views = ZhangData.getAllViewTransforms();   // cheating a bit, using final views from Zhang
    // initial estimate obtained from Calibration
    static double[] camIntrinsics = {877.16, 876.80, 0.1751, 301.04, 220.41};


    @Test
    public void getEstimateRadial2TermTest() {
        Camera cam1 = new Camera(camIntrinsics, Radial2TermDistortion.INSTANCE);
        LensDistortion dist = LensDistortion.from(cam1, views, modelPts, obsPts);
        assertNotNull(dist);
        // System.out.println(Arrays.toString(dist.getParameters()));
        assertArrayEquals(new double[] {-1.6129, 6.5133}, dist.getParameters(), 1e-3);
    }

    @Test
    public void getEstimateRadial3TermTest() {
        Camera cam1 = new Camera(camIntrinsics, Radial3TermDistortion.INSTANCE);
        LensDistortion dist = LensDistortion.from(cam1, views, modelPts, obsPts);
        assertNotNull(dist);
        // System.out.println(Arrays.toString(dist.getParameters()));
        assertArrayEquals(new double[] {-2.1701, 18.4892, -57.64063}, dist.getParameters(), 1e-3);
    }

    // @Test
    // public void getDistortion() {
    // }
    //
    // @Test
    // public void getError() {
    // }
    //
    // @Test
    // public void testGetError() {
    // }
}