package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_0_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_0_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_1_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_1_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_2_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_2_corners;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_3_contour;
import static imagingbook.jaruco.marker.MarkerTestContours.single_marker_5_3_corners;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContourSegmenterTest {

    static final double polygonalApproxAccuracyRate = 0.03;  // from ArUco parameters

    @Test
    void segmentTest() {
        runSegmentTest(single_marker_5_0_contour.getPoints(), single_marker_5_0_corners.getPoints());
        runSegmentTest(single_marker_5_1_contour.getPoints(), single_marker_5_1_corners.getPoints());
        runSegmentTest(single_marker_5_2_contour.getPoints(), single_marker_5_2_corners.getPoints());
        runSegmentTest(single_marker_5_3_contour.getPoints(), single_marker_5_3_corners.getPoints());
    }

    // Checks if corners of segmented contours are as expected
    static void runSegmentTest(Pnt2d[] contour, Pnt2d[] corners) {
        SegmentedPolygon segCtr = new ContourSegmenter(polygonalApproxAccuracyRate) // tol = contour.length * polygonalApproxAccuracyRate
                .segment(new Polygon2d(contour));

        // check if 4 corners exactly
        assertEquals(4, segCtr.getSegmentCount());

        // check if all corner points are the same
        Polygon2d cornerPoly1 = new Polygon2d(corners);
        Polygon2d cornerPoly2 = new Polygon2d(segCtr.getCorners());
        assertEquals(4, cornerPoly2.length());
        assertEquals(cornerPoly1, cornerPoly2);

        // check if the segment's point count adds up to the contour's point count
        int pntCnt = 0;
        for (int k = 0; k < segCtr.getSegmentCount(); k++) {
            pntCnt += segCtr.getSegmentPoints(k).length;
        }
        assertEquals(contour.length, pntCnt);
    }

}