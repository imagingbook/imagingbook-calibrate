package Calibration_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.extras.calibration.zhang.Camera;
import imagingbook.extras.calibration.zhang.testdata.ZhangData;
import imagingbook.extras.calibration.zhang.util.MathUtil;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.pub.geometry.mappings.Mapping;
import imagingbook.lib.interpolation.InterpolationMethod;

import org.apache.commons.math3.linear.RealMatrix;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images and removes the lens distortion based on the calibrated 
 * camera parameters. The resulting rectified frames are shown
 * in a new image stack.
 * 
 * @author W. Burger
 * @version 2015-06-05
 */
public class Demo_Rectification implements PlugIn {

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
		ImagePlus distIm = new Opener().openImage(path);
		if (distIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		distIm.show();
		String title = distIm.getShortTitle();
		
		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();
		
		// create a special geometric mapping
		Mapping mapping = new UndistortMapping(camera);
		
		// rectify the images and create a new stack:
		ImageStack distStack = distIm.getStack();
		final int w = distStack.getWidth();
		final int h = distStack.getHeight();
		final int M = distStack.getSize();
		ImageStack rectStack = new ImageStack(w, h);
		for (int i = 0; i < M; i++) {
			IJ.showProgress(i, M);
			ImageProcessor source = distStack.getProcessor(i + 1);
			ImageProcessor target = source.createProcessor(w, h);
			mapping.applyTo(source, target, InterpolationMethod.Bicubic);
			rectStack.addSlice("frame"+ (i + 1), target);
		}
		new ImagePlus(title + "-rectified", rectStack).show();
	}

	/** 
	 * This class represents a special geometric mapping for
	 * rectifying (i.e., removing the lens distortion from)
	 * an image, given the associated camera parameters.
	 */
	private class UndistortMapping extends Mapping {
		private final Camera cam;
		private final RealMatrix Ai;	// inverse of the intrinsic camera matrix (2 x 3)

		public UndistortMapping (Camera cam) {
			this.isInverseFlag = true;	// maps target -> source
			this.cam = cam;
			this.Ai = cam.getInverseA();
		}
		
		@Override
		public double[] applyTo(double[] uv) {
			// (u,v) is an observed sensor point
			// apply the inverse camera mapping to get the normalized (x,y) point:
			double[] xy = Ai.operate(MathUtil.toHomogeneous(uv));
			// apply the camera's radial lens distortion in the normalized plane:
			double[] xyd = cam.warp(xy);
			// apply the (forward) camera mapping to get the undistorted sensor point (u',v'):
			return cam.mapToSensorPlane(xyd);
		}
	}
	

}
