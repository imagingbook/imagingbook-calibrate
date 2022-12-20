/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_1;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.color.sets.BasicAwtColor;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.core.resource.ImageResource;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.common.ij.DialogUtils.splitLines;


/**
 * This plugin projects opens an image stack containing the 5 Zhang test images, then outlines the positions of the
 * observed image points and finally projects the points of the calibration model using the calculated intrinsic camera
 * parameters (same for all views) and the extrinsic parameters calculated for each view. All data are part of Zhang's
 * demo data set that comes with the EasyCalib program. NO CALIBRATION is performed here!
 * Graphic elements are drawn as non-destructive vector overlays:
 * <ul>
 * <li>BLUE circles: observed corner points (used for calibration), </li>
 * <li>RED markers: the model reference points projected using the documented intrinsic camera and view parameters.</li>
 * </ul>
 * LOOK CLOSELY / ZOOM IN! The complete image stack with overlay can be saved as a TIFF file.
 *
 * @author WB
 * @version 2022/12/19
 */
public class Validate_EasyCalib_Data implements PlugIn {
	
	private static ImageResource resource = CalibrationImage.CalibImageStack;
	private static BasicAwtColor ProjectedModelColor = BasicAwtColor.Red;
	private static BasicAwtColor CornerMarkColor = BasicAwtColor.Blue;
	private static double CornerMarkRadius = 2.0;
	private static double StrokeWidth  = 0.5;

	public void run(String arg0) {
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		testIm.show();

		if (!runDialog()) {
			return;
		}
		
		final int M = testIm.getNSlices();
		ShapeOverlayAdapter ola = new ShapeOverlayAdapter();


		Point2D[] modelPoints = ZhangData.getModelPoints();				// get the reference model points
		Camera camReal = ZhangData.getCameraIntrinsics();				// get the (known) camera intrinsics
		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();	// get the (known) camera views
		if (viewsReal.length != M) {
			IJ.error("Wrong number of view transforms: " + viewsReal.length);
			return;
		}

		// project and draw the model's reference squares:
		ola.setStroke(new ColoredStroke(StrokeWidth, ProjectedModelColor.getColor()));
		for (int i = 0; i < M; i++) {
			int sliceNo = i + 1;
			ola.setStackPosition(sliceNo);
			Point2D[] projPnts = camReal.project(viewsReal[i], modelPoints);
			//for (Shape s : makeCrossShapes(projPnts)) {
			for (Shape s : makeQuads(projPnts)) {
				ola.addShape(s);
			}
		}

		// draw the observed image (corner) points as circles:
		ola.setStroke(new ColoredStroke(StrokeWidth, CornerMarkColor.getColor()));
		Point2D[][] obsPoints = ZhangData.getAllObservedPoints();
		for (int i = 0; i < obsPoints.length; i++) {
			int sliceNo = i + 1;
			ola.setStackPosition(sliceNo);
			for (Shape s : makeCircleShapes(obsPoints[i])) {
				ola.addShape(s);
			}
		}

		testIm.setOverlay(ola.getOverlay());
	}

	// ----------------------------------------------------------------------

	private List<Shape> makeQuads(Point2D[] pnts) {
		List<Shape> shapes = new ArrayList<>(pnts.length);
		// 4 successive points make a quad (projected rectangle)
		for (int i = 0; i < pnts.length; i += 4) {
			Path2D path = new Path2D.Double();
			path.moveTo(pnts[i].getX(), pnts[i].getY());
			for (int j = 1; j < 4; j++) {
				Point2D p = pnts[i + j];
				path.lineTo(p.getX(), p.getY());
			}
			path.closePath();
			shapes.add(path);
		}
		return shapes;
	}

	private List<Shape> makeCircleShapes(Point2D[] pnts) {	// , Color lineCol
		final double r = CornerMarkRadius;
		final double ofs = 0.5;	// pixel offset (elements to be placed at pixel centers)
		List<Shape> shapes = new ArrayList<>(pnts.length);
		for (int j = 0; j < pnts.length; j++) {
			double x = pnts[j].getX();
			double y = pnts[j].getY();
			// OvalRoi circle = new OvalRoi(x - r + ofs, y - r + ofs, 2 * r, 2 * r) ;
			Shape circle = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
			shapes.add(circle);
		}
		return shapes;
	}

	// -------------------------------------------------------------------------

	private boolean runDialog() {
		GenericDialog gd = new GenericDialog(this.getClass().getSimpleName());
		gd.setInsets(0, 0, 0);
		gd.addMessage(splitLines(40,
				"This plugin only displays the 5 sample view images, marks",
				"the (given) image corner points and projects the model's",
				"squares using the given view and camera parameters. No",
				"calibration is performed!"));

		gd.addEnumChoice("Reference squares color", ProjectedModelColor);
		gd.addEnumChoice("Corner mark color", CornerMarkColor);
		gd.addNumericField("Corner mark radius", CornerMarkRadius, 1);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		ProjectedModelColor = gd.getNextEnumChoice(BasicAwtColor.class);
		CornerMarkColor = gd.getNextEnumChoice(BasicAwtColor.class);
		CornerMarkRadius = gd.getNextNumber();
		return true;
	}
	
}
