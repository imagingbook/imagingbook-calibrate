/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.plumbline;

import ij.ImagePlus;
import ij.gui.Overlay;
import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.color.iterate.CssColorSequencer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PolyLine2d;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;

import java.util.ArrayList;
import java.util.List;

public final class CollinearPointsGenerator {

    private final Camera camera;

    public CollinearPointsGenerator(Camera camera) {
        this.camera = camera;
    }

    /**
     * Creates sets of "collinear" points by sampling straight lines in the ideal projection plane
     * and applying lens distortion as specified by the supplied camera.
     * @param nHor number of horizontal lines
     * @param nVer number of vertical lines
     * @param roundToInt set true to round point coordinates to integers
     * @return
     */
    public List<List<Pnt2d>> makeCollinearPoints(int nHor, int nVer, boolean roundToInt) {
        double W = camera.getUc() * 2;    // width and height of image, assuming uc/vc is at center
        double H = camera.getVc() * 2;
        int N = 10; // sample points per line

        AffineMapping2D sensorToNormalizedMapping = new AffineMapping2D(camera.getAffineMatrixInverse().getData());
        AffineMapping2D normalizedToSensorMapping = new AffineMapping2D(camera.getAffineMatrix().getData());

        DistortionModel dist = camera.getDistortion();
        List<List<Pnt2d>> lines = new ArrayList<>();

        // horizontal lines (step vertical)
        for (int j = 0; j < nHor && j < N; j++) {
            List<Pnt2d> pntSet = new ArrayList<>();
            double v = (0.5 + j) * H / N;
            for (int i = 0; i < N; i++) {
                double u = (0.5 + i) * W / N;                                       // sensor coordinate (u,v)
                Pnt2d xy = sensorToNormalizedMapping.applyTo(Pnt2d.from(u, v));     // normalized proj. (x, y)
                Pnt2d xyw = dist.warp(xy);                                          // warped normalized proj. (xw, yw)
                Pnt2d uvw = normalizedToSensorMapping.applyTo(xyw);                 // warped image coord.
                pntSet.add(roundToInt ? round(uvw) : uvw);
            }
            lines.add(pntSet);
        }

        // vertical lines (step horizontal):
        for (int i = 0; i < nVer && i < N; i++) {
            List<Pnt2d> pntSet = new ArrayList<>();
            double u = (0.5 + i) * W / N;
            for (int j = 0; j < N; j++) {
                double v = (0.5 + j) * H / N;                                       // sensor coordinate (u,v)
                Pnt2d xy = sensorToNormalizedMapping.applyTo(Pnt2d.from(u, v));     // normalized proj. (x, y)
                Pnt2d xyw = dist.warp(xy);                                          // warped normalized proj. (xw, yw)
                Pnt2d uvw = normalizedToSensorMapping.applyTo(xyw);                 // warped image coord.
                pntSet.add(roundToInt ? round(uvw) : uvw);
            }
            lines.add(pntSet);
        }
        return lines;
    }

    private static Pnt2d round(Pnt2d p) {
        int u = (int) Math.round(p.getX());
        int v = (int) Math.round(p.getY());
        return Pnt2d.from(u, v);
    }

    /**
     * Creates and returns an ImageJ {@link Overlay} to be attached to and displayed on top of a
     * {@link ImagePlus} instance.
     * @param pointSets a list of collinear sets of 2D points
     * @return an ImageJ {@link Overlay} instance for the supplied point sets
     */
    public static Overlay makeOverlay(List<List<Pnt2d>> pointSets, double pntRadius) {
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        CssColorSequencer colSeq = new CssColorSequencer();
        for (List<Pnt2d> pointSet : pointSets) {
            ola.setStroke(new ColoredStroke(1.0, colSeq.next()));
            PolyLine2d poly = new PolyLine2d(pointSet);
            ola.addShape(poly.getShape());
            for (Pnt2d point : pointSet) {
                ola.addShape(point.getShape(pntRadius));
            }
        }
        return ola.getOverlay();
    }
}
