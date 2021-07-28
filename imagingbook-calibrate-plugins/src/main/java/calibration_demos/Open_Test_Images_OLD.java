package calibration_demos;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.lib.util.ResourceUtils;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * 
 * @author W. Burger
 * @version 2017-05-30
 *
 */
public class Open_Test_Images_OLD implements PlugIn {
	
	static {
		LogStream.redirectSystem();
	}
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		if(ResourceUtils.isInsideJar(resourceRootClass))
			IJ.log("Loading resource from JAR file");
		else
			IJ.log("Loading resource from file system");
		
		ImagePlus im = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not load resource!");
		}
	}
	
}
