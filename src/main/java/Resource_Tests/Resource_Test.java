package Resource_Tests;

import java.net.URI;
import java.nio.file.Path;

import ij.IJ;
import ij.plugin.PlugIn;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.util.ResourceUtils;
import jarWithResouces.Root;	// test file: jarWithRousources.jar

/**
 * This plugin demonstrates the use of the resource access
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
	
	public void run(String arg0) {
		URI uri = ResourceUtils.getResourceUri(resourceRootClass, resourceDir);
		System.out.println("uri = " + uri);
		Path[] paths = ResourceUtils.listResources(uri);
		for (Path p : paths) {
			System.out.println(" - " + p.toUri().toString());
		}
	}

}
