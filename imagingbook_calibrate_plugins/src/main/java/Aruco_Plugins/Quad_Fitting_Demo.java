/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Aruco_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.color.iterate.ColorSequencer;
import imagingbook.common.color.iterate.CssColorSequencer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.geometry.mappings.linear.Scaling2D;
import imagingbook.common.geometry.mappings.linear.Translation2D;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.core.jdoc.JavaDocHelp;
import imagingbook.jaruco.ContourSegmenter;
import imagingbook.jaruco.obsolete.QuadHomographyFit;
import imagingbook.jaruco.SegmentedPolygon;

import java.awt.Color;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.getPolygonPath;

/**
 * Shows the difference between the simple 4-point homography (red) and
 * the full contour-based homography estimate (blue).
 *
 * @author WB
 * @version 2025/12/01
 */
@Deprecated
public class Quad_Fitting_Demo implements PlugIn, JavaDocHelp {

    static {
        LogStream.redirectSystem();	// redirects System.out and System.err streams to IJ.log
    }

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";
    // static String IMG_PATH = "../aruco-images/DSC_2702_small.jpg";
    static String IMG_PATH = "../aruco-images/DSC_2705_singleC.jpg";

    static double accuracyRate = 0.03; //0.03;

    static final double[][] UNIT_SQUARE =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};


    @Override
    public void run(String args) {

        System.out.println("LogStream redirected!");

        ImagePlus im = null;
        IJ.log("user path = " + System.getProperty("user.dir"));
        File f = new File(IMG_PATH);
        URI uri = Paths.get(f.toURI()).normalize().toUri();
        IJ.log(("URI = " + uri));
        if (!f.exists()) {
            IJ.log(("Could not find image " + uri));
        }

        im = IjUtils.openImage(uri);
        im.show();

        ImageProcessor ip = im.getProcessor();
        ByteProcessor gray = ip.convertToByteProcessor();

        int thr = Math.round(new OtsuThresholder().getThreshold(gray));
        gray.threshold(thr);

        ContourTracer ct = new RegionContourSegmentation(gray);
        List<? extends Contour> contours = ct.getInnerContours();

        double ContourStrokeWidth = 0.25;
        ColorSequencer cseq = new CssColorSequencer();
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

        for (Contour contour : contours) {
            SegmentedPolygon segCont =
                    new ContourSegmenter().segment(contour.getPointList());   // tol = contour.length() * accuracyRate

            // Show original corners from segmentation:
            ColoredStroke stroke = new ColoredStroke(ContourStrokeWidth, Color.red);
            List<Pnt2d> quadCorners = segCont.getCorners();
            ola.addShape(getPolygonPath(quadCorners, 0, 0), stroke);

            //-------------------------------------------------------------------
            QuadHomographyFit fit = new QuadHomographyFit(segCont);
            double[][] A = fit.getTransformationMatrix();
            //-------------------------------------------------------------------

            ProjectiveMapping2D map = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping

            // Project the unit square to the image:
            List<Pnt2d> projectedQuad = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Pnt2d cu = map.applyTo(Pnt2d.from(UNIT_SQUARE[i]));
                // IJ.log(i + ": " + cu);
                projectedQuad.add(cu);
            }
            stroke = new ColoredStroke(ContourStrokeWidth, Color.blue);
            ola.addShape(getPolygonPath(projectedQuad, 0, 0), stroke);

            // extract the canonical marker image
            ByteProcessor markerIp1 = extractMarkerImage(gray, map);     // gray is binary
            new ImagePlus("Marker1 - from binary", markerIp1).show();

            ByteProcessor markerIp2 = extractMarkerImage(im.getProcessor(), map);
            // markerIp2.threshold(thr);    // use original threshold
            new ImagePlus("Marker2 - from original", markerIp2).show();

        }

        im.setOverlay(ola.getOverlay());
        // im.setTitle(im.getTitle() + " rot=" + res.rotation);
        im.updateAndDraw();
    }

    // ---------------------------------------------------------------------------

    static Contour toContour(List<Pnt2d> points) {
        Contour cont = new Contour(0);
        for (Pnt2d p : points) {
            cont.addPoint(p);
        }
        return cont;
    }

    static int MARKER_SIZE = 64;

    /**
     *
     * @param origIp
     * @param unitMapping target-to-source-mapping from the unit square to the marker's quad
     * @return
     */
    static ByteProcessor extractMarkerImage(ImageProcessor origIp, LinearMapping2D unitMapping) {
        ByteProcessor targetIp = new ByteProcessor(MARKER_SIZE, MARKER_SIZE);
        // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
        // 1: shift 1/2 pixel (in target space)
        // 2. scale from MARKER_SIZE to 1
        // 3. map to the source image
        LinearMapping2D map = new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / MARKER_SIZE)).concat(unitMapping);
        new ImageMapper(map).map(origIp, targetIp);
        return targetIp;
    }

    static void print(List<Pnt2d> points, String title) {
        IJ.log("Contour " + title + ":");
        int i = 0;
        for (Pnt2d p : points) {
            IJ.log("   " + p.toString()); i++;
        }

    }
}
