package Calibration_Plugins;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.FileUtils;

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * @author W. Burger
 *
 */
public class Open_Test_Images implements PlugIn {
	
	static boolean BeVerbose = false;
	
	static {
		IjLogStream.redirectSystem();
	}
	
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		String path = ZhangData.getResourcePath(resourceName);
		IJ.log("resource path = " + path);
		try {
			IJ.log("resource URL = " + new URL("file:" + path));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ------------------------------------------
		
		Path[] paths = FileUtils.listResources(ZhangData.class, "resources");
		for (Path p : paths) {
			IJ.log("path = " + p.toString());
			
		}
		
		InputStream strm = FileUtils.getResourceStream(ZhangData.class, "resources/" + resourceName);
		IJ.log("stream = " + strm);
		
		Path p2 = FileUtils.getResourcePath2(ZhangData.class, "resources/" + resourceName);
		IJ.log("path 2 = " + p2);
	

		
		// ------------------------------------------
		
		if (path == null) {
			System.out.println("Image not found: " + resourceName);
			return;
		}
		
		if (BeVerbose)
			System.out.println("Opening image: " + path);
			
		ImagePlus im = new Opener().openImage(path);
		if (im == null) {
			System.out.println("Opening failed.");
			return;
		}
		
		im.show();
	}
	

}
