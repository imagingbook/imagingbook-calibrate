package imagingbook.jaruco.math;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ParabolaTest {

    @Test
    void getValTest() {
        Parabola.ParabolaX P0 = new Parabola.ParabolaX(-0.1, 0.02, 0.5);
        Parabola.ParabolaY P1 = new Parabola.ParabolaY(0.1, 0.03 + 1, 0.5);
        double y0 = P0.getVal(0.7);
        // System.out.println("val0 = " + y0);
        assertEquals(0.016, y0, 1e-6);

        double x1 = P1.getVal(0.7);
        // System.out.println("val1 = " + x1);
        assertEquals(1.034, x1, 1e-6);
    }

    @Test
    void getDerivTest() {
        Parabola.ParabolaX P0 = new Parabola.ParabolaX(-0.1, 0.02, 0.5);
        Parabola.ParabolaY P1 = new Parabola.ParabolaY(0.1, 0.03 + 1, 0.5);
        double yy0 = P0.getDeriv(0.7);
        // System.out.println("val0 = " + yy0);
        assertEquals(-0.04, yy0, 1e-6);

        double xx1 = P1.getDeriv(0.7);
        // System.out.println("val1 = " + xx1);
        assertEquals(0.04, xx1, 1e-6);
    }

    @Test
    void sampleTest() {
    }

    @Test
    void intersectTest() {
        Parabola.ParabolaX P0 = new Parabola.ParabolaX(-0.1, 0.02, 0.5);
        Parabola.ParabolaY P1 = new Parabola.ParabolaY(0.1, 0.03 + 1, 0.5);
        double x0 = 1, y0 = 0;

        double[] X1 = P0.getIntersection(P1, x0, y0).toDoubleArray();
        // System.out.println("Intersection at " + Arrays.toString(X1));
        double[] Xexp = {1.0561044473921515, -0.010925215448726792};
        assertArrayEquals(Xexp, X1, 1e-6);

        double[] X2 = P1.getIntersection(P0, x0, y0).toDoubleArray();
        // System.out.println("Intersection at " + Arrays.toString(X2));
        assertArrayEquals(Xexp, X2, 1e-6);
    }

}