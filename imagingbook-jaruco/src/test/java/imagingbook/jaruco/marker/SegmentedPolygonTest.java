package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.common.geometry.basic.Polygon2d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentedPolygonTest {

    @Test
    void cornersOnlyTest() {
        List<Pnt2d> contour = PntUtils.makePntList(10, 7,  23, 1,  19, 21,  12, 15);
        //List<Integer> corners = List.of(0, 1, 2, 3);
        SegmentedPolygon segCtr = new SegmentedPolygon(new Polygon2d(contour), List.of(0, 1, 2, 3));
        // must have 4 segments/corners
        assertEquals(4, segCtr.getSegmentCount());
        // each segment has length 1 and contains a corner
        for (int k = 0; k < segCtr.getSegmentCount(); k++) {
            assertEquals(1, segCtr.getSegmentPoints(k).length);
            assertSame(segCtr.getCornerPoint(k), segCtr.getSegmentPoints(k)[0]);
        }
    }

    @Test
    void lengthTest() {
    }

    @Test
    void getCornerPointTest() {
    }

    @Test
    void getCornerPointPointsTest() {
    }

    @Test
    void getSegmentPointsTest() {
    }
}