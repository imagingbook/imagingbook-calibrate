/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate;

import imagingbook.calibrate.distortion.DistortionModelType;
import imagingbook.calibrate.distortion.Radial2TermDistortionModel;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import org.junit.Test;

import static org.junit.Assert.*;

public class CalibrationTest {

    static final Pnt2d[] modelPoints = ZhangData.getModelPoints();
    static final Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
    static final ViewTransform[] refViews = ZhangData.getAllViewTransforms();
    static final int M = obsPoints.length;    // number of views

    @Test
    public void calibrateZhangCam2Test() {  // using original 2-term distortion model
        // Set up the calibration ------------------------------------------
        Calibration.Parameters params = new Calibration.Parameters();
        params.distModelType = DistortionModelType.Radial2Term;
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = false;

        Calibration calibration = new Calibration(params, 640, 480);
        assertNotNull(calibration);
        for (int k = 0; k < M; k++) {
            calibration.addView(modelPoints, obsPoints[k]);
        }

        // Perform calibration ------------------------------------------
        calibration.calibrate();
        Camera finCam = calibration.getFinalCamera();
        assertNotNull(finCam);
        System.out.println("finCam = " + Matrix.toString(finCam.getParameterVector()));
        // Initial camera = [877.1610736944268, 876.8009085961099, 0.17515644031677685, 301.0436734292903, 220.4104056624287, 0.0, 0.0]

        Camera refCam = ZhangData.getCamera();  // reference camera
        System.out.println("refCam = " + Matrix.toString(refCam.getParameterVector()));
        assertNotNull(refCam);

        double[] pf = finCam.getParameterVector();
        double[] pr = refCam.getParameterVector();
        assertArrayEquals(pr, pf, 1e-3);

        assertEquals(M, refViews.length);
        for (int k = 0; k < M; k++) {
            ViewTransform finView = calibration.getFinalViewTransform(k);
            assertArrayEquals(refViews[k].getParameters(), finView.getParameters(), 1e-3);
        }
    }

    // @Test
    // public void calibrateZhangCam3Test() {  // using 3-term distortion model
    //     // Set up the calibration ------------------------------------------
    //     Calibration.Parameters params = new Calibration.Parameters();
    //     params.distortionModel = Radial3TermDistortionModel.INSTANCE;
    //     params.normalizePoints = true;
    //     params.useNumericJacobian = true;
    //     params.debug = false;
    //
    //     Calibration calibration = new Calibration(params, modelPoints, -1, -1);
    //     assertNotNull(calibration);
    //     calibration.addViews(obsPoints);
    //
    //     // Perform calibration ------------------------------------------
    //     Camera finCam = calibration.calibrate();
    //     assertNotNull(finCam);
    //     Camera refCam = ZhangData.getCamera();  // reference camera
    //     assertNotNull(refCam);
    //     // System.out.println("Initial camera = " + Arrays.toString(calibration.getInitialCamera().getParameterVector()));
    //     // Initial camera = [877.1610736944268, 876.8009085961099, 0.17515644031677685, 301.0436734292903, 220.4104056624287, 0.0, 0.0]
    //
    //     // double[] pf = finCam.getParameterVector();
    //     // double[] pr = refCam.getParameterVector();
    //     // assertArrayEquals(pr, pf, 1e-3);
    //
    //     ViewTransform[] finViews = calibration.getFinalViews();
    //     assertEquals(M, finViews.length);
    //     assertEquals(M, refViews.length);
    //     // for (int i = 0; i < M; i++) {
    //     //     assertArrayEquals(refViews[i].getParameters(), finViews[i].getParameters(), 1e-3);
    //     // }
    // }

    // @Test
    // public void calibrateRadialLateralTest() {  // using radial+lateral distortion model
    //     // Set up the calibration ------------------------------------------
    //     Calibration.Parameters params = new Calibration.Parameters();
    //     params.distortionModel = RadialLateralDistortionModel.INSTANCE;
    //     params.normalizePoints = true;
    //     params.useNumericJacobian = true;
    //     params.debug = false;
    //
    //     Calibration calibration = new Calibration(params, modelPoints, -1, -1);
    //     assertNotNull(calibration);
    //     calibration.addViews(obsPoints);
    //
    //     // Perform calibration ------------------------------------------
    //     Camera finCam = calibration.calibrate();
    //     assertNotNull(finCam);
    //     Camera refCam = ZhangData.getCamera();  // reference camera
    //     assertNotNull(refCam);
    //     // System.out.println("Initial camera = " + Arrays.toString(calibration.getInitialCamera().getParameterVector()));
    //     // Initial camera = [877.1610736944268, 876.8009085961099, 0.17515644031677685, 301.0436734292903, 220.4104056624287, 0.0, 0.0]
    //
    //     double[] pf = finCam.getParameterVector();
    //     double[] pr = refCam.getParameterVector();
    //     // assertArrayEquals(pr, pf, 1e-3);
    //
    //     ViewTransform[] finViews = calibration.getFinalViews();
    //     assertEquals(M, finViews.length);
    //     assertEquals(M, refViews.length);
    //     // for (int i = 0; i < M; i++) {
    //     //     assertArrayEquals(refViews[i].getParameters(), finViews[i].getParameters(), 1e-3);
    //     // }
    // }


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