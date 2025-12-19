package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;

import java.util.Arrays;

/**
 * https://chatgpt.com/share/694448e5-c3d4-8006-b5ed-ca6bf915b13c
 */
public class TwoVarNewton {

    static double F1(double x, double y,
                     double a1, double d1, double c1) {
        return y - a1*(x - d1)*(x - d1) - c1;
    }

    static double F2(double x, double y,
                     double a2, double d2, double c2) {
        return x - a2*(y - d2)*(y - d2) - c2;
    }

    static double dF1dx(double x, double a1, double d1) {
        return -2.0*a1*(x - d1);
    }

    static double dF1dy() { return 1.0; }

    static double dF2dx() { return 1.0; }

    static double dF2dy(double y, double a2, double d2) {
        return -2.0*a2*(y - d2);
    }


    // // Your functions
    // static double F1(double x, double y, double a1, double c1) {
    //     return y - (a1*x*x + c1);
    // }
    // static double F2(double x, double y, double a2, double c2) {
    //     return x - (a2*y*y + c2);
    // }
    //
    // // Partial derivatives
    // static double dF1dx(double x, double y, double a1) { return -2*a1*x; }
    // static double dF1dy(double x, double y)           { return 1; }
    // static double dF2dx(double x, double y)           { return 1; }
    // static double dF2dy(double x, double y, double a2) { return -2*a2*y; }

    public static double[] solve(
            double x0, double y0,
            double a1, double d1, double c1,
            double a2, double d2, double c2,
            double tol, int maxIter) {

        double x = x0;
        double y = y0;
        int iterations = 0;

        for (int k = 0; k < maxIter; k++) {
            iterations++;
            System.out.printf("x,y = %.6f, %.6f\n", x, y);

            // Residual vector F
            double f1 = F1(x, y, a1, d1, c1);
            double f2 = F2(x, y, a2, d2, c2);

            // Convergence test (residual-based)
            double residual = Math.hypot(f1, f2);
            System.out.printf("residual = %.6f\n", residual);
            if (residual < tol) {
                break;
            }

            // Jacobian matrix
            double[][] jArr = {
                    { dF1dx(x, a1, d1), dF1dy() },
                    { dF2dx(),          dF2dy(y, a2, d2) }
            };

            RealMatrix J = new Array2DRowRealMatrix(jArr);
            DecompositionSolver solver =
                    new LUDecomposition(J).getSolver();

            // Solve J * delta = F
            RealVector F =
                    new ArrayRealVector(new double[]{f1, f2});
            RealVector delta = solver.solve(F);

            // Newton update
            x -= delta.getEntry(0);
            y -= delta.getEntry(1);
        }

        System.out.println("iterations: " + iterations);
        return new double[]{x, y};
    }

    // --------------------------------------------------------------
    public static void main(String[] args) {
        double a0 = -0.1;
        double c0 = 0.02;
        double d0 = 0.5;

        double a1 = 0.1;
        double c1 = 0.03 + 1;
        double d1 = 0.5;

        double x0 = 1, y0 = 0;

        double[] X = solve(x0, y0,
                a0, d0, c0,
                a1, d1, c1,
                1e-6, 15);
        System.out.println(Arrays.toString(X));

    }
}
