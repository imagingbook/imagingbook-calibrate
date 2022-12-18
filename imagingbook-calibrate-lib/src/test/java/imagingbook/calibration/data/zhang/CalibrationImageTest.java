package imagingbook.calibration.data.zhang;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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
