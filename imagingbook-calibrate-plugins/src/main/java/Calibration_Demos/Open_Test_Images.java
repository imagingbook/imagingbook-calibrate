package Calibration_Demos;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import imagingbook.lib.util.resource.ResourceLocation;
import imagingbook.lib.util.resource.ResourceLocation.Resource;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * 
 * @author W. Burger
 * @version 2021-08-22
 *
 */
public class Open_Test_Images implements PlugIn {
	
	static {
		LogStream.redirectSystem();
	}
	
	static String resourceName = "CalibImageStack.tif";

	public void run(String args) {
		
		ResourceLocation loc = new imagingbook.calibration.data.zhang.DATA.RLOC();
		if(loc.insideJAR())
			IJ.log("Resources from JAR file: " + loc.getPath());
		else
			IJ.log("Resources from regular file: " + loc.getPath());
		
		Resource res = loc.getResource(resourceName);
		if (res == null) {
			IJ.error("Could not find resource " + resourceName);
			return;
		}
		
		ImagePlus im = res.openAsImage();
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not open image " + resourceName);
		}
	}
	
}
