package Calibration_Demos;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.common.math.PrintPrecision;
import imagingbook.core.resource.ImageResource;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


/**
 * This plugin projects opens an image stack containing the 5 Zhang test images, then outlines the positions of the
 * observed image points and finally projects the points of the calibration model using the calculated intrinsic camera
 * parameters (same for all views) and the extrinsic parameters calculated for each view. All data are part of Zhang's
 * demo data set that comes with the EasyCalib program. NO CALIBRATION is performed here!
 * Graphic elements are drawn as non-destructive vector overlays: BLUE circles: observed corner points (used for
 * calibration). RED markers: projected model points (to validate parameters). - LOOK CLOSELY / ZOOM IN! The complete
 * stack with overlay can be saved as a TIFF file.
 *
 * @author W. Burger
 * @version 2022/04/14
 */
public class Demo_Zhang_Projection_Overlay implements PlugIn {
	
	private static ImageResource resource = CalibrationImage.CalibImageStack;	// = "CalibImageStack.tif"
	
	static double CircleRadius = 1.0;
	static double CrossRadius  = 2.0;
	static Color CircleColor = Color.blue;
	static Color CrossColor = Color.red;
	static double StrokeWidth  = 0.25;

	public void run(String arg0) {
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		
		int M = testIm.getNSlices();
		ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

		// plot the observed image points as circles:
		ola.setStroke(new ColoredStroke(StrokeWidth, CircleColor));
		Point2D[][] obsPoints = ZhangData.getAllObservedPoints();
		for (int i = 0; i < obsPoints.length; i++) {
			int sliceNo = i + 1;
			ola.setStackPosition(sliceNo);	// associate overly with this slice (important)
			for (Shape s : makeCircleRois(obsPoints[i])) {	// , CircleColor
				ola.addShape(s);
			}
		}
		
		// plot the projected model points as crosses:
		Point2D[] modelPoints = ZhangData.getModelPoints();
		Camera camReal = ZhangData.getCameraIntrinsics();
		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();
		ola.setStroke(new ColoredStroke(StrokeWidth, CrossColor));
		for (int i = 0; i < M; i++) {
			int sliceNo = i + 1;
			ola.setStackPosition(sliceNo);
			Point2D[] projPnts = camReal.project(viewsReal[i], modelPoints);
			for (Shape s : makeCrossRois(projPnts)) {	// CrossColor
				ola.addShape(s);
			}
		}
		
		testIm.setOverlay(ola.getOverlay());
		testIm.show();	
	}

	// ----------------------------------------------------------------------
	
	List<Shape> makeCrossRois(Point2D[] pnts) {  // Color lineCol, StrokeWidth
		final double r = CrossRadius;
		final double ofs = 0.5;	// pixel offset (elements to be placed at pixel centers)
		List<Shape> shapes = new ArrayList<>(pnts.length);
		for (int j = 0; j < pnts.length; j++) {
			double x = pnts[j].getX();
			double y = pnts[j].getY();
			Path2D path = new Path2D.Double();
			path.moveTo(x - r, y);
			path.lineTo(x + r, y);
			path.moveTo(x, y - r);
			path.lineTo(x, y + r);
			shapes.add(path);
		}
		return shapes;
	}
	
	List<Shape> makeCircleRois(Point2D[] pnts) {	// , Color lineCol
		final double r = CircleRadius;
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
	
}
