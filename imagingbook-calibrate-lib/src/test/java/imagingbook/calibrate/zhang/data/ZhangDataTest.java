/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.zhang.data;

import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.distortion.Radial2TermDistortionModel;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.Test;

import static imagingbook.calibrate.zhang.data.StandardModel.NumberOfModelPoints;
import static imagingbook.calibrate.zhang.data.ZhangData.NumberOfViews;
import static org.junit.Assert.*;

public class ZhangDataTest {

    @Test   //
    public void numberOfModelPointsTest() {
        assertEquals(256, NumberOfModelPoints);
    }

    @Test
    public void getModelPointsTest() {
        Pnt2d[] modelPoints = ZhangData.getModelPoints();
        assertNotNull(modelPoints);
        assertEquals(NumberOfModelPoints, modelPoints.length);
    }

    @Test
    public void getObservedPoints() {
        for (int i = 0; i < NumberOfViews; i++) {
            Pnt2d[] obsPnts = ZhangData.getObservedPoints(i);
            assertNotNull(obsPnts);
            assertEquals(NumberOfModelPoints, obsPnts.length);
        }
    }

    @Test
    public void getAllObservedPoints() {
        Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
        assertNotNull(obsPoints);
        assertEquals(NumberOfViews, obsPoints.length);
        for (int j = 0; j < obsPoints.length; j++) {
            assertEquals(NumberOfModelPoints, obsPoints[j].length); // 256 points per view
        }
    }

    @Test
    public void getAllViewTransforms() {
        ViewTransform[] views = ZhangData.getAllViewTransforms();
        assertNotNull(views);
        assertEquals(NumberOfViews, views.length);
    }

    @Test
    public void getViewTransform() {
        for (int i = 0; i < NumberOfViews; i++) {
            ViewTransform view = ZhangData.getViewTransform(i);
            assertNotNull(view);
            assertNotNull(view.getTranslationVector());
            assertNotNull(view.getRotation());
            assertNotNull(view.getRotationMatrix());
        }
    }

    @Test
    public void getCamera() {
        Camera cam = ZhangData.getCamera();
        assertNotNull(cam);

        assertNotNull(cam.getDistortion());
        assertTrue(cam.getDistortion() instanceof Radial2TermDistortionModel);

        assertNotNull(cam.getParameters());
        assertEquals(cam.getParameterCount(), cam.getParameters().length);
    }

}