package imagingbook.jaruco.math;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Represents a function of type
 * {@code outVal = a * (inVal - d)^2 + c }.
 */
public abstract class Parabola {

    public final double a;
    public final double c;
    public final double d;

    private Parabola(double a, double c, double d) {
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

    public String toString() {
        return String.format(Locale.US, "%s [%.4f, %.4f, %.4f]", this.getClass().getCanonicalName(), a, c, d);
    }

    /**
     * Sample the curve in regular steps and return a sequence of xy-points
     * @param from
     * @param to
     * @param steps
     * @return
     */
    public abstract List<Pnt2d> sample(double from, double to, int steps);

    // ---------------------------------------------------------------------

    /**
     * Parabola with vertical axis:   y = a0 (x - d0)^2 + c0
     */
    public static class OverX extends Parabola {

        public OverX(double a, double c, double d) {
            super(a, c, d);
        }

        @Override
        public List<Pnt2d> sample(double fromX, double toX, int steps) {
            int n = steps + 1;
            List<Pnt2d> pts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double x = fromX + i * (toX - fromX) / n;
                pts.add(Pnt2d.from(x, getVal(x)));
            }
            return pts;
        }

        public double getYvalue(double x) {
            return this.getVal(x);
        }

        public double[] getIntersection(Parabola.OverY other,  double xStart, double yStart) {
            return new ParabolaIntersector().getIntersection(this, other, xStart, yStart);
        }
    }

    /**
     * Parabola with horizontal axis: x = a1 (y - d1)^2 + c1
     */
    public static class OverY extends Parabola {

        public OverY(double a, double c, double d) {
            super(a, c, d);
        }

        @Override
        public List<Pnt2d> sample(double fromY, double toY, int steps) {
            int n = steps + 1;
            List<Pnt2d> pts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double y = fromY + i * (toY - fromY) / n;
                pts.add(Pnt2d.from(getVal(y), y));
            }
            return pts;
        }

        public double getXvalue(double y) {
            return this.getVal(y);
        }

        public double[] getIntersection(Parabola.OverX other,  double xStart, double yStart) {
            return new ParabolaIntersector().getIntersection(other, this, xStart, yStart);
        }
    }

    // ---------------------------------------------------------------------


}
