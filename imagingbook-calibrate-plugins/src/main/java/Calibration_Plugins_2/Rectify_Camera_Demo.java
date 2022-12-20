/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.RectificationMapping;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.mappings.Mapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.image.interpolation.InterpolationMethod;
import imagingbook.core.resource.ImageResource;

/**
 * This plugin opens an image stack containing the 5 Zhang test images and removes the lens distortion based on the
 * intrinsic camera parameters estimated by calibration. The resulting (rectified) frames are shown in a new image
 * stack. Note that this plugin uses pre-calculated camera parameters, i.e., no calibration is performed.
 *
 * @author W. Burger
 * @version 2022/12/19
 */
public class Rectify_Camera_Demo implements PlugIn {

	private static ImageResource resource = CalibrationImage.CalibImageStack;

	public void run(String arg0) {
		// open the test image (stack):
		ImagePlus testIm = resource.getImagePlus();

		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();
		String title = testIm.getShortTitle();

		// get pre-calculated camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();

		// create a special geometric mapping
		Mapping2D mapping = new RectificationMapping(camera);	// inverse, ie., maps target to source

		// get the original (distorted) image stack:
		ImageStack distStack = testIm.getStack();
		final int w = distStack.getWidth();
		final int h = distStack.getHeight();
		final int M = distStack.getSize();

		// create a new stack for the rectified images:
		ImageStack rectStack = new ImageStack(w, h);
		for (int i = 0; i < M; i++) {
			ImageProcessor source = distStack.getProcessor(i + 1);
			ImageProcessor target = source.createProcessor(w, h);
			ImageMapper mapper = new ImageMapper(mapping, null, InterpolationMethod.Bicubic);
			mapper.map(source, target);
			String label = distStack.getSliceLabel(i + 1);
			rectStack.addSlice(label, target);
		}
		// display the new stack:
		new ImagePlus(title + " (rectified)", rectStack).show();
	}

}
