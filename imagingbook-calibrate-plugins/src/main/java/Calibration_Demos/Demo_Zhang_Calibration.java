package Calibration_Demos;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.calibration.zhang.Calibrator;
import imagingbook.calibration.zhang.Calibrator.Parameters;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.common.math.PrintPrecision;
import imagingbook.core.resource.ImageResource;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * This plugin performs Zhang's camera calibration on the pre-calculated point data for the N given target views. Based
 * on the estimated intrinsic and extrinsic (view) parameters, the corner points of the 3D target model are then
 * projected onto the corresponding calibration images (a stack). All rendering is done by pixel drawing (no graphic
 * overlays).
 *
 * @author W. Burger
 * @version 2022/04/14
 */
public class Demo_Zhang_Calibration implements PlugIn {
	
	private static boolean DEBUG = true;
	private static ImageResource resource = CalibrationImage.CalibImageStack;	// = "CalibImageStack.tif"
	
	private static boolean ShowObservedModelPoints = true;		// draw observed image points into a new stack
	private static boolean ShowProjectedImagePoints = true;		// draw projected image points into the test image stack
	private static boolean ListCameraViews = true;
	
	private static Color BackGroundColor = Color.white;
	
	static {
		LogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	
	public void run(String arg0) {
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		
		int M = testIm.getNSlices(); 	// number of views
		if (M < 2) {
			IJ.error("Image must be a stack with 2+ images!"); 
			return;
		}
		
		testIm.show();
		int width = testIm.getWidth();
		int height = testIm.getHeight();
		Point2D[] modelPoints = ZhangData.getModelPoints();
		Camera camReal = ZhangData.getCameraIntrinsics();
		if (DEBUG) {
			System.out.println("Camera intrinsics (reference from EasyCalib):");
			System.out.println(camReal.toString());
		}
		
//		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();	
		Point2D[][] obsPoints 	  = ZhangData.getAllObservedPoints();
			
		if (ShowObservedModelPoints){
			ImageStack stack = new ImageStack(width, height);
			drawSquares(stack, obsPoints);
			new ImagePlus("Observed points", stack).show();
		}
		
		// Set up calibrator ------------------------------------------
		
		Parameters params = new Calibrator.Parameters();
		params.normalizePointCoordinates = true;
		params.lensDistortionKoeffients = 2;
		params.useNumericJacobian = true;
		params.debug = DEBUG;
		
		Calibrator zcalib = new Calibrator(params, modelPoints);
		for (int i = 0; i < M; i++) {
			zcalib.addView(obsPoints[i]);
		}
		
		// Perform calibration ------------------------------------------
		
		Camera camFinal = zcalib.calibrate();
		if (camFinal == null) {
			System.out.println("calibration failed");
			return;
		}
		
		// Show results ------------------------------------------
		
		System.out.println("Camera intrinsics (final estimate):");
		System.out.println(camFinal.toString());
		
		ViewTransform[] finalViews = zcalib.getFinalViews();
		System.out.format("Final (squared) projection error: %.3f\n", zcalib.getProjectionError(camFinal, finalViews, obsPoints));
				
		if (ListCameraViews) {
			for (int i = 0; i < M; i++) {
				System.out.println("**** View " + i + ": ****");
				System.out.println(finalViews[i].toString());
			}
		}
		
		if (ShowProjectedImagePoints) {
			Point2D[][] projPoints = new Point2D[M][];
			for (int i = 0; i < M; i++) {
				projPoints[i] = camFinal.project(finalViews[i], modelPoints);
			}
			drawSquares(testIm.getStack(), projPoints);
			testIm.updateAndDraw();
		}
	}
	
	// --------------------------------------------------------------------

	/**
	 * Draws the array of image points to a given (possibly empty) stack image. The image points are assumed to be the
	 * corners of the standard calibration model, i.e., 4 consecutive points form a projected square.
	 *
	 * @param stack a stack with M images (views)
	 * @param imagePoints a sequence of 2D point sets, one for each view
	 */
	private void drawSquares(ImageStack stack, Point2D[][] imagePoints) {
		final int width = stack.getWidth();
		final int height = stack.getHeight();
		int M = imagePoints.length;
		for (int i = 0; i < M; i++) {
			ImageProcessor ip;
			if (stack.getSize() > i) {	// use existing stack slice
				ip = stack.getProcessor(i + 1);
			}
			else {						// create and fill new slice
				ip = new ColorProcessor(width, height);
				if (BackGroundColor != null) {
					ip.setColor(BackGroundColor);
					ip.fill();
				}
				stack.addSlice("View" + i, ip);
			}
			GridPainter painter = new GridPainter(ip);
			painter.drawSquares(imagePoints[i]);
		}
	}

}
