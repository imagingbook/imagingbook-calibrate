package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.math.Parabola;
import imagingbook.jaruco.util.Polygons;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitParabolicMarkerLocatorTest {

    @Test
    void fitOverXYTest() {
        // fit parabola over X:
        List<Pnt2d> polyX = Polygons.makePolygon(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
        Parabola.ParabolaX[] fitX = SplitParabolicMarkerLocator.fitOverX(polyX, 0.5);
        Parabola L0 = fitX[0];
        Parabola R0 = fitX[1];

        assertEquals(-3.0924045398152815, L0.a, 1e-6);
        assertEquals( 0.8736732068189127, L0.c, 1e-6);
        assertEquals( 0.5, L0.d, 1e-6);

        assertEquals(-3.494692827275651, R0.a, 1e-6);
        assertEquals( L0.c, R0.c, 1e-6);
        assertEquals( L0.d, R0.d, 1e-6);

        // fit same parabola over Y:
        // List<Pnt2d> polyY = Polygons.makePolygon(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
        double[][] XY = Polygons.toXYArray(polyX);
        List<Pnt2d> polyY = Polygons.fromXYArray(new double[][] {XY[1], XY[0]});    // swap X/Y
        Parabola.ParabolaY fitY[] = SplitParabolicMarkerLocator.fitOverY(polyY, 0.5);

        Parabola L1 = fitY[0];
        Parabola R1 = fitY[1];

        assertEquals(L0.a, L1.a, 1e-6);
        assertEquals(L0.c, L1.c, 1e-6);
        assertEquals(L0.d, L1.d, 1e-6);

        assertEquals(R0.a, R1.a, 1e-6);
        assertEquals(R0.c, R1.c, 1e-6);
        assertEquals(R0.d, R1.d, 1e-6);
    }


    @Test
    void getCorners() {
    }

    @Test
    void fitOverX() {
    }

    @Test
    void fitOverY() {
    }
}