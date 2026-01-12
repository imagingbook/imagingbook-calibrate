package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_0_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_0_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_1_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_1_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_2_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_2_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_3_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_3_corners;
import static org.junit.jupiter.api.Assertions.*;

class MarkerTestContoursTest {

    @Test
    void getPointsTest() {
        for (var item : MarkerTestContours.values()) {
            Pnt2d[] corners = item.getPoints();
            assertNotNull(corners);
            assertTrue(corners.length > 0);
        }
    }

    @Test
    public void testLoadContours() {
        assertEquals(816, single_marker_5_0_contour.getPoints().length);
        assertEquals(820, single_marker_5_1_contour.getPoints().length);
        assertEquals(814, single_marker_5_2_contour.getPoints().length);
        assertEquals(816, single_marker_5_3_contour.getPoints().length);
    }

    @Test
    public void testLoadCorners() {
        assertEquals(4, single_marker_5_0_corners.getPoints().length);
        assertEquals(4, single_marker_5_1_corners.getPoints().length);
        assertEquals(4, single_marker_5_2_corners.getPoints().length);
        assertEquals(4, single_marker_5_3_corners.getPoints().length);
    }
}