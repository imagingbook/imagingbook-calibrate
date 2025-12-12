package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static imagingbook.jaruco.Polygons.checkSame;
import static imagingbook.jaruco.Polygons.getArea;
import static imagingbook.jaruco.Polygons.circularity;
import static imagingbook.jaruco.Polygons.getLength;
import static imagingbook.jaruco.Polygons.getMostEccentricVertexIndex;
import static imagingbook.jaruco.Polygons.convexity;
import static imagingbook.jaruco.Polygons.makePolygon;
import static imagingbook.jaruco.util.ListUtils.reversedCopy;
import static org.junit.jupiter.api.Assertions.*;

class PolygonsTest {

    static List<Pnt2d> unitSquareCW = makePolygon(0, 0, 0, 1, 1, 1, 1, 0);
    static List<Pnt2d> unitSquareCCW = reversedCopy(unitSquareCW);
    static List<Pnt2d> triangleCCW  = makePolygon(-2, -1, 4, 3, -1, 5);
    static List<Pnt2d> triangleCW  = reversedCopy(triangleCCW);

    @Test
    void simplify() {
    }


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
}