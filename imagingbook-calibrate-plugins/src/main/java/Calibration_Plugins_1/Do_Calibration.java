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
import imagingbook.calibration.zhang.Calibrator;
import imagingbook.calibration.zhang.Calibrator.Parameters;
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
 * This plugin performs Zhang's camera calibration on the pre-calculated corner point data for the M given target views.
 * Based on the estimated intrinsic and extrinsic (view) parameters, the corner points of the 3D target model are then
 * projected onto the corresponding calibration images (a stack). All rendering is done by pixel drawing (no graphic
 * overlays).
 *
 * @author W. Burger
 * @version 2022/04/14
 */
public class Do_Calibration implements PlugIn {

	private static ImageResource resource = CalibrationImage.CalibImageStack;

	private static boolean ListCameraIntrinsics = true;
	private static boolean ListCameraViews = true;

	private static boolean ShowProjectedModel = true;
	private static boolean MarkCornerPoints = true;

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

		int M = testIm.getNSlices();    // number of views
		if (M < 2) {
			IJ.error("Image must be a stack with 2+ images!");
			return;
		}
		testIm.show();

		if (!runDialog()) {
			return;
		}

		Point2D[] modelPoints = ZhangData.getModelPoints();
		Camera camReference = ZhangData.getCameraIntrinsics();
		Point2D[][] obsPoints = ZhangData.getAllObservedPoints();

		// Set up the calibrator ------------------------------------------

		Parameters params = new Calibrator.Parameters();
		params.normalizePointCoordinates = true;
		params.lensDistortionKoeffients = 2;
		params.useNumericJacobian = true;
		params.debug = false;

		Calibrator zcalib = new Calibrator(params, modelPoints);
		for (int i = 0; i < M; i++) {
			zcalib.addView(obsPoints[i]);
		}

		// Perform calibration ------------------------------------------

		Camera camFinal = zcalib.calibrate();
		if (camFinal == null) {
			IJ.error("Calibration failed");
			return;
		}
		ViewTransform[] finalViews = zcalib.getFinalViews();

		// Show results ------------------------------------------

		if (ListCameraIntrinsics) {
			IJ.log("\n**** Intrinsic camera parameters (common to all views): ****");
			IJ.log("Final estimate:\n   " + camFinal.toString());
			IJ.log("Reference (from EasyCalib):\n   " + camReference.toString());
		}

		if (ListCameraViews) {
			IJ.log("\n**** Camera view parameters (3D rotation and translation): ****");
			for (int i = 0; i < M; i++) {
				IJ.log("View " + i + ":\n" + finalViews[i].toString());
			}
			IJ.log(String.format("\nSquared projection error: %.3f\n",
					zcalib.getProjectionError(camFinal, finalViews, obsPoints)));
		}

		ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

		// draw the projected model (squares) given the camera/view parameters:
		if (ShowProjectedModel) {
			ola.setStroke(new ColoredStroke(StrokeWidth, ProjectedModelColor.getColor()));
			for (int i = 0; i < M; i++) {
				int sliceNo = i + 1;
				ola.setStackPosition(sliceNo);
				Point2D[] projPnts = camFinal.project(finalViews[i], modelPoints);
				for (Shape s : makeQuads(projPnts)) {
					ola.addShape(s);
				}
			}
		}

		// draw the (pre-calculated) image corner points used for calibration:
		if (MarkCornerPoints) {
			ola.setStroke(new ColoredStroke(StrokeWidth, CornerMarkColor.getColor()));
			// Point2D[][] obsPoints = ZhangData.getAllObservedPoints();
			for (int i = 0; i < obsPoints.length; i++) {
				int sliceNo = i + 1;
				ola.setStackPosition(sliceNo);
				for (Shape s : makeCircleShapes(obsPoints[i])) {
					ola.addShape(s);
				}
			}
		}

		testIm.setOverlay(ola.getOverlay());
	}

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
				"This plugin performs calibration on the supplied test images.",
				"Note that pre-calculated image corner coordinates are used, i.e.,",
				"no corner detection is performed."));

		gd.addCheckbox("List camera intrinsics", ListCameraIntrinsics);
		gd.addCheckbox("List camera views", ListCameraViews);

		gd.addCheckbox("Show projected model", ShowProjectedModel);
		gd.addCheckbox("Mark corner points", MarkCornerPoints);

		gd.addEnumChoice("Projected model color", ProjectedModelColor);
		gd.addEnumChoice("Corner mark color", CornerMarkColor);
		gd.addNumericField("Corner mark radius", CornerMarkRadius, 1);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		ListCameraIntrinsics = gd.getNextBoolean();
		ListCameraViews = gd.getNextBoolean();

		ShowProjectedModel = gd.getNextBoolean();
		MarkCornerPoints = gd.getNextBoolean();

		ProjectedModelColor = gd.getNextEnumChoice(BasicAwtColor.class);
		CornerMarkColor = gd.getNextEnumChoice(BasicAwtColor.class);
		CornerMarkRadius = gd.getNextNumber();
		return true;
	}

}
