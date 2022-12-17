package imagingbook.calibration.data.zhang;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import imagingbook.core.resource.ImageResource;

public class CalibrationImageTest {

	@Test
	public void test1() {
		for (CalibrationImage ir : CalibrationImage.values()) {
			// System.out.println(ir.getAbsolutePath());
			assertNotNull("could not find resource " + ir, ir.getURL());
			assertNotNull("could not open image for resource " + ir, ir.getImagePlus());
		}
	}

}
