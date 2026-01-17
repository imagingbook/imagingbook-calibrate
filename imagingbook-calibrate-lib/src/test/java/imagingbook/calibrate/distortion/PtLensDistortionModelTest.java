/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.math3legacy.Rotation;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PtLensDistortionModelTest {

    @Test
    public void constructorTest1() {
        double[] abc = {0.01144, -0.0102, 0.01051};
        double scale = 2.7;
        PtLensDistortionModel dist1 = new PtLensDistortionModel(abc, scale);
        assertArrayEquals(abc, dist1.getParameters(), 1e-6);
        assertEquals(abc.length, dist1.getParameterCount());
        assertEquals(scale, dist1.getScale(), 1e-6);

        PtLensDistortionModel dist2 = dist1.fromParameters(dist1.getParameters());
        assertArrayEquals(abc, dist2.getParameters(), 1e-6);
        assertEquals(abc.length, dist2.getParameterCount());
        assertEquals(scale, dist2.getScale(), 1e-6);
    }

    @Test
    public void fRadTest() {
    }

    @Test
    public void fRadInvTest() {
    }

    @Test
    public void getDMatrixRowsUVTest() {
    }


    @Test   // checks if circle with r=1/s in normalized space fits exactly into image
    public void scaleCalculationTest() {
        double[] abc = {0.01144, -0.0102, 0.01051};     // distortion parameters (not relevant)
        int W = 640;
        int H = 480;
        double alpha = 700;
        double beta = alpha;

        Camera cam = new Camera(new double[] { alpha, beta, 0, 0.5 * W, 0.5 * H }, null);
        cam.setDistortion(DistortionModelType.PtLens.create(cam, W, H));

        double scale = cam.getDistortion().getScale();       // = 2.916666
        assertEquals(beta / (0.5 * H), scale, 1e-6);

        {   // point exactly at bottom of image
            Pnt2d xy = Pnt2d.from(0, 1/scale);        // normalized point with r = 1/scale
            double[] xyd = cam.getDistortion().warp(xy.toDoubleArray());   // warped point (same)
            assertArrayEquals(xy.toDoubleArray(), xyd, 1e-6);
            // should project to (W/2, H)
            double[] uv = cam.mapToSensorPlane(xyd);
            assertArrayEquals(new double[] {0.5 * W, H}, uv, 1e-6); // {W/2, H}
        }
        {   // point exactly at top of image
            Pnt2d xy = Pnt2d.from(0, -1/scale);         // normalized point with r = 1/scale
            double[] xyd = cam.getDistortion().warp(xy.toDoubleArray());   // warped point (same)
            assertArrayEquals(xy.toDoubleArray(), xyd, 1e-6);
            // should project to (W/2, 0)
            double[] uv = cam.mapToSensorPlane(xyd);
            assertArrayEquals(new double[] {0.5 * W, 0}, uv, 1e-6); // {W/2, 0}
        }
    }

    @Test       // OBSOLETE!
    public void estimateParametersTest() {
        double[] abc = {0.01144, -0.0102, 0.01051};     // distortion parameters
        int W = 640;
        int H = 480;
        double s = 0.7;
        double alpha = (1/s) * H / 2;
        double beta = alpha;

        PtLensDistortionModel realDist = new PtLensDistortionModel(abc, s);
        ViewTransform view = new ViewTransform(Rotation.IDENTITY, new double[]{0, 0, 1});
        Camera realCam = new Camera(new double[] { alpha, beta, 0, 0, 0 }, realDist);

        Pnt2d[] modelPoints = makeModelPoints();
        List<Pnt2d[]> modPntList = Collections.singletonList(modelPoints);
        List<Pnt2d[]> imgPntList = new ArrayList<>();

        // System.out.println("realCam = " + realCam);
        // System.out.println("view = " + view);
        // System.out.println("realCam = " + realCam);
        // System.out.println("N = " + modelPoints.length);

        Pnt2d[] imgPnts = new Pnt2d[modelPoints.length];
        for (int i = 0; i < modelPoints.length; i++) {
            Pnt2d XY = modelPoints[i];
            Pnt2d xy = Pnt2d.from(realCam.projectNormalized(view, XY));
            Pnt2d uv = Pnt2d.from(realCam.project(view, XY));
            // System.out.printf("%s -> %s -> %s\n", XY, xy, uv);
            imgPnts[i] = uv;
        }
        imgPntList.add(imgPnts);

        // start parameter estimation:
        Camera initCam = new Camera(new double[] { alpha, beta, 0, 0, 0}, null);
        // initCam.setDistortion(DistortionModelType.PtLens.create(initCam, W, H));
        DistortionModel dm = DistortionModelType.PtLens.create(initCam, W, H);

        System.out.println("dm.scale = " + dm.getScale());
        DistortionEstimator estimtr = new DistortionEstimator(initCam, dm);
        Camera camImproved = estimtr.getEstimate(List.of(view), modPntList, imgPntList);

        PrintPrecision.set(8);
        System.out.println("distortion = " + camImproved.getDistortion());
        // assertArrayEquals(abc, camImproved.getDistortion().getParameters(), 1e-6);
    }

    static Pnt2d[] makeModelPoints() {
        int n = 20;
        List<Pnt2d> points = new ArrayList<Pnt2d>();
        for (int i = 0; i <= n; i++) {
            double r = i * 1.0 / n;
            double xy = Math.sqrt(0.5 * r);
            points.add(Pnt2d.from(r, 0));
            points.add(Pnt2d.from(-r, 0));
            points.add(Pnt2d.from(0, r));
            points.add(Pnt2d.from(0, -r));
            points.add(Pnt2d.from(xy, xy));
            points.add(Pnt2d.from(-xy, xy));
            points.add(Pnt2d.from(xy, -xy));
            points.add(Pnt2d.from(-xy, -xy));
        }
        return points.toArray(new Pnt2d[0]);
    }

}