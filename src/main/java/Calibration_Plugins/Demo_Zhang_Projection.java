package Calibration_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.extras.calibration.zhang.Camera;
import imagingbook.extras.calibration.zhang.ViewTransform;
import imagingbook.extras.calibration.zhang.testdata.ZhangData;
import imagingbook.extras.calibration.zhang.util.GridPainter;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;

import java.awt.Color;
import java.awt.geom.Point2D;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images and projects the model points into each view,
 * using the (known) camera and view parameters.
 * All data are part of Zhang's demo data set that comes with the
 * EasyCalib program. No calibration is performed.
 * 
 * @author W. Burger
 * @version 2015-05-25
 */
public class Demo_Zhang_Projection implements PlugIn {
	
	static Color BackGroundColor = Color.white;
	static Color LineColor = Color.magenta;
	static boolean BeVerbose = false;
	
	static final String TestImgName = "CalibImageStack.tif";
	static {
		IjLogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	
	@Override
	public void run(String arg0) {
		// open the test image (stack):
		String path = ZhangData.getResourcePath(TestImgName);
		ImagePlus testIm = new Opener().openImage(path);
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
