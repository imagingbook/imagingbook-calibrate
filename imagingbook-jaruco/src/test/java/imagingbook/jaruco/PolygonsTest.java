package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static imagingbook.jaruco.Polygons.checkSame;
import static imagingbook.jaruco.Polygons.getArea;
import static imagingbook.jaruco.Polygons.circularity;
import static imagingbook.jaruco.Polygons.getCentroid;
import static imagingbook.jaruco.Polygons.getLength;
import static imagingbook.jaruco.Polygons.getMostEccentricVertexIndex;
import static imagingbook.jaruco.Polygons.convexity;
import static imagingbook.jaruco.Polygons.makePolygon;
import static imagingbook.jaruco.Polygons.perpDist;
import static imagingbook.jaruco.Polygons.perpDistSq;
import static imagingbook.jaruco.Polygons.simplify;
import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static imagingbook.jaruco.util.ListUtils.reversedCopy;
import static org.junit.jupiter.api.Assertions.*;

class PolygonsTest {

    static List<Pnt2d> unitSquareCW = makePolygon(0, 0, 0, 1, 1, 1, 1, 0);
    static List<Pnt2d> unitSquareCCW = reversedCopy(unitSquareCW);
    static List<Pnt2d> triangleCCW  = makePolygon(-2, -1, 4, 3, -1, 5);
    static List<Pnt2d> triangleCW  = reversedCopy(triangleCCW);

        @Test
    void convexityTest1() {
        assertEquals(-1, convexity(unitSquareCW));
        assertEquals( 1, convexity(unitSquareCCW));

        assertEquals(1, convexity(triangleCCW));
        assertEquals(-1, convexity(triangleCW));
    }

    @Test
    void getLengthTest() {
        assertEquals(4.0, getLength(unitSquareCW));
        assertEquals(4.0, getLength(unitSquareCCW));

        assertEquals(18.67902988, getLength(triangleCW), 1e-6);
        assertEquals(18.67902988, getLength(triangleCCW), 1e-6);
    }

    @Test
    void getAreaTest() {
        assertEquals(1.0, getArea(unitSquareCW));
        assertEquals(1.0, getArea(unitSquareCCW));
    }

    @Test
    void circularityTest() {
        assertEquals(0.78539816, circularity(unitSquareCW), 1e-6);
        assertEquals(0.78539816, circularity(unitSquareCCW), 1e-6);

        assertEquals(0.57626363, circularity(triangleCW), 1e-6);
        assertEquals(0.57626363, circularity(triangleCCW), 1e-6);
    }

    @Test
    void perpDistSqTest() {
        Pnt2d A = Pnt2d.from(2, -5);
        Pnt2d B = Pnt2d.from(17, 1);
        Pnt2d P = Pnt2d.from(-3, 2);
        double expected = 74.0;
        assertEquals(expected, perpDistSq(P, A, B), 1e-6);
        assertEquals(expected, perpDistSq(P, B, A), 1e-6);
    }

    @Test
    void perpDistTest() {
        Pnt2d A = Pnt2d.from(-2, 5);
        Pnt2d B = Pnt2d.from(17, 1);
        Pnt2d P = Pnt2d.from(1, 0);
        double expected = 4.27471748;
        assertEquals(expected, perpDist(P, A, B), 1e-6);
        assertEquals(expected, perpDist(P, B, A), 1e-6);
    }

    @Test
    void testGetMostEccentricVertexIndexTest() {
        assertEquals(2, getMostEccentricVertexIndex(triangleCCW));
        assertEquals(0, getMostEccentricVertexIndex(triangleCW));
    }

    @Test
    void checkSameTest() {
        assertTrue(checkSame(triangleCCW, reversedCopy(triangleCW)));
        assertTrue(checkSame(triangleCW, reversedCopy(triangleCCW)));

        assertFalse(checkSame(triangleCCW, triangleCW));
        assertFalse(checkSame(triangleCW, triangleCCW));

        assertFalse(checkSame(triangleCCW, unitSquareCCW));
        assertFalse(checkSame(triangleCW, unitSquareCW));
    }

    @Test
    void getCentroidTest() {
        double[] expected = {1.0/3, 7.0/3};
        double[] ctr1 = getCentroid(triangleCCW).toDoubleArray();
        double[] ctr2 = getCentroid(triangleCW).toDoubleArray();
        assertArrayEquals(expected, ctr1, 1e-6);
        assertArrayEquals(expected, ctr2, 1e-6);
    }

    @Test
    void testSimplifyTest() {
        String RES_PATH = "test-contours/";     // these are long contours!
        runSimplifyTest(RES_PATH + "single-marker-5-0-contour.json", RES_PATH + "single-marker-5-0-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-1-contour.json", RES_PATH + "single-marker-5-1-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-2-contour.json", RES_PATH + "single-marker-5-2-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-3-contour.json", RES_PATH + "single-marker-5-3-corners.json");
    }

    static final double polygonalApproxAccuracyRate =  0.03;  // from ArUco parameters

    void runSimplifyTest(String contourPath, String cornersPath) {
        double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
        double[][] corners1 = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
        List<Pnt2d> cornerList2 = simplify(makePolygon(contour), contour.length * polygonalApproxAccuracyRate);
        // System.out.println("corners1 = " + Polygons.toString(makePolygon(corners1)));
        System.out.println("corners2 = " + Polygons.toString(cornerList2));
        assertTrue(checkSame(makePolygon(corners1), cornerList2));
    }


}