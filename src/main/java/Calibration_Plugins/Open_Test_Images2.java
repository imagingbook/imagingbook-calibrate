package Calibration_Plugins;

import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.ResourceUtils;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * @author W. Burger
 * 
 * TODO: fix opening images from JAR, see Open_Image_from_Resource.java (in programming examples)!
 *
 */
public class Open_Test_Images2 implements PlugIn {
	
	static {
		IjLogStream.redirectSystem();
	}
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		if(ResourceUtils.isInsideJar(resourceRootClass))
			IJ.log("Loading image from JAR");
		else
			IJ.log("Loading image from file system");
		
		ImagePlus im = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("image could not be loaded!");
		}
	}
	
}
