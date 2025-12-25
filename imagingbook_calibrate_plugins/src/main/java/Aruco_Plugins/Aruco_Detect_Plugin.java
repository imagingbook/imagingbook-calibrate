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
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.ArucoDetector;
import imagingbook.jaruco.ArucoDictionary;
import imagingbook.jaruco.ArucoDictionaryPredefined;
import imagingbook.jaruco.util.Polygons;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.getPolygonPath;

/**
 * First test of ArUco functionality.
 *
 * @author WB
 * @version 2025/12/01
 */
public class Aruco_Detect_Plugin implements PlugInFilter {
    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    private static final Font MarkerFont = new Font(Font.SANS_SERIF, Font.BOLD, 32);
    private static final Font CornerFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Color MarkerColor = Color.magenta;
    private static final Color CornerColor = Color.blue;

    static {
        LogStream.redirectSystem();    // redirects System.out and System.err streams to IJ.log
    }

    private ImagePlus im;

    @Override
    public int setup(String s, ImagePlus im) {
        this.im = im;
        return DOES_8G + DOES_RGB;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();
        ArucoDetector detector = new ArucoDetector(dict);

        ColoredStroke stroke = new ColoredStroke(1.0, Color.blue);
        ColoredStroke stroke0 = new ColoredStroke(1.0 * 3, Color.red);

        List<ArucoDetector.DetectionResult> markerDetectionResults =
                detector.detectMarkers(im.getProcessor());
        // System.out.println("Markers found: " + markerDetectionResultObsoletes.size());
        if (markerDetectionResults.isEmpty()) {
            IJ.log("No markers found!");
            return;
        }

        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

        for (ArucoDetector.DetectionResult res : markerDetectionResults) {
            Polygon2d corners = res.corners();
            ola.addShape(corners.getShape(), stroke);

            ola.setFont(CornerFont);
            ola.setTextColor(CornerColor);
            double rad = 2;
            int j = 0;
            for (Pnt2d p : corners) {
                double x = p.getX() - rad;
                double y = p.getY() - rad;
                ola.addShape(new Ellipse2D.Double(x, y, 2 * rad, 2 * rad), j == 0 ? stroke0 : stroke);
                ola.addText(x + 5, y + 5, "" + j);
                j++;
            }

            // draw the marker's id number
            Pnt2d center = corners.getCentroid();
            ola.setFont(MarkerFont);
            ola.setTextColor(MarkerColor);
            ola.addText(center.getX(), center.getY(), Integer.toString(res.markerId()) + "/" + res.rotation());
        }

        im.setOverlay(ola.getOverlay());
        // im.setTitle(im.getTitle() + " rot=" + res.rotation);
        im.updateAndDraw();
    }
}
