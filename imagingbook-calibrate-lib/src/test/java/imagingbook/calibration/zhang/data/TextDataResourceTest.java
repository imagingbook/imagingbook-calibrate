/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.data;

import imagingbook.core.resource.NamedResource;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TextDataResourceTest {

	@Test
	public void test1() {
		for (NamedResource nr : TextDataResource.values()) {
			assertNotNull("could not find resource " + nr.toString(), nr.getURL());
		}
	}

}
