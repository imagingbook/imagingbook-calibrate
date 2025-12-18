package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;

/**
 * https://chatgpt.com/share/694448e5-c3d4-8006-b5ed-ca6bf915b13c
 */
public class TwoVarNewton {

    // Your functions
    static double F1(double x, double y, double a1, double c1) {
        return y - (a1*x*x + c1);
    }
    static double F2(double x, double y, double a2, double c2) {
        return x - (a2*y*y + c2);
    }

    // Partial derivatives
    static double dF1dx(double x, double y, double a1) { return -2*a1*x; }
    static double dF1dy(double x, double y)           { return 1; }
    static double dF2dx(double x, double y)           { return 1; }
    static double dF2dy(double x, double y, double a2) { return -2*a2*y; }

    public static double[] solve(
            double x0, double y0,
            double a1, double a2,
            double c1, double c2,
            double tol, int maxIter) {

        double x = x0, y = y0;
        for (int i = 0; i < maxIter; i++) {

            // Build F
            double[] fVec = {
                    F1(x, y, a1, c1),
                    F2(x, y, a2, c2)
            };

            // Stop if small
            if (Math.hypot(fVec[0], fVec[1]) < tol) break;

            // Jacobian
            double[][] jacArr = {
                    { dF1dx(x,y,a1), dF1dy(x,y)      },
                    { dF2dx(x,y),    dF2dy(x,y,a2)   }
            };

            RealMatrix jac = new Array2DRowRealMatrix(jacArr);
            DecompositionSolver solver = new LUDecomposition(jac).getSolver();

            RealVector fR = new ArrayRealVector(fVec);
            RealVector delta = solver.solve(fR);

            // Newton step
            x -= delta.getEntry(0);
            y -= delta.getEntry(1);
        }
        return new double[]{x, y};
    }
}
