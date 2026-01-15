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
import imagingbook.common.math.PrintPrecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PtLensDistortionModelTest {

    @Test
    public void fromParametersTest() {
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

    @Test
    public void estimateParametersTest() {
        double[] abc = {0.01144, -0.0102, 0.01051};     // distortion parameters

        PtLensDistortionModel realDist = new PtLensDistortionModel(640, 480).fromParameters(abc);
        ViewTransform view = new ViewTransform(Rotation.IDENTITY, new double[]{0, 0, 1});
        Camera realCam = new Camera(1, 1, 0, 0, 0, realDist);

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
        Camera initCam = new Camera(1, 1, 0, 0, 0, null);
        DistortionModel dm = new PtLensDistortionModel(640, 480);
        DistortionEstimator estimtr = new DistortionEstimator(initCam, dm);
        Camera camImproved = estimtr.getEstimate(List.of(view), modPntList, imgPntList);

        // PrintPrecision.set(8);
        // System.out.println("distortion = " + camImproved.getDistortion());
        assertArrayEquals(abc, camImproved.getDistortion().getParameters(), 1e-6);
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