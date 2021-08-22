package Calibration_Demos_More;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.data.zhang.ZhangData;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.resource.ResourceLocation;


/**
 * This plugin draws the projected X/Y/Z coordinate axes
 * for each of the given camera views.
 * 
 * @author W. Burger
 * @version 2021-08-22
 */
public class Demo_Draw_3D_Axes implements PlugIn {

	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	static Color BackGroundColor = Color.white;
	static Color LineColor = Color.magenta;
	static Color xColor = Color.red;
	static Color yColor = Color.green;
	static Color zColor = Color.blue;

	static double axisLength = 5;	
	static boolean BeVerbose = false;

	static {
		LogStream.redirectSystem();
		PrintPrecision.set(6);
	}

	public void run(String arg0) {
		// create a 3D model:

		double[] p0 = {0.0, 0.0, 0.0}; // 3D origin
		double[] p1 = {axisLength, 0.0, 0.0};
		double[] p2 = {0.0, axisLength, 0.0};
		double[] p3 = {0.0, 0.0, axisLength};

		// open the test image (stack):
		ResourceLocation loc = new imagingbook.calibration.data.zhang.DATA.RLOC();
		ImagePlus testIm = loc.getResource(resourceName).openAsImage();
		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();

		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();

		// get all view parameters (extrinsics):
		ViewTransform[] views = ZhangData.getAllViewTransforms();
		int M = views.length;

		// project and draw the model into the views:
		ImageStack stack = testIm.getStack();
		for (int i = 0; i < M; i++) {
			ImageProcessor ip = stack.getProcessor(i + 1);
			ip.setColor(xColor);
			drawProjectedSegment(ip, camera, views[i], p0, p1);
			ip.setColor(yColor);
			drawProjectedSegment(ip, camera, views[i], p0, p2);
			ip.setColor(zColor);
			drawProjectedSegment(ip, camera, views[i], p0, p3);
		}
		testIm.updateAndDraw();
	}

	private void drawProjectedSegment(ImageProcessor ip, Camera cam, ViewTransform V, double[] P1, double[] P2) {
		double[] u1 = cam.project(V, P1);
		int u1x = (int) Math.round(u1[0]);
		int u1y = (int) Math.round(u1[1]);
		double[] u2 = cam.project(V, P2);
		int u2x = (int) Math.round(u2[0]);
		int u2y = (int) Math.round(u2[1]);
		ip.drawLine(u1x, u1y, u2x, u2y);
	}


}
