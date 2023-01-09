/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.InterCameraMapping;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.mappings.Mapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.image.interpolation.InterpolationMethod;
import imagingbook.core.plugin.JavaDocHelp;
import imagingbook.core.resource.ImageResource;

import static imagingbook.common.ij.DialogUtils.formatText;


/**
 * This plugin opens an image stack containing the 5 Zhang test images (assumed to be taken with camera A) and
 * re-renders the images by mapping them to a new camera B. In this example, only the lens distortion coefficients are
 * modified but in principle all intrinsic parameters of camera B could be changed.
 *
 * @author W. Burger
 * @version 2021-08-22
 */
public class Replace_Camera_Demo implements PlugIn, JavaDocHelp {

	private static ImageResource resource = CalibrationImage.CalibImageStack;

	// modified lens distortion coefficients:
	private static double k1 = -0.1;
	private static double k2 =  2.0;

	public void run(String arg0) {
		// open the test image (stack):	
		ImagePlus testIm = resource.getImagePlus();

		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();

		// get the camera intrinsics (typically by calibration):
		Camera cameraA = ZhangData.getCameraIntrinsics();

		double[] cameraParameters = cameraA.getParameterVector();
		k1 = cameraParameters[5];
		k2 = cameraParameters[6];

		if (!runDialog()) {
			return;
		}

		cameraParameters[5] = k1;	// change only radial distortion parameters
		cameraParameters[6] = k2;

		Camera cameraB = new Camera(cameraParameters);

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
			ImageMapper mapper = new ImageMapper(mapping, null, InterpolationMethod.Bicubic);
			mapper.map(source, target);
//			mapping.applyTo(source, target, InterpolationMethod.Bicubic);
			rectStack.addSlice("frame"+ (i + 1), target);
		}

		new ImagePlus(testIm.getShortTitle() + " (modfied)", rectStack).show();
	}

	// -------------------------------------------------------------------------

	private boolean runDialog() {
		GenericDialog gd = new GenericDialog(this.getClass().getSimpleName());
		gd.addHelp(getJavaDocUrl());
		gd.setInsets(0, 0, 0);
		gd.addMessage(formatText(60,
				"This plugin transforms the stack of test images by replacing",
				"the original camera by a new camera with different parameters.",
				"Only the radial distortion parameters (κ1, κ2) are changed.",
				"For example, try κ1 = -0.1, κ2 = 2.0."));

		gd.addNumericField("Rad. distortion param. κ1", k1, 3);
		gd.addNumericField("Rad. distortion param. κ2", k2, 3);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		k1 = gd.getNextNumber();
		k2 = gd.getNextNumber();
		return true;
	}

}
