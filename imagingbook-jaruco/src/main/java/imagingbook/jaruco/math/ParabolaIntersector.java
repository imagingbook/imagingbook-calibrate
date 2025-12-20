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
public class ParabolaIntersector {

    static double DEFAULT_TOL = 1e-6;
    static int DEFAULT_MAX_ITER = 15;

    private final double tol;
    private final int maxIter;

    public ParabolaIntersector() {
        this(DEFAULT_TOL, DEFAULT_MAX_ITER);
    }

    public ParabolaIntersector(double tol, int maxIter) {
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
    public double[] getIntersection (Parabola.OverX P0, Parabola.OverY P1, double xStart, double yStart) {
        double x = xStart;
        double y = yStart;
        int iterations = 0;

        for (int k = 0; k < maxIter; k++) {
            iterations++;
            System.out.printf("x,y = %.6f, %.6f\n", x, y);

            // Residual vector F
            double r0 = x - P1.getXvalue(y); // = f0(x, y)
            double r1 = y - P0.getYvalue(x); // = f1(x, y);

            // Convergence test (residual-based)
            double residual = Math.hypot(r0, r1);
            System.out.printf("residual = %.6f\n", residual);
            if (residual < tol) {
                break;
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

        System.out.println("iterations: " + iterations);
        return new double[]{x, y};
    }

    // --------------------------------------------------------------

    public static void main(String[] args) {
        Parabola.OverX P0 = new Parabola.OverX(-0.1, 0.02, 0.5);
        Parabola.OverY P1 = new Parabola.OverY(0.1, 0.03 + 1, 0.5);
        double x0 = 1, y0 = 0;

        double[] X = new ParabolaIntersector().getIntersection(P0, P1, x0, y0);
        System.out.println(Arrays.toString(X));
        // try also imagingbook.common.math.nonlinear.solveGaussNewton()
    }

}
