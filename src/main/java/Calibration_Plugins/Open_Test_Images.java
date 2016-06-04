package Calibration_Plugins;

import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * @author W. Burger
 *
 */
public class Open_Test_Images implements PlugIn {
	
	static {
		IjLogStream.redirectSystem();
	}
	
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		Path path = ZhangData.getResourcePath(resourceName);
		IJ.log("resource path = " + path);
		
		if (path == null) {
			System.out.println("Image not found: " + resourceName);
			return;
		}
		
		ImagePlus im = new Opener().openImage(path.toString());
		if (im == null) {
			System.out.println("Opening failed.");
			return;
		}
		
		im.show();
	}
	

}
