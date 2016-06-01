package Calibration_Other;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

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
public class Open_Image_from_Jar implements PlugIn {
	
	static boolean BeVerbose = false;
	static String tmpDir = IJ.getDirectory("temp");
	
	static {
		IjLogStream.redirectSystem();
	}
	
	static String resourceName = "CalibImageStack.tif";

	public void run(String arg0) {
		Path resourcePath = getResourcePath(ZhangData.class, "resources/" + resourceName); 

		if (resourcePath == null) {
			System.out.println("Image not found: " + resourceName);
			return;
		}
		
		IJ.log("resourcePath = " + resourcePath);
		IJ.log("resourceURI = " + resourcePath.toUri());
		IJ.log("resourcePath file name = " + resourcePath.getFileName());
		
		ImagePlus im = openImageFromResource(ZhangData.class, "resources/" + resourceName);
		
		if (im != null) {
			im.show();
		}
		else {
			IJ.error("image could not be loaded!");
		}
	}
	
//	private ImagePlus openImageFromResource(Class<?> clazz, String relPath) {
//		//Path path = this.getResourcePath(clazz, relPath);
//		URI uri = this.getResourceURI(clazz, relPath);
//		String scheme = uri.getScheme();
//		switch (scheme) {
//		case "file": {	// resource in ordinary file system
//			IJ.log("opening image from file");
//			return new Opener().openImage(uri.getPath());
//		}
//		case "jar":
//			IJ.log("opening image from JAR");
//			String filename = null;
//			try {
//				filename = uri.toURL().getFile();
//			} catch (MalformedURLException e1) {}
//			String tmpPath = tmpDir + "/" + filename;
//			IJ.log("tmpPath = " + tmpPath);
//			File tmpFile = new File(tmpPath);	// use File.createTempFile()?
//			InputStream inStrm = clazz.getResourceAsStream(relPath);
//			ImagePlus im = null;
//			try {
//				copyFile(inStrm, tmpFile);
//				im = new Opener().openImage(tmpPath);
//				Files.deleteIfExists(tmpFile.toPath());
//			} catch (IOException e) { }
//			return im;
//		default:
//			throw new IllegalArgumentException("Cannot handle this path type: " + scheme);
//		}
//	}
	

	
	private ImagePlus openImageFromResource(Class<?> clazz, String relPath) {
		Path path = this.getResourcePath(clazz, relPath);
		URI uri = path.toUri();
		String scheme = uri.getScheme();
		switch (scheme) {
		case "file": {	// resource in ordinary file system
			IJ.log("opening image from file");
			return new Opener().openImage(path.toString());
		}
		case "jar":
			IJ.log("opening image from JAR");
			String tmpPath = tmpDir + "/" + path.getFileName();
			File tmpFile = new File(tmpPath);	// TODO: use File.createTempFile()?
			InputStream inStrm = clazz.getResourceAsStream(relPath);
			ImagePlus im = null;
			try {
				copyToFile(inStrm, tmpFile);
				im = new Opener().openImage(tmpPath);
				Files.deleteIfExists(tmpFile.toPath());
			} catch (IOException e) { }
			return im;
		default:
			throw new IllegalArgumentException("Cannot handle this path type: " + scheme);
		}
	}
	
	public File extractResourceToFile(Class<?> clazz, String directory, String name) {
		
		return null;
	}



	// from https://bukkit.org/threads/extracting-file-from-jar.16962/
	public void copyToFile(InputStream in, File file) throws IOException {
		InputStream fis = in;
		FileOutputStream fos = new FileOutputStream(file);
		try {
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}
	
	
	
	private Path getResourcePath(Class<?> theClass, String relPath) {
		URI uri = null;
		try {
			uri = theClass.getResource(relPath).toURI();
			//IJ.log("uri = " + uri.getPath().toString());
		} catch (Exception e) {
			System.err.println(e);
			return null;
		}
		Path resourcePath = null;
		String scheme = uri.getScheme();
		
		switch (scheme) {
		case "file": {	// resource in ordinary file system
			resourcePath = Paths.get(uri);
			break;
		}
		case "jar":	{	// resource inside a JAR file
			FileSystem fs = null;
			try {
				// wilbur: check if this FileSystem already exists (can't create twice!)
				fs = FileSystems.getFileSystem(uri);
			} catch (Exception e) {}

			if (fs == null) {	// FileSystem does not yet exist in this runtime
				try {
					fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
				} catch (IOException e) { }
			}
			
			if (fs == null) {	// FileSystem could not be created for some reason
				return null;
			}
			String ssp = uri.getSchemeSpecificPart();
			int start = ssp.lastIndexOf('!');
			String inJarPath = ssp.substring(start + 1);  // remove leading part including the last '!'
			//System.out.println("[listResources] inJarPath = "  + inJarPath);
			resourcePath = fs.getPath(inJarPath);
			break;
		}
		default:
			throw new IllegalArgumentException("Cannot handle this path type: " + scheme);
		}
		return resourcePath;
	}
	
	
	private URI getResourceURI(Class<?> theClass, String relPath) {
		URI uri = null;
		try {
			uri = theClass.getResource(relPath).toURI();
			//IJ.log("uri = " + uri.getPath().toString());
		} catch (Exception e) {
			System.err.println(e);
			return null;
		}
		//Path resourcePath = null;
		String scheme = uri.getScheme();
		
		switch (scheme) {
		case "file": {	// resource in ordinary file system
			//resourcePath = Paths.get(uri);
			return uri;
			//break;
		}
		case "jar":	{	// resource inside a JAR file
			FileSystem fs = null;
			try {
				// wilbur: check if this FileSystem already exists (can't create twice!)
				fs = FileSystems.getFileSystem(uri);
			} catch (Exception e) {}

			if (fs == null) {	// FileSystem does not yet exist in this runtime
				try {
					fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
				} catch (IOException e) { }
			}
			
			if (fs == null) {	// FileSystem could not be created for some reason
				return null;
			}
			String ssp = uri.getSchemeSpecificPart();
			int start = ssp.lastIndexOf('!');
			String inJarPath = ssp.substring(start + 1);  // remove leading part including the last '!'
			//System.out.println("[listResources] inJarPath = "  + inJarPath);
			//resourcePath = fs.getPath(inJarPath);
			return fs.getPath(inJarPath).toUri();
			//break;
		}
		default:
			throw new IllegalArgumentException("Cannot handle this path type: " + scheme);
		}
	}

}
