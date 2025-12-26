package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.jaruco.math.Parabola;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParabolicMarkerLocatorTest {

    @Test
    void fitOverXYTest() {
        // fit parabola over X:
        List<Pnt2d> polyX = PntUtils.makePntList(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
        Parabola.ParabolaX fitX = ParabolicMarkerLocator.fitOverX(polyX, 0.5);
        // System.out.println("fitX = " + fitX);
        assertEquals(-3.32417317, fitX.a, 1e-6);
        assertEquals( 0.88380459, fitX.c, 1e-6);

        // fit same parabola over Y:
        // List<Pnt2d> polyY = PntUtils.makePntList(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
        double[][] XY = PntUtils.toXYArray(polyX);
        List<Pnt2d> polyY = PntUtils.fromXYArray(new double[][] {XY[1], XY[0]});    // swap X/Y
        Parabola.ParabolaY fitY = ParabolicMarkerLocator.fitOverY(polyY, 0.5);
        // System.out.println("fitY = " + fitY);
        assertEquals(fitX.a, fitY.a, 1e-6);
        assertEquals(fitX.c, fitY.c, 1e-6);
    }

}