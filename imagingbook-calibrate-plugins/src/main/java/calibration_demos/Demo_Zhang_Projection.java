package calibration_demos;

import java.awt.Color;
import java.awt.geom.Point2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.data.zhang.ZhangData;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images and projects the model points into each view,
 * using the (known) camera and view parameters.
 * All data are part of Zhang's demo data set that comes with the
 * EasyCalib program. NO CALIBRATION is performed here!
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Zhang_Projection implements PlugIn {
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	
	static Color BackGroundColor = Color.white;
	static Color LineColor = Color.magenta;
	static boolean BeVerbose = false;
	
	static {
		LogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	

	public void run(String arg0) {
		// open the test image (stack):
		ImagePlus testIm = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();
		
		// get the 3D model points:
		Point2D[] modelPoints = ZhangData.getModelPoints();
		
		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();
		
		// get all view parameters (extrinsics):
		ViewTransform[] views = ZhangData.getAllViewTransforms();
		int M = views.length;

		// project and draw the model into the views:
		ImageStack stack = testIm.getStack();
		for (int i = 0; i < M; i++) {
			// project the model points:
			Point2D[] projectedPoints = camera.project(views[i], modelPoints);
			ImageProcessor ip = stack.getProcessor(i + 1);
			// draw the squares:
			GridPainter painter = new GridPainter(ip);
			painter.lineCol = LineColor;
			painter.drawSquares(projectedPoints);
		}
		testIm.updateAndDraw();
	}

}
