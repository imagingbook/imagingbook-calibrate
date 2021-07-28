package calibration_demos;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.io.LogStream;
import ij.io.Opener;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.lib.ij.IjUtils;
import imagingbook.lib.util.ResourceUtils;
import imagingbook.lib.util.resource.ResourceLocation;
import imagingbook.lib.util.resource.ResourceLocation.Resource;

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
	
//	static Class<?> resourceRootClass = ZhangData.class;
//	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	//static String resourceName = "marked1.png";
	//static String resourceName = "CalibIm1.jpg";
	//static String resourceName = "data1.txt";

	public void run(String args) {
		
		ResourceLocation loc = new imagingbook.calibration.zhang.data.files.RLOC();
		
		if(loc.insideJAR())
			IJ.log("Resources in JAR file: " + loc.getPath());
		else
			IJ.log("Resources in regular file: " + loc.getPath());
		
		URI rootURI = loc.getUri();
		Path rootPath = loc.getPath();
		
		IJ.log("getURI()           = " + rootURI);
		IJ.log("getPath()          = " + rootPath);
		IJ.log("Paths.get(rootURI) = " + Paths.get(rootURI));

		IJ.log("");

//		Resource[] resources1 = loc.getResources();
//		IJ.log("Resources found (1): " + resources1.length);
//		for (Resource res : resources1) {
//			IJ.log("   " + res.getName() + " | " + res.getURL());
//		}
		
//		Path[] paths2 = loc.getResourcePaths();
//		IJ.log("Resources found (2): " + paths2.length);
//		for (Path p : paths2) {
//			IJ.log("   " + p);
//		}
		
		String[] names = loc.getResourceNames();
		IJ.log("Resources found (names): " + names.length);
		for (String p : names) {
			IJ.log("   " + p);
		}
		
//		Path p = loc.getPath(resourceName);
//		if (p == null) {
//			IJ.error("Resource not found!");
//		}
		
//		IJ.log("\nPath to " + resourceName + ": " + p);
		
//		InputStream strm = loc.getResourceAsStream(resourceName);
//		IJ.log("\nStream to " + resourceName + ": " + strm);
		
		//ImagePlus im = IjUtils.openImage(p);	// does not work if in JAR
		//ImagePlus im = openTiffFromStream(strm, resourceName);	// works
		
		ImagePlus im = null;

		Resource res = loc.getResource(resourceName);
		IJ.log("\nOpening image: " + res.getName());
		IJ.log("Is in JAR: " + loc.insideJAR());
		
		Path path = res.getPath();
		IJ.log("Path = " + path + " (" + path.getClass() + ")");
		IJ.log("Path.toUri() = " + path.toUri());
		try {
			IJ.log("Path.toUri().toURL() = " + path.toUri().toURL());
		} catch (MalformedURLException e) {	}
		
		//im = new Opener().openURL(url.toString());
		im = new Opener().openImage(res.getUri().toString());
		
		//im = res.openAsImage(); // uses URL
		

		
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("Could not open image " + resourceName);
		}
		
		IJ.log("---------------------------------");
		IJ.log("res.getURI()  = " + res.getUri());
//		IJ.log("res.getURL()  = " + res.getURL());
		IJ.log("res.getPath() = " + res.getPath());
//		try {
//			IJ.log("res.getURL().toURI() = " + res.getURL().toURI());
//		} catch (URISyntaxException e) {}

		IJ.log("res.getStream() = " + res.getStream());
	}
	
	
//	private ImagePlus openTiffFromStream(InputStream is, String name) {
//		IJ.log("opening TIFF from stream ...");
//		return new Opener().openTiff(is, name);
//	}
	
}
