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
     * Parabolic function over x (with horizontal axis):
     * {@code y = f(x) = a (x - d)^2 + c}
     */
    public static class ParabolaX extends Parabola {

        public ParabolaX(double a, double c, double d) {
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

        /**
         * Find the intersection between this parabola (over x) with another
         * parabola over y).
         * @param parY the other parabola
         * @param xStart initial guess for x-intersection
         * @param yStart initial guess for y-intersection
         * @return the intersection as a {@link Pnt2d} instance
         */
        public Pnt2d getIntersection(ParabolaY parY, double xStart, double yStart) {
            return Pnt2d.from(new ParabolaIntersector().getIntersection(this, parY, xStart, yStart));
        }
    }

    // ---------------------------------------------------------------------

    /**
     * Parabolic function over y (with horizontal axis):
     * {@code x = f(y) = a (y - d)^2 + c}
     */
    public static class ParabolaY extends Parabola {

        public ParabolaY(double a, double c, double d) {
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

        /**
         * Find the intersection between this parabola (over y) with another
         * parabola over x).
         * @param parX the other parabola
         * @param xStart initial guess for x-intersection
         * @param yStart initial guess for y-intersection
         * @return the intersection as a {@link Pnt2d} instance
         */
        public Pnt2d getIntersection(ParabolaX parX, double xStart, double yStart) {
            return Pnt2d.from(new ParabolaIntersector().getIntersection(parX, this, xStart, yStart));
        }
    }

    // ---------------------------------------------------------------------


}
