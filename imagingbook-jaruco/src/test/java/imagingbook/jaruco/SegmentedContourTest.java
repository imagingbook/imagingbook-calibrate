package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.util.Polygons;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentedContourTest {

    @Test
    void cornersOnlyTest() {
        List<Pnt2d> contour = Polygons.makePolygon(10, 7,  23, 1,  19, 21,  12, 15);
        //List<Integer> corners = List.of(0, 1, 2, 3);
        SegmentedContour segCtr = new SegmentedContour(List.of(0, 1, 2, 3), contour);
        // must have 4 segments/corners
        assertEquals(4, segCtr.getSegmentCount());
        // each segment has length 1 and contains a corner
        for (int k = 0; k < segCtr.getSegmentCount(); k++) {
            assertEquals(1, segCtr.getSegment(k).length);
            assertSame(segCtr.getCorner(k), segCtr.getSegment(k)[0]);
        }
    }

    @Test
    void lengthTest() {
    }

    @Test
    void getCornerTest() {
    }

    @Test
    void getCornersTest() {
    }

    @Test
    void getSegmentTest() {
    }
}