package Calibration_Other;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.ResourceUtils;
import jarWithResouces.Root;	// test file: jarWithRousources.jar

/**
 * Opens Zhang's standard calibration images as a stack of RGB images. 
 * The image data are stored as a resource in the local Java class tree.
 * This plugin also demonstrates the use of the resource access
 * mechanism.
 * 
 * @author W. Burger
 *
 */
public class Open_Image_from_Resource implements PlugIn {
	
	static boolean BeVerbose = false;
	static String tmpDir = IJ.getDirectory("temp");
	
	static {
		IjLogStream.redirectSystem();
	}
	
	// resources are located in test file "jarWithResources.jar"
	static Class<?> resourceRootClass = Root.class;
	static String resourceName = "CalibImageStack.tif";
	static String resourceDir = "resources/";

	public void run(String arg0) {
		
		IJ.log("00: " + ResourceUtils.getResourcePath(resourceRootClass, resourceDir + resourceName));
		IJ.log("01: " + Root.class.getResource("resources/CalibImageStack.tif"));
		
		// list the contained resources:
//		Path[] paths = this.listResources(resourceRootClass, "resources/CalibImageStack.tif");
//		if (paths == null) {
//			IJ.log("no resources found!");
//		}
//		else {	
//			for (Path p : paths) {
//				IJ.log("path = " + p.toString());
//			}
//		}
		
		Path resourcePath = ResourceUtils.getResourcePath(resourceRootClass, resourceDir + resourceName); 
		if (resourcePath == null) {
			IJ.log("Image not found: " + resourceName);
			return;
		}
		
		IJ.log("resourcePath = " + resourcePath);
		IJ.log("resourceURI = " + resourcePath.toUri());
		IJ.log("resourcePath file name = " + resourcePath.getFileName());
//
//		ImagePlus im = null;
//		im = ResourceUtils.openImageFromResource(resourceRootClass, resourceDir, resourceName);
//		if (im != null) {
//			im.show();
//		}
//		else {
//			IJ.error("image could not be loaded!");
//		}
	}
	
	
	/**
	 * Use this method to obtain the paths to all files in a directory located
	 * relative to the specified class. This should work in an ordinary file system
	 * as well as a (possibly nested) JAR file.
	 * TODO: change to return empty array instead of null.
	 * 
	 * @param clazz class whose source location specifies the root 
	 * @param resDir path relative to the root class
	 * @return a sequence of paths or {@code null} if the specified path is not a directory
	 */
	private Path[] listResources(Class<?> clazz, String resDir) {
		IJ.log("listResources");
//		Path resourcePath = ResourceUtils.getResourcePath(clazz, resDir);
		
		Path resourcePath = this.getResourcePath(clazz, resDir);
		if (resourcePath == null) {
			IJ.log("resource path not found");
			return null; 	// cannot list if no directory
		}
		if (!Files.isDirectory(resourcePath)) {
			IJ.log("resource path is no directory");
			return null; 	// cannot list if no directory
		}
		
		IJ.log("listResources(): path = " + resourcePath.toString());
	
		List<Path> rlst = new ArrayList<Path>();
		// with help from http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file, #10
		Stream<Path> walk;
		try {
			walk = Files.walk(resourcePath, 1);
		} catch (IOException e) {
			System.err.println("listResources: " + e);
			return null;
		}	    
		for (Iterator<Path> it = walk.iterator(); it.hasNext();){
			Path p = it.next();
			//	        try {
			//				System.out.println("[listResources] " + p.getFileName().toString() + " " + Files.isRegularFile(p));
			//			} catch (Exception e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
			if (Files.isRegularFile(p) && Files.isReadable(p)) {
				rlst.add(p);
			}
		}
		walk.close();
		return rlst.toArray(new Path[0]);
	}
	
	/**
	 * Find the path to a resource relative to the location of class c.
	 * Example: Assume class C was loaded from file someLocation/C.class
	 * and there is a subfolder someLocation/resources/ that contains 
	 * an image 'lenna.jpg'. Then the absolute path to this image
	 * is obtained by 
	 * String path = getResourcePath(C.class, "resources/lenna.jpg");
	 * 
	 * 2016-06-03: modified to return proper path to resource inside 
	 * a JAR file.
	 * 
	 * @param clazz anchor class 
	 * @param relPath the path of the resource to be found (relative to the location of the anchor class)
	 * @return the path to the specified resource
	 */
	private Path getResourcePath(Class<?> clazz, String relPath) {
		IJ.log("getResourcePath");
//		URI uri = null;
//		try {
//			uri = clazz.getResource(relPath).toURI();
//		} catch (Exception e) {
//			System.err.println("getResourcePath: " + e);
//			return null;
//		}
		
		URI uri = ResourceUtils.getResourceUri(clazz, relPath);
		if (uri == null) {
			IJ.log("URI not found");
			return null;
		}
		IJ.log("getResourcePath(): uri = " + uri.toString());
		
		
		
		Path path = null;
		//IJ.log("getResourcePath(): path = " + path);
		String scheme = uri.getScheme();
		
		switch (scheme) {
		case "file": {	// resource in ordinary file system
			path = Paths.get(uri);
			break;
		}
		case "jar":	{	// resource inside a JAR file
			FileSystem fs = null;
			try {
				// check if this FileSystem already exists (can't create twice!)
				fs = FileSystems.getFileSystem(uri);
			} catch (Exception e) { }
	
			if (fs == null) {	// FileSystem does not yet exist in this runtime
				try {
					fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
				} catch (IOException e) { }
			}
			
			if (fs == null) {	// FileSystem could not be created for some reason
				throw new RuntimeException("FileSystem could not be created");
			}
			String ssp = uri.getSchemeSpecificPart();
			int startIdx = ssp.lastIndexOf('!');
			String inJarPath = ssp.substring(startIdx + 1);  // in-Jar path (after the last '!')
			path = fs.getPath(inJarPath);
			break;
		}
		default:
			throw new IllegalArgumentException("Cannot handle path type: " + scheme);
		}
		return path;
	}

}
