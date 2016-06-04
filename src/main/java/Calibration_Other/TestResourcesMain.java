package Calibration_Other;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import jarWithResouces.Root;

public class TestResourcesMain {

	static Class<?> resourceRootClass = Root.class;
	static String resourceDir = "resources/";
	static String resourceName = "CalibImageStack.tif";

	public static void main (String[] a) throws URISyntaxException {
		TestResourcesMain m = new TestResourcesMain();

		//		m.test1();

		//URI uri = m.getResourceURI(TestResourcesMain.class, resourceDir);	// resource in file system
		URI uri = m.getResourceURI(resourceRootClass, resourceDir);		// resource in JAR
		System.out.println("uri = " + uri);
		Path[] paths = m.listResources(uri);
		for (Path p : paths) {
			System.out.println(" - " + p.toUri().toString());
		}
	}


	boolean isInJar(Class<?> clazz) {
		URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		String path = url.getPath();
		File file = new File(path);
		return file.isFile();
	}


	// --------------------------------------------------------------------------

	public URI getResourceURI(Class<?> clazz, String  relPath) {
		URI uri = null;
		if (isInJar(clazz)) {
//			System.out.println("JAR path");
			String classPath = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
//			System.out.println("classPath = " + classPath);
			String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
//			System.out.println("packagePath = " + packagePath);
			String compPath = "jar:file:" + classPath + "!/" + packagePath + "/" + relPath;
//			System.out.println("compPath = " + compPath);
			try {
				uri = new URI(compPath);
			} catch (URISyntaxException e) {
				throw new RuntimeException("getResourceURI: " + e.toString());
			}	
		}
		else {
			System.out.println("regular file path");
			try {
				uri = clazz.getResource(relPath).toURI();
			} catch (Exception e) {
				throw new RuntimeException("getResourceURI: " + e.toString());
			}
		}
		return uri;
	}
	
	public Path getResourcePath(Class<?> clazz, String  relPath) {
		URI uri = getResourceURI(clazz, relPath);
		if (uri != null) {
			return uriToPath(uri);
		}
		else {
			return null;
		}
	}
	
	private Path uriToPath(URI uri) {
		Path path = null;
		String scheme = uri.getScheme();
		switch (scheme) {
		case "file": {	// resource in ordinary file system
			path = Paths.get(uri);
			break;
		}
		case "jar":	{	// resource inside JAR file
			FileSystem fs = null;
			try { // check if this FileSystem already exists (can't create twice!)
				fs = FileSystems.getFileSystem(uri);
			} catch (FileSystemNotFoundException e) {
				// that's OK to happen, the file system is not created automatically
			}

			try {
				fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
			} catch (IOException e) {
				throw new RuntimeException("uriToPath: " + e.toString());
			}
			
			String ssp = uri.getSchemeSpecificPart();
			int startIdx = ssp.lastIndexOf('!');
			String inJarPath = ssp.substring(startIdx + 1);  // in-Jar path (after the last '!')
			path = fs.getPath(inJarPath);
			break;
		}
		default:
			throw new IllegalArgumentException("Cannot handle this URI type: " + scheme);
		}
		return path;
	}

	Path[] listResources(URI uri) {
		return listResources(uriToPath(uri));
	}
	
	Path[] listResources(Path path) {
		// with help from http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file, #10
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("path is not a directory: " + path.toString());
		}
		
		List<Path> pathList = new ArrayList<Path>();
		Stream<Path> walk = null;
		try {
			walk = Files.walk(path, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Iterator<Path> it = walk.iterator(); it.hasNext();){
			Path p = it.next();
			if (Files.isRegularFile(p) && Files.isReadable(p)) {
				pathList.add(p);
			}
		}
		walk.close();
		return pathList.toArray(new Path[0]);
	}

	// --------------------------------------------------------------------------

	void test1() throws URISyntaxException {
		URI uri1 = new URI("jar:file:/C:/PROJEC~2/parent/IM1D84~1/ImageJ/jars/jarWithResources.jar!/jarWithResouces/resources/clown.jpg");
		System.out.println("uri1 = " + uri1.toString());

		URI uri2 = new URI("jar:file:/C:/PROJEC~2/parent/IM1D84~1/ImageJ/jars/jarWithResources.jar!/jarWithResouces/resources/");
		System.out.println("uri2 = " + uri2.toString());

		FileSystem fs = null;
		// check if this FileSystem already exists (can't create twice!)
		try {
			fs = FileSystems.getFileSystem(uri2);
		} catch (FileSystemNotFoundException e) {
			// that's OK, the file system is not created automatically
		}

		try {
			fs = FileSystems.newFileSystem(uri2, Collections.<String, Object>emptyMap());
			System.out.println("created file system " + fs);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String ssp = uri2.getSchemeSpecificPart();
		int startIdx = ssp.lastIndexOf('!');
		String inJarPath = ssp.substring(startIdx + 1);  // in-Jar path (after the last '!')
		Path path = fs.getPath(inJarPath);
		System.out.println("path = " + path);
		System.out.println("uri = " + path.toUri());

		if (!Files.isDirectory(path)) {
			System.out.println("*** not a directory ***");
		}

		// with help from http://stackoverflow.com/questions/1429172/how-do-i-list-the-files-inside-a-jar-file, #10
		System.out.println("Listing directory:");
		Stream<Path> walk = null;
		try {
			walk = Files.walk(path, 1);
		} catch (IOException e) {
			e.printStackTrace();
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
				System.out.println(" - " + p.toUri().toString());
			}
		}
		walk.close();
	}

}



