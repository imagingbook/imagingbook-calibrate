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
import imagingbook.jaruco.ContourSimplifier;
import imagingbook.jaruco.Polygons;
import imagingbook.common.color.iterate.ColorSequencer;
import imagingbook.common.color.iterate.CssColorSequencer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.core.jdoc.JavaDocHelp;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.jaruco.Polygons.getCircularity;
import static imagingbook.jaruco.Polygons.isConvex;

/**
 * First test of ArUco functionality.
 *
 * @author WB
 * @version 2025/12/01
 */
public class Aruco_Marker_Check_Plugin implements PlugIn, JavaDocHelp {

    static {
        LogStream.redirectSystem();	// redirects System.out and System.err streams to IJ.log
    }

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";
    // static String IMG_PATH = "../aruco-images/DSC_2702_small.jpg";
    static String IMG_PATH = "../aruco-images/DSC_2705_singleC.jpg";

    static double accuracyRate = 0.03; //0.03;

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
        // im.show();

        ImageProcessor ip = im.getProcessor();
        ByteProcessor gray = ip.convertToByteProcessor();

        new OtsuThresholder().threshold(gray);
        ImagePlus ig = new ImagePlus(im.getShortTitle() + "-gray", gray);

        ContourTracer ct = new RegionContourSegmentation(gray);
        List<? extends Contour> ocs = ct.getOuterContours();
        List<? extends Contour> ics = ct.getInnerContours();

        IJ.log("outer contours: " + ocs.size());
        IJ.log("inner contours: " + ics.size());

        // ocs.removeIf(ctr -> ctr.getLength() > 50);
        // ics.removeIf(ctr -> ctr.getLength() > 50);

        List<? extends Contour> ocsCln =
                ocs.stream().filter(ctr -> ctr.getLength() > 50).toList();
        List<? extends Contour> icsCln =
                ics.stream().filter(ctr -> ctr.getLength() > 50).toList();

        IJ.log("outer contours cleaned: " + ocsCln.size());
        IJ.log("inner contours cleaned: " + icsCln.size());


        double ContourStrokeWidth = 0.25;
        // ColoredStroke outerStroke = new ColoredStroke(ContourStrokeWidth, Color.red);
        // ColoredStroke innerStroke = new ColoredStroke(ContourStrokeWidth, Color.blue);
        ColorSequencer cseq = new CssColorSequencer();

        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        // for (Contour oc : ocsCln) {
        //     ColoredStroke stroke = new ColoredStroke(ContourStrokeWidth, cseq.next());
        //     ola.addShape(oc.getPolygonPath(), stroke);
        // }
        //
        // for (Contour ic : icsCln) {
        //     ColoredStroke stroke = new ColoredStroke(ContourStrokeWidth, cseq.next());
        //     ola.addShape(ic.getPolygonPath(), stroke);
        // }

        // make polygon approximations -------------

        List<List<Pnt2d>> ocsSmpl = new ArrayList<>();
        List<List<Pnt2d>> icsSmpl = new ArrayList<>();

        // only keep outer contours with exactly 4 vertices:
        for (Contour oc : ocsCln) {
            List<Pnt2d> os = ContourSimplifier.simplify(oc, oc.getLength() * accuracyRate, true);
            if (os.size() >= 4) {
                ocsSmpl.add(os);
                // print(os, "outer");
            }
        }
        // only keep inner contours with exactly 4 vertices (5 because closed):
        int k = 0;
        for (Contour ic : icsCln) {
            double tol = ic.getLength() * accuracyRate;
            IJ.log("tolerance = " + (ic.getLength() * accuracyRate));
            // List<Pnt2d> is = ContourSimplifier.simplify(ic, tol, true);
            List<Pnt2d> is = Polygons.simplify(ic.getPointList(), tol);
            IJ.log("is: size = " + is.size());

            List<Pnt2d> iscln = is;
            // iscln = ContourSimplifier.cleanupCollinear(is, tol, true);   // not needed

            IJ.log("iscln: size = " + iscln.size());
            if (iscln.size() == 4 && isConvex(iscln) != 0 && getCircularity(iscln) > 0.5) {
                // print(ic.getPointList(), "inner orig" + k);
                icsSmpl.add(iscln);
                print(iscln, "inner simple" + k);
                k++;
            }
        }

        // show simplified outer contours
        // for (List<Pnt2d> oc : ocsSmpl) {
        //     ColoredStroke stroke = new ColoredStroke(ContourStrokeWidth, cseq.next());
        //     ola.addShape(toContour(oc).getPolygonPath(), stroke);
        // }

        // show simplified inner contours
        for (List<Pnt2d> ic : icsSmpl) {
            Color col = cseq.next();
            ColoredStroke stroke = new ColoredStroke(ContourStrokeWidth, col);
            ColoredStroke stroke0 = new ColoredStroke(ContourStrokeWidth * 3, col);
            ola.addShape(toContour(ic).getPolygonPath(), stroke);
            double r = 2;
            int j = 0;
            for (Pnt2d p : ic) {
                double x = p.getX() - r;
                double y = p.getY() - r;
                ola.addShape(new Ellipse2D.Double(x, y, 2*r, 2*r), j == 0 ? stroke0 : stroke);
                j++;
            }
        }

        IJ.log("outer contours simplified: " + ocsSmpl.size());
        IJ.log("inner contours simplified: " + icsSmpl.size());

        // show thresholded image
        // ig.setOverlay(ola.getOverlay());
        // ig.show();

        // show oiginal grayscale image
        im.setOverlay(ola.getOverlay());
        im.show();

        // CORNER REFINEMENT should come here!

        // extract a 32 x 32 subimage
        List<Pnt2d> box1 = icsSmpl.get(0);
        ByteProcessor markerIp = extractMarkerImage(im.getProcessor(), box1);
        new OtsuThresholder().threshold(markerIp);
        new ImagePlus("Marker1", markerIp).show();


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
    static ByteProcessor extractMarkerImage(ImageProcessor origIp, List<Pnt2d> corners) {
        Pnt2d[] sourcePts = corners.toArray(new Pnt2d[0]);
        Pnt2d[] targetPts = {
                Pnt2d.from(0, 0),
                Pnt2d.from(0, MARKER_SIZE-1),
                Pnt2d.from(MARKER_SIZE-1, MARKER_SIZE-1),
                Pnt2d.from(MARKER_SIZE-1, 0)};
        ProjectiveMapping2D map = ProjectiveMapping2D.fromPoints(targetPts, sourcePts);
        ByteProcessor targetIp = new ByteProcessor(MARKER_SIZE, MARKER_SIZE);
        IJ.log("map = " + map.toString());
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
