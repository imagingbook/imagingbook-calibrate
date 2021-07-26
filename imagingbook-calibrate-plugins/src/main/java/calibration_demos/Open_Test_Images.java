package calibration_demos;

import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjUtils;
import imagingbook.lib.util.ResourceLocation;
import imagingbook.lib.util.ResourceUtils;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * 
 * @author W. Burger
 * @version 2021-07-26
 *
 */
public class Open_Test_Images implements PlugIn {
	
	static {
		LogStream.redirectSystem();
	}
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		
		ResourceLocation loc = new imagingbook.calibration.zhang.testdata.resources.Resources();	
		if(loc.isInsideJAR())
			IJ.log("Loading resource from JAR file: " + resourceName);
		else
			IJ.log("Loading resource from file system: " + resourceName);
		
		Path p = loc.getResourcePath(resourceName);
		if (p == null) {
			IJ.error("Resource not found!");
		}
		
		IJ.log("\nPath to " + resourceName + ": " + p);
		
		ImagePlus im = IjUtils.openImage(p);
		
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not open image!");
		}
	}
	
}
