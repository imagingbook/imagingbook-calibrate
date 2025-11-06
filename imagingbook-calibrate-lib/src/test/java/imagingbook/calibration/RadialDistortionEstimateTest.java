/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.Test;

import static org.junit.Assert.*;

public class RadialDistortionEstimateTest {

    @Test
    public void fromTest() {
        Pnt2d[] modelPoints = ZhangData.getModelPoints();
        Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
        int M = obsPoints.length;    // number of views

        // RadialDistortionEstimate from(Camera cam, ViewTransform[] views, Pnt2d[] modelPts, Pnt2d[][] obsPts) {
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