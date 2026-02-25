/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.hugin;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;

import java.util.ArrayList;
import java.util.List;

public final class Utils {
    private Utils() {}

    /**
     * Sample straight lines in the ideal projection plane.
     * @param cam the camera
     * @param nHor number of horizontal lines
     * @param nVer number of vertical lines
     * @param roundToInt set true to round point coordinates to integers
     * @return
     */
    public static List<List<Pnt2d>> makeCollinearPoints(Camera cam, int nHor, int nVer, boolean roundToInt) {
        double w = cam.getUc() * 2;    // width and height of image, assuming uc/vc is at center
        double h = cam.getVc() * 2;
        int N = 10; // sample points per line

        AffineMapping2D sensorToNormalizedMapping = new AffineMapping2D(cam.getAffineMatrixInverse().getData());
        AffineMapping2D normalizedToSensorMapping = new AffineMapping2D(cam.getAffineMatrix().getData());

        DistortionModel dist = cam.getDistortion();
        List<List<Pnt2d>> lines = new ArrayList<>();

        // horizontal lines (step vertical)
        for (int j = 0; j < nHor; j++) {
            List<Pnt2d> pntSet = new ArrayList<>();
            double v = j * h / nHor;
            for (int i = 0; i <= N; i++) {
                double u = i * w / N;                                               // sensor coordinate (u,v)
                Pnt2d xy = sensorToNormalizedMapping.applyTo(Pnt2d.from(u, v));     // normalized proj. (x, y)
                Pnt2d xyw = dist.warp(xy);                                          // warped normalized proj. (xw, yw)
                Pnt2d uv = normalizedToSensorMapping.applyTo(xyw);                  // warped image coord.
                pntSet.add(roundToInt ? round(uv) : uv);
            }
            lines.add(pntSet);
        }

        // vertical lines (step horizontal):
        for (int i = 0; i < nVer; i++) {
            List<Pnt2d> pntSet = new ArrayList<>();
            double u = i * w / nVer;
            for (int j = 0; j <= N; j++) {
                double v = j * h / N;                                               // sensor coordinate (u,v)
                Pnt2d xy = sensorToNormalizedMapping.applyTo(Pnt2d.from(u, v));     // normalized proj. (x, y)
                Pnt2d xyw = dist.warp(xy);                                          // warped normalized proj. (xw, yw)
                Pnt2d uv = normalizedToSensorMapping.applyTo(xyw);                  // warped image coord.
                pntSet.add(roundToInt ? round(uv) : uv);
            }
            lines.add(pntSet);
        }
        return lines;
    }

    static Pnt2d round(Pnt2d p) {
        int u = (int) Math.round(p.getX());
        int v = (int) Math.round(p.getY());
        return Pnt2d.from(u, v);
    }

}
