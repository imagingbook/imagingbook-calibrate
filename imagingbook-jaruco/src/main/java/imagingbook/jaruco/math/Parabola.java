package imagingbook.jaruco.math;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PolyLine2d;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
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
            return Pnt2d.from(new Intersector().getIntersection(this, parY, xStart, yStart));
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
            return Pnt2d.from(new Intersector().getIntersection(parX, this, xStart, yStart));
        }
    }

    // ---------------------------------------------------------------------


    /**
     * https://chatgpt.com/share/694448e5-c3d4-8006-b5ed-ca6bf915b13c
     */
    public static class Intersector {

        static double DEFAULT_TOL = 1e-6;
        static int DEFAULT_MAX_ITER = 15;

        private final double tol;
        private final int maxIter;

        public Intersector() {
            this(DEFAULT_TOL, DEFAULT_MAX_ITER);
        }

        public Intersector(double tol, int maxIter) {
            this.tol = tol;
            this.maxIter = maxIter;
        }

        // ----------------------------------------------------------------------------

        /**
         *
         * @param P0 parabola with vertical axis:   y = a0 (x - d0)^2 + c0
         * @param P1 parabola with horizontal axis: x = a1 (y - d1)^2 + c1
         * @param xStart x-start coordinate for intersection
         * @param yStart y-start coordinate for intersection
         * @return the intersection point
         */
        public double[] getIntersection (ParabolaX P0, ParabolaY P1, double xStart, double yStart) {
            double x = xStart;
            double y = yStart;
            int iterations = 0;

            for (int k = 0; k < maxIter; k++) {
                iterations++;
                // System.out.printf("x,y = %.6f, %.6f\n", x, y);

                // Residual vector F
                double r0 = x - P1.getXvalue(y); // = f0(x, y)
                double r1 = y - P0.getYvalue(x); // = f1(x, y);

                // Convergence test (residual-based)
                double residual = Math.hypot(r0, r1);
                // System.out.printf("residual = %.6f\n", residual);
                if (residual < tol) {
                    // System.out.println("iterations needed = " +  iterations);
                    return new double[]{x, y};
                }

                double[][] JJ = {
                    { 1, -P1.getDeriv(y) },
                    { -P0.getDeriv(x), 1 }
                };

                RealMatrix J = new Array2DRowRealMatrix(JJ);
                RealVector F = new ArrayRealVector(new double[] {r0, r1});
                // Solve J * delta = F
                RealVector delta = new LUDecomposition(J).getSolver().solve(F);
                // Newton update
                x -= delta.getEntry(0);
                y -= delta.getEntry(1);
            }

            // System.out.println("iterations: " + iterations);
            throw new RuntimeException("max. number of iterations exceeded: " + maxIter);
        }

        // --------------------------------------------------------------

        public static void main(String[] args) {
            ParabolaX P0 = new ParabolaX(-0.1, 0.02, 0.5);
            ParabolaY P1 = new ParabolaY(0.1, 0.03 + 1, 0.5);
            double x0 = 1, y0 = 0;

            double[] X = new Intersector().getIntersection(P0, P1, x0, y0);
            System.out.println("Intersection at " + Arrays.toString(X));
            // try also imagingbook.common.math.nonlinear.solveGaussNewton()
        }

    }
}
