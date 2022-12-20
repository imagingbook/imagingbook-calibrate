/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_1;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.core.resource.ImageResource;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. The image data are stored as a resource in the
 * local Java class tree. Also demonstrates the use of the resource access mechanism.
 *
 * @author WB
 * @version 2021/08/22
 */
public class Open_Image_Stack implements PlugIn {
	
	static ImageResource resource = CalibrationImage.CalibImageStack;

	public void run(String args) {
		
		if (resource == null) {
			IJ.error("Could not find resource " + resource);
			return;
		}
		
		// if (resource.isInsideJar())
		// 	IJ.log("Loading resources from JAR file: " + resource.getURL());
		// else
		// 	IJ.log("Loading resources from regular file: " + resource.getURL());
		
		ImagePlus im = resource.getImagePlus();
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not open image " + resource);
		}
	}
	
}
