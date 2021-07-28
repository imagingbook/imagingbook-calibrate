package imagingbook.calibration.zhang.data;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ij.ImagePlus;
import imagingbook.lib.util.resource.ResourceLocation;
import imagingbook.lib.util.resource.ResourceLocation.Resource;

public class ResourceLoadingTest {
	
	private ResourceLocation loc = new imagingbook.calibration.zhang.data.files.RLOC();
	
	@Test
	public void testOpenImage() {
		assertNotNull(loc);
		String nam = "CalibImageStack.tif";
		Resource res = loc.getResource(nam);
		assertNotNull(res);
		
		ImagePlus im = loc.getResource(nam).openAsImage();
		assertNotNull(im);
		if (im != null)
			im.close();
	}
	
	@Test
	public void testOpenFile() {
		assertNotNull(loc);
		String nam = "all-calibration-results.txt";
		Resource res = loc.getResource(nam);
		assertNotNull(res);
		assertNotNull(res.getUri());
		assertNotNull(res.getStream());
	}

}
