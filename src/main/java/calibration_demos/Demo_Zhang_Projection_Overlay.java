package calibration_demos;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images, then outlines the positions of the observed image 
 * points and finally projects the points of the calibration model
 * using the calculated intrinsic camera parameters (same for all
 * views) and the extrinsic parameters calculated for each view.
 * All data are part of Zhang's demo data set that comes with the
 * EasyCalib program. No calibration is performed.
 * 
 * Graphic elements are drawn as non-destructive vector overlays:
 * Blue circles: observed corner points (used for calibration).
 * Red crosses: projected model points (to validate parameters).
 * - LOOK CLOSELY! 
 * The complete stack with overlay can be saved as a tiff file. 
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Zhang_Projection_Overlay implements PlugIn {
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	
	static double CircleRadius = 1.0;
	static double CrossRadius  = 2.0;
	static Color CircleColor = Color.blue;
	static Color CrossColor = Color.red;
	static double StrokeWidth  = 0.25;
	
	static boolean BeVerbose = false;
	
	static {
		IjLogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	
	@Override
	public void run(String arg0) {
		ImagePlus testIm = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		
		int M = testIm.getNSlices();
		Overlay oly = new Overlay();
		
		// plot the observed image points as circles:
		Point2D[][] obsPoints = ZhangData.getAllObservedPoints();
		for (int i = 0; i < obsPoints.length; i++) {
			int sliceNo = i + 1;
			for (Roi roi : makeCircleRois(obsPoints[i], CircleColor)) {
				roi.setPosition(sliceNo);	// associate overly with this slice (important)
				oly.add(roi);
			}
		}
		
		// plot the projected model points as crosses:
		Point2D[] modelPoints = ZhangData.getModelPoints();
		Camera camReal = ZhangData.getCameraIntrinsics();
		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();
		
		for (int i = 0; i < M; i++) {
			int sliceNo = i + 1;
			Point2D[] projPnts = camReal.project(viewsReal[i], modelPoints);
			for (Roi roi : makeCrossRois(projPnts, CrossColor)) {
				roi.setPosition(sliceNo);	// associate overly with this slice (important)
				oly.add(roi);
			}
		}
		
		testIm.setOverlay(oly);
		testIm.show();	
	}

	// ----------------------------------------------------------------------
	
	List<Roi> makeCrossRois(Point2D[] pnts, Color lineCol) {
		final double r = CrossRadius;
		final double ofs = 0.5;	// pixel offset (elements to be placed at pixel centers)
		List<Roi> rois = new ArrayList<Roi>();
		for (int j = 0; j < pnts.length; j++) {
			double x = pnts[j].getX();
			double y = pnts[j].getY();
			Line linX = new Line(x - r + ofs, y + ofs, x + r + ofs, y + ofs);
			Line linY = new Line(x + ofs, y - r + ofs, x + ofs, y + r + ofs);
			linX.setStrokeColor(lineCol);
			linY.setStrokeColor(lineCol);
			linX.setStrokeWidth(StrokeWidth);
			linY.setStrokeWidth(StrokeWidth);
			rois.add(linX);
			rois.add(linY);
		}
		return rois;
	}
	
	List<Roi> makeCircleRois(Point2D[] pnts, Color lineCol) {
		final double r = CircleRadius;
		final double ofs = 0.5;	// pixel offset (elements to be placed at pixel centers)
		List<Roi> rois = new ArrayList<Roi>();
		for (int j = 0; j < pnts.length; j++) {
			double x = pnts[j].getX();
			double y = pnts[j].getY();
			OvalRoi circle = new OvalRoi(x - r + ofs, y - r + ofs, 2 * r, 2 * r) ;
			circle.setStrokeColor(lineCol);
			circle.setStrokeWidth(StrokeWidth);
			rois.add(circle);
		}
		return rois;
	}
	
}
