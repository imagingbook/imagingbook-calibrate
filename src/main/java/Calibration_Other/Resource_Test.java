package Calibration_Other;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import ij.IJ;
import ij.plugin.PlugIn;

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
public class Resource_Test implements PlugIn {
	
	static boolean BeVerbose = false;
	static String tmpDir = IJ.getDirectory("temp");
	
	static {
		IjLogStream.redirectSystem();
	}
	
	// resources are located in test file "jarWithResources.jar"
	static Class<?> resourceRootClass = Root.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";
	
	/*
	 * typ. JAR URI = jar:file:/C:/PROJEC~2/parent/IM1D84~1/ImageJ/jars/jarWithResources.jar!/jarWithResouces/resources/clown.jpg
	 * (non-Javadoc)
	 *
	 */

	public void run(String arg0) {
		
		//URI xuri = new URI("jar:file:/C:/PROJEC~2/parent/IM1D84~1/ImageJ/jars/jarWithResources.jar!/jarWithResouces/resources/clown.jpg);
		
//		IJ.log("00: " + this.getResourcePath(resourceRootClass, resourceDir + resourceName));
//		IJ.log("01: " + this.getResourcePath(resourceRootClass, "resources/CalibImageStack.tif"));
//		IJ.log("02: " + this.getResourcePath(resourceRootClass, "."));
		
//		testClass(this.getClass(), "resources/");
		testClass(resourceRootClass, "resources/clown.jpg");
		//testClass(resourceRootClass, "resources/");
		
		
//		Path[] paths = listResources(resourceRootClass, "/resources/CalibImageStack.tif");
//		
//		IJ.log("paths = " + paths);
		
		// list the contained resources:
	}
	
	private void testClass(Class<?> clazz, String path) {
		IJ.log("********* testing class = " + clazz.getName());
		IJ.log("class directory = " + getClassDir(clazz).toString());
		IJ.log("isInJar = " + isInJar(clazz));
		IJ.log("class URI = " + getClassURI(clazz));
		IJ.log("resource URI = " + getResourceURI(clazz, path));
		
		IJ.log("listing resources:");
		String[] resList = listResources2(clazz, path);
		if (resList == null) {
			IJ.log(" - none");
		}
		else {
			for (String s : listResources2(clazz, path)) {
				IJ.log("  - "  + s);
			}
		}
	}
	
	
	//http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file
	private static File getClassDir(Class<?> clazz) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
        String codeSourcePath = null;
		try {
			codeSourcePath = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) { }
		IJ.log(" +++ codeSourcePath = " + codeSourcePath);
		String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
		IJ.log(" +++ packagePath = " + packagePath);
        File thisClassPath = new File(codeSourcePath, packagePath);
        
        try {
			IJ.log(" +++ packageURI = " + new URI("jar", thisClassPath.getPath(), null));
		} catch (URISyntaxException e) {
		}
       
        return thisClassPath;

	}
	
	// -------------------------------------------------------------------------
	
	// see also http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file
	
	//https://brixomatic.wordpress.com/2013/01/27/listing-subdirectory-in-jar-file/
	private String[] listResources2(Class<?> clazz, String path) {
		URL dirURL = clazz.getResource(path);
		
		// resource path in file system
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
			String[] files = null;
			try {
				files = new File(dirURL.toURI()).list();
			} catch (URISyntaxException e) {
				// never happens
			}
			return files;
		}
		
		// resource path inside JAR
		if (dirURL == null) {
			IJ.log("making JAR file system:");
			URI classUri = getClassURI(clazz);
			IJ.log("classURI = " + classUri.toString());
			IJ.log("scheme = " + classUri.getScheme());
			IJ.log("ssp = " + classUri.getSchemeSpecificPart());
			IJ.log("fragment = " + classUri.getFragment());
			
			try {
				URI newUri = new URI("jar", classUri.getSchemeSpecificPart() + "!", null);
				IJ.log("newURI = " + newUri.toString());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}


		return null;
	}
	
	// -------------------------------------------------------------------------
	
	URI getClassURI(Class<?> clazz) {
		URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		URI uri = null;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			// never happens
		}
		return uri;
	}
	
	URI getResourceURI(Class<?> clazz, String  relPath) {
		URI uri = null;
		try {
			uri = clazz.getResource(relPath).toURI();
			//IJ.log("uri = " + uri.getPath().toString());
		} catch (Exception e) {
			IJ.log("getResourceURI: " + e.toString());
			return null;
		}
		return uri;
	}
	
	// needed?
	boolean isInJar(Class<?> clazz) {
		URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		String path = url.getPath();
		File file = new File(path);
		return file.isFile();
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
