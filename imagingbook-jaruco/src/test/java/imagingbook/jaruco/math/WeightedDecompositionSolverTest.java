package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class WeightedDecompositionSolverTest {

    static RealMatrix A = new Array2DRowRealMatrix(new double[][]
            {
                    {1, 0},
                    {1, 1},
                    {1, 2},
                    {1, 3}
            });
    static RealVector w = new ArrayRealVector(new double[]
            {1, 4, 0.25, 1});

    static RealVector b = new ArrayRealVector(new double[]
            {1, 2, 2, 4});

    static double[] ws_expected = {1.0, 2.0, 0.5, 1.0};
    static double[] bs_expected = {1.0, 4.0, 1.0, 4.0};

    static double[] expected = {1.008, 0.96};

    static void runExampleTest(WeightedDecompositionSolver solver) {
        RealVector ws = solver.getWeightVector();
        assertArrayEquals(ws_expected, ws.toArray(), 1e-6);

        RealVector bs = solver.getWeightedTargetVector(b);
        assertArrayEquals(bs_expected, bs.toArray(), 1e-6);

        RealVector x = solver.solve(b);
        assertArrayEquals(expected, x.toArray(), 1e-6);
    }

    // --------------

    @Test
    public void solveTestSVD() {
        runExampleTest(new WeightedDecompositionSolverSVD(A, w));
    }

    @Test
    public void solveTestQR() {
        runExampleTest(new WeightedDecompositionSolverQR(A, w));
    }
}