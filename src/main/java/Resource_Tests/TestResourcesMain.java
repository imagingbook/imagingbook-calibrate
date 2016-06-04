package Resource_Tests;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import imagingbook.lib.util.ResourceUtils;
import jarWithResouces.Root;

public class TestResourcesMain {

	static Class<?> resourceRootClass = Root.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	public static void main (String[] a) throws URISyntaxException {
		{	// resource in file system
			URI uri = ResourceUtils.getResourceUri(TestResourcesMain.class, resourceDir);	
			System.out.println("uri = " + uri);
			Path[] paths = ResourceUtils.listResources(uri);
			for (Path p : paths) {
				System.out.println(" - " + p.toUri().toString());
			}
		}
		
		{	// resource inside JAR
			URI uri = ResourceUtils.getResourceUri(resourceRootClass, resourceDir);		
			System.out.println("uri = " + uri);
			Path[] paths = ResourceUtils.listResources(uri);
			for (Path p : paths) {
				System.out.println(" - " + p.toUri().toString());
			}
		}
	}
}



