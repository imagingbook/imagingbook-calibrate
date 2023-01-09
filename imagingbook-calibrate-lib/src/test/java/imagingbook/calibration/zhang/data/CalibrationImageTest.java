/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.data;

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
