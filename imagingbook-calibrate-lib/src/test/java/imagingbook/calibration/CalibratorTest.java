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
import java.util.Arrays;
import static org.junit.Assert.*;

public class CalibratorTest {

    static final Pnt2d[] modelPoints = ZhangData.getModelPoints();
    static final Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
    static final ViewTransform[] refViews = ZhangData.getAllViewTransforms();

    @Test
    public void calibrateZhangCamTest() {

        int M = obsPoints.length;    // number of views

        // Set up the calibrator ------------------------------------------

        Calibrator.Parameters params = new Calibrator.Parameters();
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = false;

        Calibrator calibrator = new Calibrator(params, modelPoints);
        assertNotNull(calibrator);
        calibrator.addViews(obsPoints);

        // Perform calibration ------------------------------------------
        Camera finCam = calibrator.calibrate();
        assertNotNull(finCam);
        Camera refCam = ZhangData.getCamera();  // reference camera
        assertNotNull(refCam);
        // System.out.println("Initial camera = " + Arrays.toString(calibrator.getInitialCamera().getParameterVector()));
        // Initial camera = [877.1610736944268, 876.8009085961099, 0.17515644031677685, 301.0436734292903, 220.4104056624287, 0.0, 0.0]

        double[] pf = finCam.getParameterVector();
        double[] pr = refCam.getParameterVector();
        assertArrayEquals(pr, pf, 1e-3);

        ViewTransform[] finViews = calibrator.getFinalViews();
        assertEquals(M, finViews.length);
        assertEquals(M, refViews.length);
        for (int i = 0; i < M; i++) {
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