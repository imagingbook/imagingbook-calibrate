package Calibration_Demos;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.core.resource.ImageResource;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. The image data are stored as a resource in the
 * local Java class tree. This plugin also demonstrates the use of the resource access mechanism.
 *
 * @author W. Burger
 * @version 2021/08/22
 */
public class Open_Test_Images implements PlugIn {
	
	static {
		LogStream.redirectSystem();
	}
	
	static ImageResource resource = CalibrationImage.CalibImageStack;

	public void run(String args) {
		
		if (resource == null) {
			IJ.error("Could not find resource " + resource);
			return;
		}
		
		if(resource.isInsideJar())
			IJ.log("Resources from JAR file: " + resource.getURL());
		else
			IJ.log("Resources from regular file: " + resource.getURL());
		
		ImagePlus im = resource.getImagePlus();
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not open image " + resource);
		}
	}
	
}
