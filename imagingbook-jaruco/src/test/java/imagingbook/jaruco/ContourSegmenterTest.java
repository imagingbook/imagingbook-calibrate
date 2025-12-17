package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static imagingbook.jaruco.util.Polygons.checkSame;
import static imagingbook.jaruco.util.Polygons.makePolygon;
import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.*;

class ContourSegmenterTest {

    @Test
    void extractQuadTest() {
    }

    // cIdx = [0, 199, 406, 607, 816]
    // cIdx = [0, 203, 411, 613, 820]
    // cIdx = [0, 197, 404, 605, 814]
    // cIdx = [0, 200, 407, 608, 816]

    @Test
    void segmentTest() {
        String RES_PATH = "test-contours/";     // these are long contours!
        runSegmentTest(RES_PATH + "single-marker-5-0-contour.json", RES_PATH + "single-marker-5-0-corners.json");
        runSegmentTest(RES_PATH + "single-marker-5-1-contour.json", RES_PATH + "single-marker-5-1-corners.json");
        runSegmentTest(RES_PATH + "single-marker-5-2-contour.json", RES_PATH + "single-marker-5-2-corners.json");
        runSegmentTest(RES_PATH + "single-marker-5-3-contour.json", RES_PATH + "single-marker-5-3-corners.json");
    }

    static final double polygonalApproxAccuracyRate =  0.03;  // from ArUco parameters

    // Checks if corners of segmented contours are as expected
    void runSegmentTest(String contourPath, String cornersPath) {
        double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
        double[][] corners = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
        SegmentedContour segCtr = new ContourSegmenter().segment(makePolygon(contour), contour.length * polygonalApproxAccuracyRate);

        // check if 4 corners exactly
        assertEquals(4, segCtr.getSegmentCount());

        // check all corner points
        List<Pnt2d> cornerList2 = segCtr.getCorners();
        assertTrue(checkSame(makePolygon(corners), cornerList2));

        // check if the segments' point count adds up to the contour's point count
        int pntCnt = 0;
        for (int k = 0; k < segCtr.getSegmentCount(); k++) {
            pntCnt += segCtr.getSegment(k).length;
        }
        assertEquals(contour.length, pntCnt);
    }

}