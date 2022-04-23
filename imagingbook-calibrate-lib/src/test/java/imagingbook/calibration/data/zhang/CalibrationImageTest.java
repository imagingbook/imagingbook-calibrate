package imagingbook.calibration.data.zhang;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import imagingbook.core.resource.ImageResource;

public class CalibrationImageTest {

	@Test
	public void test1() {
		for (ImageResource ir : CalibrationImage.values()) {
			assertNotNull("could not open ipage for resource " + ir.toString(), ir.getImage());
//			System.out.println(ir.getImage().toString());
		}
	}

}
