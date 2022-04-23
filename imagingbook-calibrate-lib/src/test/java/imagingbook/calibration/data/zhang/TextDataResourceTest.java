package imagingbook.calibration.data.zhang;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import imagingbook.core.resource.NamedResource;

public class TextDataResourceTest {

	@Test
	public void test1() {
		for (NamedResource nr : TextDataResource.values()) {
			assertNotNull("could not find resource " + nr.toString(), nr.getURL());
		}
	}

}
