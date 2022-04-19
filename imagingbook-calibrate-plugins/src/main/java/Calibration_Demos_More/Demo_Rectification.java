package Calibration_Demos_More;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.data.zhang.CalibrationImage;
import imagingbook.calibration.data.zhang.ZhangData;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.RectificationMapping;
import imagingbook.core.resource.ImageResource;
import imagingbook.lib.image.ImageMapper;
import imagingbook.lib.interpolation.InterpolationMethod;
import imagingbook.lib.math.PrintPrecision;
import imagingbook.pub.geometry.mappings.Mapping2D;

/**
 * This plugin opens an image stack containing the 5 Zhang
 * test images and removes the lens distortion based on the calibrated 
 * camera parameters. The resulting (rectified) frames are shown
 * in a new image stack.
 * 
 * @author W. Burger
 * @version 2021-08-22
 */
public class Demo_Rectification implements PlugIn {

	static boolean BeVerbose = false;

	private static ImageResource resource = CalibrationImage.CalibImageStack_tif;	// = "CalibImageStack.tif"

	static {
		LogStream.redirectSystem();
		PrintPrecision.set(6);
	}

	public void run(String arg0) {
		// open the test image (stack):
		ImagePlus testIm = resource.getImage();

		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();
		String title = testIm.getShortTitle();

		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();

		// create a special geometric mapping
		Mapping2D mapping = new RectificationMapping(camera);	// inverse, ie., maps target to source

		// rectify the images and create a new stack:
		ImageStack distStack = testIm.getStack();
		final int w = distStack.getWidth();
		final int h = distStack.getHeight();
		final int M = distStack.getSize();
		ImageStack rectStack = new ImageStack(w, h);
		for (int i = 0; i < M; i++) {
			IJ.showProgress(i, M);
			ImageProcessor source = distStack.getProcessor(i + 1);
			ImageProcessor target = source.createProcessor(w, h);
			ImageMapper mapper = new ImageMapper(mapping, null, InterpolationMethod.Bicubic);
			mapper.map(source, target);
//			mapping.applyTo(source, target, InterpolationMethod.Bicubic);
			rectStack.addSlice("frame"+ (i + 1), target);
		}
		new ImagePlus(title + "-rectified", rectStack).show();
	}

}
