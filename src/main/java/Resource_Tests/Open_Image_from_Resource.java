package Resource_Tests;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.ResourceUtils;
import jarWithResouces.Root;	// test file: jarWithRousources.jar

/**
 * Plugin that demonstrates how to load and open an ImageJ
 * image from a resource (placed in the file system or
 * inside a JAR file). 
 * 
 * @author W. Burger
 *
 */
public class Open_Image_from_Resource implements PlugIn {
	
	static {
		IjLogStream.redirectSystem();
	}
	
	// resources are located in test file "jarWithResources.jar"
	static Class<?> resourceRootClass = Root.class;
	static String resourceName = "CalibImageStack.tif";
	static String resourceDir = "resources/";

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
