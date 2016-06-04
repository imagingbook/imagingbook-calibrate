package Calibration_Plugins;

import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.RectificationMapping;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.interpolation.InterpolationMethod;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;
import imagingbook.pub.geometry.mappings.Mapping;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images and removes the lens distortion based on the calibrated 
 * camera parameters. The resulting rectified frames are shown
 * in a new image stack.
 * 
 * @author W. Burger
 * @version 2016-05-31
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
		Path path = ZhangData.getResourcePath(TestImgName);
		ImagePlus distIm = new Opener().openImage(path.toString());
		if (distIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		distIm.show();
		String title = distIm.getShortTitle();
		
		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();
		
		// create a special geometric mapping
		Mapping mapping = new RectificationMapping(camera);
		
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

}
