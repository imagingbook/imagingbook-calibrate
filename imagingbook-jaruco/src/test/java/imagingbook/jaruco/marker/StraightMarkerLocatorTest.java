package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
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
import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StraightMarkerLocatorTest {

    static final double polygonalApproxAccuracyRate = 0.03;  // from ArUco parameters

    @Test
    void fitRealContourTest() {
        runSegmentTest(single_marker_5_0_contour.getPoints(), single_marker_5_0_corners.getPoints());
        runSegmentTest(single_marker_5_1_contour.getPoints(), single_marker_5_1_corners.getPoints());
        runSegmentTest(single_marker_5_2_contour.getPoints(), single_marker_5_2_corners.getPoints());
        runSegmentTest(single_marker_5_3_contour.getPoints(), single_marker_5_3_corners.getPoints());
    }

    static void runSegmentTest(Pnt2d[] contour, Pnt2d[] corners) {
        assertNotNull(contour);
        assertNotNull(corners);
        SegmentedPolygon segCtr = new ContourSegmenter(polygonalApproxAccuracyRate)
                .segment(new Polygon2d(contour)); // tol = contour.length * polygonalApproxAccuracyRate
        assertEquals(4, segCtr.getSegmentCount());
    }

    // void runFitTest(String contourPath, String cornersPath) {
    //     double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
    //     double[][] corners = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
    //     assertNotNull(contour);
    //     assertNotNull(corners);
    //
    //     SegmentedPolygon segCtr = new ContourSegmenter(polygonalApproxAccuracyRate)
    //             .segment(PntUtils.makePntList(contour)); // tol = contour.length * polygonalApproxAccuracyRate
    //     assertEquals(4, segCtr.getSegmentCount());
    //
    //     // QuadHomographyFit fit = new QuadHomographyFit(segCtr);
    //     // double[][] A = fit.getTransformationMatrix();
    //     // assertNotNull(A);
    //     // PrintPrecision.set(8);
    //     // System.out.println("A quadFit = \n" + Matrix.toString(A));
    //     // System.out.println("A quadFit error = " + fit.getError());
    //
    //     // -------------------------------------------
    //
    //     // ProjectiveFit2d fitP = new ProjectiveFit2d(segCtr.getCorners().toArray(new Pnt2d[0]), UnitSquare.toArray(new Pnt2d[0]));
    //     // System.out.println("A projective = \n" + Matrix.toString(fitP.getTransformationMatrix()));
    //     // System.out.println("A projective error = " + fitP.getError());
    //
    // }

    @Test
    void getCorners() {
    }

    @Test
    void getError() {
    }
}