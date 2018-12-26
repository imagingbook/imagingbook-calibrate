package calibration_demos;

import java.awt.Color;
import java.awt.geom.Point2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Calibrator;
import imagingbook.calibration.zhang.Calibrator.Parameters;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;

/**
 * This plugin performs Zhang's camera calibration on the
 * pre-calculated point data for the N given target views.
 * Based on the estimated intrinsic and extrinsic (view)
 * parameters, the corner points of the 3D target model are
 * then projected onto the corresponding calibration images
 * (a stack).
 * All rendering is done by pixel drawing (no graphic overlays).
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Zhang_Calibration implements PlugIn {
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	
	static boolean ShowObservedModelPoints = true;		// draw observed image points into a new stack
	static boolean ShowProjectedImagePoints = true;		// draw projected image points into the test image stack
	static boolean ListCameraViews = true;
	static boolean BeVerbose = false;
	
	static Color BackGroundColor = Color.white;
	
	static {
		IjLogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	
	public void run(String arg0) {
		ImagePlus testIm = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
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
		if (BeVerbose)
			System.out.println("Camera intrinsics (real):");
			System.out.println(camReal.toString());
		
//		ViewTransform[] viewsReal = ZhangData.getAllViewTransforms();	
		Point2D[][] obsPoints 	  = ZhangData.getAllObservedPoints();
			
		if (ShowObservedModelPoints){
			ImageStack stack = new ImageStack(width, height);
			drawSquares(stack, obsPoints);
			new ImagePlus("Observed points", stack).show();
		}
		
		// perform calibration ------------------------------------------
		
		Parameters params = new Calibrator.Parameters();
		params.normalize = true;
		params.assumeZeroSkew = false;
		params.lensDistortionKoeffients = 2;
		params.beVerbose = BeVerbose;
		
		Calibrator zcalib = new Calibrator(params, modelPoints);
		for (int i = 0; i < M; i++) {
			zcalib.addView(obsPoints[i]);
		}
		
		Camera camFinal = zcalib.calibrate();
		if (camFinal == null) {
			System.out.println("calibration failed");
			return;
		}
		
		// show results ------------------------------------------
		
		System.out.println("Camera intrinsics (final):");
		System.out.println(camFinal.toString());
		
		ViewTransform[] finalViews = zcalib.getFinalViews();
		
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
	 * Draws the array of image points to a given (possibly empty) stack image.
	 * The image points are assumed to be the corners of the standard
	 * calibration model, i.e., 4 consecutive points form a projected square.
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
