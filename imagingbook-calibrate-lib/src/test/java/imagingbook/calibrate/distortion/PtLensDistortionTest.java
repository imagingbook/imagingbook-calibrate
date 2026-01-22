/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PtLensDistortionTest {

    static final double tol = 1e-6;


    @Test
    public void constructorTest1() {
        double[] abc1= {0.01144, -0.0102, 0.01051};
        double scale = 2.7;

        // see if coefficients and scale gets set up right
        PtLensDistortion dist1 = new PtLensDistortion(abc1, scale);
        assertArrayEquals(abc1, dist1.getParameters(), tol);
        assertEquals(abc1.length, dist1.getParameterCount());
        assertEquals(scale, dist1.getScale(), tol);

        // see if new coefficients are accepted and existing scale is copied
        double[] abc2= {-0.2, 0.01, 0.0};
        PtLensDistortion dist2 = dist1.withParameters(abc2);
        assertArrayEquals(abc2, dist2.getParameters(), tol);
        assertEquals(abc2.length, dist2.getParameterCount());
        assertEquals(scale, dist2.getScale(), tol);
    }

    @Test
    public void fRadTestZero() {
        double[] abc = {0.01144, -0.0102, 0.01051};
        double scale = 3.1;
        PtLensDistortion dm = new PtLensDistortion(abc, scale);
        double r0 = dm.fRad(0.0);
        assertEquals(0.0, r0, tol);
    }

    @Test
    public void fRadTest() {
        double[] abc = {0.01144, -0.0102, 0.01051};
        double scale = 3.1;
        PtLensDistortion dm = new PtLensDistortion(abc, scale);
        double r1 = 0.35;
        double r2 = dm.fRad(r1);
        assertEquals(0.35079024490, r2, tol);
        double r3 = dm.fRadInv(r2);
        assertEquals(r1, r3, tol);
    }

    @Test
    public void fRadInvTest() {
        double[] abc = {0.01144, -0.0102, 0.01051};
        double scale = 3.1;
        PtLensDistortion dm = new PtLensDistortion(abc, scale);
        int n = 20;
        double range = 2.0;
        for (int i = 0; i < n; i++) {
            double r1 = i * range / n;
            double r2 = dm.fRad(r1);
            double r3 = dm.fRadInv(r2);
            assertEquals(r1, r3, tol);
        }
    }


    @Test   // checks if points on r = 1/scale circle are fixed points.
    public void fRadFixedPointRadiusTest() {
        double[] abc = {0.01144, -0.0102, 0.01051}; // not relevant
        double scale = 2.7;
        PtLensDistortion dist = new PtLensDistortion(abc, scale);
        int n = 100;
        double r = 1 / scale;
        for (int i = 0; i < n; i++) {
            double phi = i * 2 * Math.PI / n;
            double[] xy = { r * Math.cos(phi), r * Math.sin(phi) };
            // System.out.println("\nxy = " + Pnt2d.from(xy));
            double[] xyd = dist.warp(xy);
            assertArrayEquals(xy, xyd, tol);
            // System.out.println("xyd = " + Pnt2d.from(xyd));
        }
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

    @Test   // check distortion parameter estimation (without noise)
    public void estimateParametersTest() {
        double[] abc = {0.011, -0.014, 0.017};     // assumed PtLens distortion parameters
        int W = 640, H = 480;
        double scale = 3.0;     // arbitrary scale
        double alpha = 700;     // focal length in pixels
        double beta = 710;
        double uc = 0.5 * W;
        double vc = 0.5 * H;

        PtLensDistortion nullDist = new PtLensDistortion(null, scale);
        // non-distorting camera (just for comparison):
        Camera nullCam = new Camera(new double[] { alpha, beta, 0, uc, vc }, nullDist);

        // set up the actual camera:
        ViewTransform view = new ViewTransform();           // identity view
        PtLensDistortion realDist = new PtLensDistortion(abc, scale);
        Camera realCam = new Camera(new double[] { alpha, beta, 0, uc, vc }, realDist);

        Pnt2d[] modelPoints = makeModelPoints(20);
        List<Pnt2d[]> modPntList = Collections.singletonList(modelPoints);
        List<Pnt2d[]> imgPntList = new ArrayList<>();

        // create image point by projecting model points through realCam:
        Pnt2d[] imgPnts = new Pnt2d[modelPoints.length];
        for (int i = 0; i < modelPoints.length; i++) {
            Pnt2d XY = modelPoints[i];
            // Pnt2d xy = Pnt2d.from(realCam.projectNormalized(view, XY));
            // Pnt2d uvN = Pnt2d.from(nullCam.project(view, XY));  // no distortion
            Pnt2d uv = Pnt2d.from(realCam.project(view, XY));   // with distortion
            // System.out.printf("XY=%s    uvN=%s   uv=%s\n", XY, uvN, uv);
            imgPnts[i] = uv;
        }
        imgPntList.add(imgPnts);    // projected image points

        // start parameter estimation:
        DistortionEstimator estimtr = new DistortionEstimator(nullCam);
        Camera camImproved = estimtr.getEstimate(List.of(view), modPntList, imgPntList);

        // PrintPrecision.set(8);
        System.out.println("distortion = " + camImproved.getDistortion());
        assertArrayEquals(abc, camImproved.getDistortion().getParameters(), 1e-6);
    }

    static Pnt2d[] makeModelPoints(int n) {
        // int n = 20;
        List<Pnt2d> points = new ArrayList<Pnt2d>();
        for (int i = -n; i <= n; i++) {
            double r = i * 0.5 / n;
            // double xy = Math.sqrt(0.5 * r);
            points.add(Pnt2d.from(r, 0));
            points.add(Pnt2d.from(0, r));
            // points.add(Pnt2d.from(-r, 0));
            // points.add(Pnt2d.from(0, r));
            // points.add(Pnt2d.from(0, -r));
            // points.add(Pnt2d.from(xy, xy));
            // points.add(Pnt2d.from(-xy, xy));
            // points.add(Pnt2d.from(xy, -xy));
            // points.add(Pnt2d.from(-xy, -xy));
        }
        return points.toArray(new Pnt2d[0]);
    }

}