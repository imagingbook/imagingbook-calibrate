package Calibration_Other;

import java.io.IOException;
import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.FileUtils;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * 
 * @author W. Burger
 *
 */
public class Open_Image_from_Jar implements PlugIn {
	
	static boolean BeVerbose = false;
	static String tmpDir = IJ.getDirectory("temp");
	
	static {
		IjLogStream.redirectSystem();
	}
	
	static Class<?> resourceRootClass = ZhangData.class;
	static String resourceName = "CalibImageStack.tif";
	static String resourceDir = "resources/";

	public void run(String arg0) {
		Path resourcePath = FileUtils.getResourcePath(resourceRootClass, resourceDir + resourceName); 

		if (resourcePath == null) {
			System.out.println("Image not found: " + resourceName);
			return;
		}
		
		IJ.log("resourcePath = " + resourcePath);
		IJ.log("resourceURI = " + resourcePath.toUri());
		IJ.log("resourcePath file name = " + resourcePath.getFileName());

		ImagePlus im = null;
		try {
			im = FileUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("image could not be loaded!");
		}
	}

}
