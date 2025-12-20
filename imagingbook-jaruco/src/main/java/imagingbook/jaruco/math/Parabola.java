package imagingbook.jaruco.math;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Represents a function of type
 * {@code outVal = a * (inVal - d)^2 + c }.
 */
public class Parabola {

    public final double a;
    public final double c;
    public final double d;

    public Parabola(double a, double c, double d) {
        this.a = a;
        this.c = c;
        this.d = d;
    }

    public double getVal(double inVal) {
        return a * sqr(inVal - d) + c;
    }

    public double getDeriv(double inVal) {
        return 2.0 * a * (inVal - d);
    }

    /**
     * Parabola with vertical axis:   y = a0 (x - d0)^2 + c0
     */
    public static class OverX extends Parabola {

        public OverX(double a, double c, double d) {
            super(a, c, d);
        }

        public double getYvalue(double x) {
            return this.getVal(x);
        }
    }

    /**
     * Parabola with horizontal axis: x = a1 (y - d1)^2 + c1
     */
    public static class OverY extends Parabola {

        public OverY(double a, double c, double d) {
            super(a, c, d);
        }

        public double getXvalue(double y) {
            return this.getVal(y);
        }

    }


}
