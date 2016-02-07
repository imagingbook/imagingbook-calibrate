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
 * test images (assumed to be taken with camera A) and re-renders 
 * the images by mapping them to a new camera B. In this example,
 * only the lens distortion coefficients are modified but in
 * principle all intrinsic parameters of camera B could be changed.
 * 
 * @author W. Burger
 * @version 2015-06-05
 */
public class Demo_Replace_Camera implements PlugIn {

	// modified lens distortion coefficients:
	static double k1 = -0.1;
	static double k2 =  2.0;
	
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
		Camera cameraA = ZhangData.getCameraIntrinsics();
			
		double[] paramsA = cameraA.getParameterVector();
		paramsA[5] = k1;
		paramsA[6] = k2;
		
		Camera cameraB = new Camera(paramsA);
		
		// create a special geometric mapping
		Mapping mapping = new InterCameraMapping(cameraA, cameraB);
				
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
		
		new ImagePlus(title+"-modfied", rectStack).show();
	}
	
	
	/** 
	 * This class represents the geometric transformation for an image taken 
	 * with some camera A to an image taken with another camera B.
	 */
	private class InterCameraMapping extends Mapping {
		
		private final Camera camA, camB;
		private final RealMatrix Abi;	// inverse of the intrinsic camera b matrix (2 x 3)

		public InterCameraMapping (Camera camA, Camera camB) {
			this.isInverseFlag = true;	// maps target -> source
			this.camA = camA;		// camera A (used to produce the source image)
			this.camB = camB;		// camera B (determines the geometry the target image)
			this.Abi = camB.getInverseA();
		}
		
		@Override
		public double[] applyTo(double[] uv) {
			// (u,v) is an observed sensor point
			// apply the inverse camera mapping to get the distorted (x,y) point:
			double[] xy = Abi.operate(MathUtil.toHomogeneous(uv));
			
			// remove the lens distortion of camera b:
			double[] xyu = camB.unwarp(xy);
			
			// apply the lens distortion of camera a:
			double[] xyd = camA.warp(xyu);
			
			// apply the (forward) camera mapping to get the undistorted sensor point (u',v'):
			return camA.mapToSensorPlane(xyd);
		}

	}
	
}
