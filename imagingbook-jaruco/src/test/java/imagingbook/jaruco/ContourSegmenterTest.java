package imagingbook.jaruco;

import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.common.geometry.basic.Polygon2d;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContourSegmenterTest {

    @Test
    void extractQuadTest() {
    }

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
        // System.out.println("corners = " + new Polygon2d(PntUtils.makePntList(corners)));

        SegmentedPolygon segCtr = new ContourSegmenter(polygonalApproxAccuracyRate) // tol = contour.length * polygonalApproxAccuracyRate
                .segment(new Polygon2d(PntUtils.makePntList(contour)));
        // System.out.println("cornersSeg = " + segCtr.getCornerPolygon());

        // check if 4 corners exactly
        assertEquals(4, segCtr.getSegmentCount());

        // check if all corner points are the same
        Polygon2d cornerPoly1 = new Polygon2d(PntUtils.makePntList(corners));
        Polygon2d cornerPoly2 = segCtr.getCornerPolygon();
        assertEquals(4, cornerPoly2.length());
        assertEquals(cornerPoly1, cornerPoly2);

        // check if the segment's point count adds up to the contour's point count
        int pntCnt = 0;
        for (int k = 0; k < segCtr.getSegmentCount(); k++) {
            pntCnt += segCtr.getSegment(k).length;
        }
        assertEquals(contour.length, pntCnt);
    }

}