package dummy;

import ij.ImagePlus;
import imagingbook.lib.util.resource.ResourceLocation;
import imagingbook.lib.util.resource.ResourceLocation.Resource;

public class ResourceTest {

	public static void main(String[] args) {
		ResourceLocation loc = new imagingbook.calibration.zhang.testdata.resources.Resources();
		System.out.println("Inside JAR: " + loc.insideJAR());
		System.out.println("root dir = " + loc.getURI());

		String[] names = loc.getResourceNames();
		for (String nam : names) {
			System.out.println(nam);
		}
		
		String nam = names[0]; // "CalibImageStack.tif";
		Resource res = loc.getResource(nam);
		System.out.println("\nResource name = " + res.getName());
		System.out.println("Resource URI = " + res.getURI());
		
		ImagePlus im = loc.getResource(nam).openAsImage();
		System.out.println("Opened image: " + im);
		im.close();
	}

}
