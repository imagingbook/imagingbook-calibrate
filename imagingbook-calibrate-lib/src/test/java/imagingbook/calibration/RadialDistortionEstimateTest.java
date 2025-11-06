/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.distortion.LensDistortionModel;
import imagingbook.calibration.distortion.Radial2TermDistortionModel;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class RadialDistortionEstimateTest {

    @Test
    public void fromTest() {
        Pnt2d[] modelPts = ZhangData.getModelPoints();
        Pnt2d[][] obsPts = ZhangData.getAllObservedPoints();
        ViewTransform[] views = ZhangData.getAllViewTransforms();   // cheating a bit, using final views from Zhang
        int M = obsPts.length;    // number of views

        // Initial camera = [877.1610736944268, 876.8009085961099, 0.17515644031677685, 301.0436734292903, 220.4104056624287, 0.0, 0.0]
        Camera cam1 = new Camera(877.16, 876.80, 0.1751, 301.04, 220.41, Radial2TermDistortionModel.INSTANCE);

        RadialDistortionEstimate estim1 = RadialDistortionEstimate.from(cam1, views, modelPts, obsPts);
        assertNotNull(estim1);
        LensDistortionModel dist1 = estim1.getDistortion();
        assertNotNull(dist1);
        System.out.println(Arrays.toString(dist1.getParameters()));
    }

    @Test
    public void getDistortion() {
    }

    @Test
    public void getError() {
    }

    @Test
    public void testGetError() {
    }
}