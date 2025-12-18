/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Aruco_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.LogStream;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.RoiUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.ArucoDetector;
import imagingbook.jaruco.ArucoDictionary;
import imagingbook.jaruco.ArucoDictionaryPredefined;
import imagingbook.jaruco.math.ParabolicLineFit;
import imagingbook.jaruco.util.Polygons;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.getPolygonPath;

/**
 * First test of ArUco functionality.
 *
 * @author WB
 * @version 2025/12/01
 */
public class Parabolic_Fitting implements PlugInFilter {

    private ImagePlus im;

    @Override
    public int setup(String s, ImagePlus im) {
        this.im = im;
        return DOES_ALL + ROI_REQUIRED + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor ip) {
        Roi roi = im.getRoi();
        if (roi == null) {
            return;
        }
        Pnt2d[] pts = RoiUtils.getOutlinePointsFloat(roi);
        for (Pnt2d p : pts) {
            IJ.log(p.toString());
        }

        ParabolicLineFit fit = new ParabolicLineFit(Arrays.asList(pts));
        List<Pnt2d> plotPoints = fit.plotValuesInRealSpace(100);
        Arrays.toString(plotPoints.toArray(new Pnt2d[0]));
        IJ.log("plotPoints = \n" + Arrays.toString(plotPoints.toArray(new Pnt2d[0])));

    }


    static Pnt2d[] pointsFromRoi(Roi roi) {
        FloatPolygon origPoints = roi.getFloatPolygon();
        Pnt2d[] points = new Pnt2d[origPoints.npoints];
        for (int i = 0; i < origPoints.npoints; i++) {
            float x = origPoints.xpoints[i];	// IJ displays points with a 0.5 pixel offset!
            float y = origPoints.ypoints[i];
            points[i] = Pnt2d.from(x, y);
        }

        return points;
    }

}
