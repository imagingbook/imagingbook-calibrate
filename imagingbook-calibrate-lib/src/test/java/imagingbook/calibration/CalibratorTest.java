/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.core.resource.ImageResource;

import org.junit.Test;

import static org.junit.Assert.*;

public class CalibratorTest {

    static ImageResource resource = CalibrationImage.CalibImageStack;



    @Test
    public void calibrateTestZhangCam() {
        Pnt2d[] modelPoints = ZhangData.getModelPoints();
        Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
        int M = obsPoints.length;    // number of views

        // Set up the calibrator ------------------------------------------

        Calibrator.Parameters params = new Calibrator.Parameters();
        params.normalizePointSets = true;
        params.useNumericJacobian = true;
        params.debug = false;

        Calibrator calibrator = new Calibrator(params, modelPoints);
        assertNotNull(calibrator);
        for (int i = 0; i < M; i++) {
            calibrator.addView(obsPoints[i]);
        }

        // Perform calibration ------------------------------------------

        Camera finCam = calibrator.calibrate();
        assertNotNull(finCam);
        Camera refCam = ZhangData.getCamera();  // reference camera
        assertNotNull(refCam);

        double[] pf = finCam.getParameterVector();
        double[] pr = refCam.getParameterVector();
        assertArrayEquals(pr, pf, 1e-3);

        ViewTransform[] finViews = calibrator.getFinalViews();
        ViewTransform[] refViews = ZhangData.getAllViewTransforms();
        assertEquals(refViews.length, finViews.length);
        for (int i = 0; i < refViews.length; i++) {
            assertArrayEquals(refViews[i].getParameters(), finViews[i].getParameters(), 1e-3);
        }
    }

    @Test
    public void getInitialCamera() {
    }

    @Test
    public void getFinalCamera() {
    }

    @Test
    public void getInitialViews() {
    }

    @Test
    public void getFinalViews() {
    }
}