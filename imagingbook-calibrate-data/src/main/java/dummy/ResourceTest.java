package dummy;

import java.nio.file.Path;

import ij.ImagePlus;
import imagingbook.lib.ij.IjUtils;
import imagingbook.lib.util.ResourceLocation;

public class ResourceTest {

	public static void main(String[] args) {
		ResourceLocation loc = new imagingbook.calibration.zhang.testdata.resources.Resources();
		System.out.println("Inside JAR: " + loc.isInsideJAR());

		String[] names = loc.getResourceNames();
		for (String nam : names) {
			System.out.println(nam);
		}
		
		String nam = "CalibImageStack.tif";
		Path p = loc.getResourcePath(nam);
		System.out.println("\nPath to " + nam + ": " + p);
		
		ImagePlus im = IjUtils.openImage(p);
		System.out.println("\nOpened image: " + im);
		im.close();
	}

}
