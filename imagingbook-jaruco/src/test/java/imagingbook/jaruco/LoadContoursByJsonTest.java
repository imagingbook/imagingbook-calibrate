package imagingbook.jaruco;

import org.junit.Test;

import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.*;

public class LoadContoursByJsonTest {

    // Contours are too big to be defined in Java code, therefore external JSON files.
    // sample contours must be in
    // \imagingbook-calibrate\imagingbook-jaruco\src\test\resources\test-contours
    static String REL_RESOURCE_PATH = "test-contours/";

    @Test
    public void testLoadContours() {
        doArrayLoadingTest(this.getClass(), "single-marker-5-0-contour.json", 816, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-1-contour.json", 820, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-2-contour.json", 814, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-3-contour.json", 816, 2);
    }

    @Test
    public void testLoadCorners() {
        doArrayLoadingTest(this.getClass(), "single-marker-5-0-corners.json", 4, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-1-corners.json", 4, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-2-corners.json", 4, 2);
        doArrayLoadingTest(this.getClass(), "single-marker-5-3-corners.json", 4, 2);
    }

    private void doArrayLoadingTest(Class<?> clazz, String resourceName, int expectedRows, int expectedCols) {
        double[][] matrix = (double[][]) loadObject(clazz, REL_RESOURCE_PATH + resourceName, double[][].class);
        assertEquals(expectedRows, matrix.length, "trouble with " + resourceName);
        assertEquals(expectedCols, matrix[0].length);
    }

}

// loadObject(Class<?> resourceClass, String resourcePath, Class<?> objectClass)