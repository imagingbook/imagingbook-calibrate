package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;

/**
 * https://chatgpt.com/share/694448e5-c3d4-8006-b5ed-ca6bf915b13c
 */
public class ParabolaIntersectionFinder3 {

    static double DEFAULT_TOL = 1e-6;
    static int DEFAULT_MAX_ITER = 15;

    private final double tol;
    private final int maxIter;

    public ParabolaIntersectionFinder3() {
        this(DEFAULT_TOL, DEFAULT_MAX_ITER);
    }

    public ParabolaIntersectionFinder3(double tol, int maxIter) {
        this.tol = tol;
        this.maxIter = maxIter;
    }

    // --------------------------------------------------------

    private static double f0(QuadraticFunction Q1, double x, double y) {
        return x - Q1.getVal(y);
    }

    private static double f1(QuadraticFunction Q0, double x, double y) {
        return y - Q0.getVal(x);
    }

    // ----------------------------------------------------------------------------

    /**
     *
     * @param P0 parabola with vertical axis: y = a0 (x - d0)^2 + c0
     * @param P1 parabola with horizontal axis: x = a1 (y - d1)^2 + c1
     * @param x0 x-start coordinate for intersection
     * @param y0 y-start coordinate for intersection
     * @return the intersection point
     */
    public double[] getIntersection (QuadraticFunction P0, QuadraticFunction P1, double x0, double y0) {
        double x = x0;
        double y = y0;
        int iterations = 0;

        for (int k = 0; k < maxIter; k++) {
            iterations++;
            System.out.printf("x,y = %.6f, %.6f\n", x, y);

            // Residual vector F
            double f0 = f0(P1, x, y);
            double f1 = f1(P0, x, y);

            // Convergence test (residual-based)
            double residual = Math.hypot(f0, f1);
            System.out.printf("residual = %.6f\n", residual);
            if (residual < tol) {
                break;
            }

            double[][] JJ = {
                { 1, -P1.getDeriv(y) },
                { -P0.getDeriv(x), 1 }
            };

            RealMatrix J = new Array2DRowRealMatrix(JJ);
            RealVector F = new ArrayRealVector(new double[] {f0, f1});
            // Solve J * delta = F
            RealVector delta = new LUDecomposition(J).getSolver().solve(F);
            // Newton update
            x -= delta.getEntry(0);
            y -= delta.getEntry(1);
        }

        System.out.println("iterations: " + iterations);
        return new double[]{x, y};
    }

    // --------------------------------------------------------------

    // public interface Parabola {
    //     double getValueX(double x, double y);
    //     double getValueY(double x, double y);
    //     double getDerivX(double x, double y);
    //     double getDerivY(double x, double y);
    // }

    public class ParabolaX extends QuadraticFunction {

        public ParabolaX(double a, double c, double d) {
            super(a, c, d);
        }

        @Override
        public double getValueX(double x, double y) {
            return x;
        }

        @Override
        public double getValueY(double x, double y) {
            return this.getVal(x);
        }

        @Override
        public double getDerivX(double x, double y) {
            return 1;
        }

        @Override
        public double getDerivY(double x, double y) {
            return this.getDeriv(y);
        }
    }

    public class ParabolaY extends QuadraticFunction {

        public ParabolaY(double a, double c, double d) {
            super(a, c, d);
        }

        @Override
        public double getValueX(double x, double y) {
            return this.getVal(y);
        }

        @Override
        public double getValueY(double x, double y) {
            return y;
        }

        @Override
        public double getDerivX(double x, double y) {
            return this.getDeriv(y);
        }

        @Override
        public double getDerivY(double x, double y) {
            return 0;
        }
    }

    // --------------------------------------------------------------

    public static void main(String[] args) {
        QuadraticFunction Q0 = new QuadraticFunction(-0.1, 0.02, 0.5);
        QuadraticFunction Q1 = new QuadraticFunction(0.1, 0.03 + 1, 0.5);
        double x0 = 1, y0 = 0;

        double[] X = new ParabolaIntersectionFinder3().getIntersection(Q0, Q1, x0, y0);
        System.out.println(Arrays.toString(X));
        // try also imagingbook.common.math.nonlinear.solveGaussNewton()
    }

}
