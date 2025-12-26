package imagingbook.jaruco;

import imagingbook.common.geometry.basic.PntUtils;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StraightLineMarkerLocatorTest {

    static final double polygonalApproxAccuracyRate = 0.03;  // from ArUco parameters

    @Test
    void fitRealContourTest() {
        String RES_PATH = "test-contours/";     // these are long contours!
        runFitTest(RES_PATH + "single-marker-5-0-contour.json", RES_PATH + "single-marker-5-0-corners.json");
        runFitTest(RES_PATH + "single-marker-5-1-contour.json", RES_PATH + "single-marker-5-1-corners.json");
        runFitTest(RES_PATH + "single-marker-5-2-contour.json", RES_PATH + "single-marker-5-2-corners.json");
        runFitTest(RES_PATH + "single-marker-5-3-contour.json", RES_PATH + "single-marker-5-3-corners.json");
    }

    void runFitTest(String contourPath, String cornersPath) {
        double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
        double[][] corners = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
        assertNotNull(contour);
        assertNotNull(corners);

        SegmentedPolygon segCtr = new ContourSegmenter(polygonalApproxAccuracyRate)
                .segment(PntUtils.makePntList(contour)); // tol = contour.length * polygonalApproxAccuracyRate
        assertEquals(4, segCtr.getSegmentCount());

        // QuadHomographyFit fit = new QuadHomographyFit(segCtr);
        // double[][] A = fit.getTransformationMatrix();
        // assertNotNull(A);
        // PrintPrecision.set(8);
        // System.out.println("A quadFit = \n" + Matrix.toString(A));
        // System.out.println("A quadFit error = " + fit.getError());

        // -------------------------------------------

        // ProjectiveFit2d fitP = new ProjectiveFit2d(segCtr.getCorners().toArray(new Pnt2d[0]), UnitSquare.toArray(new Pnt2d[0]));
        // System.out.println("A projective = \n" + Matrix.toString(fitP.getTransformationMatrix()));
        // System.out.println("A projective error = " + fitP.getError());

    }

    @Test
    void getCorners() {
    }

    @Test
    void getError() {
    }
}