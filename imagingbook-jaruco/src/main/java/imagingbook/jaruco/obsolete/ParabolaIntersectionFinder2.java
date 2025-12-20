package imagingbook.jaruco.obsolete;

import imagingbook.jaruco.math.Parabola;
import org.apache.commons.math4.legacy.linear.*;

import java.util.Arrays;

/**
 * https://chatgpt.com/share/694448e5-c3d4-8006-b5ed-ca6bf915b13c
 */
@Deprecated
public class ParabolaIntersectionFinder2 {

    static double DEFAULT_TOL = 1e-6;
    static int DEFAULT_MAX_ITER = 15;

    private final double tol;
    private final int maxIter;

    public ParabolaIntersectionFinder2() {
        this(DEFAULT_TOL, DEFAULT_MAX_ITER);
    }

    public ParabolaIntersectionFinder2(double tol, int maxIter) {
        this.tol = tol;
        this.maxIter = maxIter;
    }

    // --------------------------------------------------------

    private static double F0(Parabola Q0, double x, double y) {
        return y - Q0.getVal(x);
    }

    private static double F1(Parabola Q1, double x, double y) {
        return x - Q1.getVal(y);
    }

    // ----------------------------------------------------------------------------

    public double[] getIntersection (Parabola Q0, Parabola Q1, double x0, double y0) {
        double x = x0;
        double y = y0;
        int iterations = 0;

        for (int k = 0; k < maxIter; k++) {
            iterations++;
            System.out.printf("x,y = %.6f, %.6f\n", x, y);

            // Residual vector F
            double f0 = F0(Q0, x, y);
            double f1 = F1(Q1, x, y);

            // Convergence test (residual-based)
            double residual = Math.hypot(f0, f1);
            System.out.printf("residual = %.6f\n", residual);
            if (residual < tol) {
                break;
            }

            double[][] JJ = {
                    { -Q0.getDeriv(x), 1 },
                    { 1, -Q1.getDeriv(y) }
            };

            RealMatrix J = new Array2DRowRealMatrix(JJ);
            RealVector F = new ArrayRealVector(new double[]{f0, f1});
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

    public static void main(String[] args) {
        Parabola Q0 = new Parabola.OverX(-0.1, 0.02, 0.5);
        Parabola Q1 = new Parabola.OverY(0.1, 0.03 + 1, 0.5);
        double x0 = 1, y0 = 0;

        double[] X = new ParabolaIntersectionFinder2().getIntersection(Q0, Q1, x0, y0);
        System.out.println(Arrays.toString(X));
        // try also imagingbook.common.math.nonlinear.solveGaussNewton()
    }

}
