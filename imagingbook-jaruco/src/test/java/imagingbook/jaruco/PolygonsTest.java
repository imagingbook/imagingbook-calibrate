package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.util.ListUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static imagingbook.jaruco.Polygons.checkSame;
import static imagingbook.jaruco.Polygons.getArea;
import static imagingbook.jaruco.Polygons.getCircularity;
import static imagingbook.jaruco.Polygons.getLength;
import static imagingbook.jaruco.Polygons.getMostEccentricVertexIndex;
import static imagingbook.jaruco.Polygons.isConvex;
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
    void isConvexTest1() {
        assertEquals(-1, isConvex(unitSquareCW));
        assertEquals( 1, isConvex(unitSquareCCW));

        assertEquals(1, isConvex(triangleCCW));
        assertEquals(-1, isConvex(triangleCW));
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
    void getCircularityTest() {
        assertEquals(0.78539816, getCircularity(unitSquareCW), 1e-6);
        assertEquals(0.78539816, getCircularity(unitSquareCCW), 1e-6);

        assertEquals(0.57626363, getCircularity(triangleCW), 1e-6);
        assertEquals(0.57626363, getCircularity(triangleCCW), 1e-6);
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