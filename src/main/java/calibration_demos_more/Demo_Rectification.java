package calibration_demos_more;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.RectificationMapping;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.image.ImageMapper;
import imagingbook.lib.interpolation.InterpolationMethod;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;
import imagingbook.pub.geometry.mappings.Mapping;

/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images and removes the lens distortion based on the calibrated 
 * camera parameters. The resulting (rectified) frames are shown
 * in a new image stack.
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Rectification implements PlugIn {

	static boolean BeVerbose = false;

	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	static {
		IjLogStream.redirectSystem();
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
		String title = testIm.getShortTitle();

		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();

		// create a special geometric mapping
		Mapping mapping = new RectificationMapping(camera);	// inverse, ie., maps target to source

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
			ImageMapper mapper = new ImageMapper(mapping, InterpolationMethod.Bicubic);
			mapper.map(source, target);
//			mapping.applyTo(source, target, InterpolationMethod.Bicubic);
			rectStack.addSlice("frame"+ (i + 1), target);
		}
		new ImagePlus(title + "-rectified", rectStack).show();
	}

}
