package imagingbook.calibration.data.zhang;

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
