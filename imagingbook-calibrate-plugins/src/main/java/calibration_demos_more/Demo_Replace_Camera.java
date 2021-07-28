package calibration_demos_more;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.InterCameraMapping;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.lib.image.ImageMapper;
import imagingbook.lib.interpolation.InterpolationMethod;
import imagingbook.lib.settings.PrintPrecision;
import imagingbook.lib.util.ResourceUtils;
import imagingbook.pub.geometry.mappings.Mapping2D;


/**
 * This plugin projects opens an image stack containing the 5 Zhang
 * test images (assumed to be taken with camera A) and re-renders 
 * the images by mapping them to a new camera B. In this example,
 * only the lens distortion coefficients are modified but in
 * principle all intrinsic parameters of camera B could be changed.
 * 
 * @author W. Burger
 * @version 2017-05-30
 */
public class Demo_Replace_Camera implements PlugIn {

	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	// modified lens distortion coefficients:
	static double k1 = -0.1;
	static double k2 =  2.0;

	static boolean BeVerbose = false;

	//static final String TestImgName = "CalibImageStack.tif";
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
		String title = testIm.getShortTitle();

		// get the camera intrinsics (typically by calibration):
		Camera cameraA = ZhangData.getCameraIntrinsics();

		double[] paramsA = cameraA.getParameterVector();
		paramsA[5] = k1;
		paramsA[6] = k2;

		Camera cameraB = new Camera(paramsA);

		// create a special geometric mapping
		Mapping2D mapping = new InterCameraMapping(cameraA, cameraB);	// inverse, maps target to source

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

		new ImagePlus(title+"-modfied", rectStack).show();
	}

}
