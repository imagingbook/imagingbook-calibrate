package imagingbook.jaruco;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnit5TestUtils {

    public static void assert2dArrayEquals(double[][] expected,
                                    double[][] actual,
                                    double delta) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], delta);
        }
    }

}
