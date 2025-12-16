package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularMatrixException;

/**
 * Defines a decomposition solver for solving linear problems of the type
 * {@code W A x = W b}, where {@code W} is a diagonal weight matrix.
 * Each diagonal value of {@code W}  assigns a specific weight to the associated equation
 * (row) of the problem. The additional vector {@code w} passed to the constructors
 * is the vector of original weights. The square root of each value {@code w[i]} is used
 * to populate the diagonal of the (internal) weight matrix {@code W}.
 * Method {@link #solve(RealVector)} returns a least-squares solution if the
 * system is over-determined.
 * This is class is abstract, concrete implementations using different
 * base solvers are available in classes
 * {@link WeightedDecompositionSolverQR} and
 * {@link WeightedDecompositionSolverSVD}.
 *
 */
public abstract class WeightedDecompositionSolver implements DecompositionSolver {
    private final RealVector ws;        // weight vector (square root of w)
    private final DecompositionSolver solver;

    // Package-private super-constructor (to be called by concrete constructors).
    WeightedDecompositionSolver(RealMatrix A, RealVector w) {
        int m = A.getRowDimension();       // number of rows
        int n = A.getColumnDimension();    // number of columns
        double[][] AA = A.getData();
        double[] ww = w.toArray();

        // set up sqrt weight vector and weighted design matrix:
        for (int i = 0; i < m; i++) {
            ww[i] = Math.sqrt(ww[i]);
            for (int j = 0; j < n; j++) {
                AA[i][j] *= ww[i];
            }
        }

        RealMatrix As = new Array2DRowRealMatrix(AA, false);
        this.ws = new ArrayRealVector(ww, false);
        this.solver = makeSolver(As);
    }

    // -------------------------------------------------------------------------

    public RealVector getWeightVector() {
        return ws;
    }

    public RealVector getWeightedTargetVector(RealVector b) {
        return ws.ebeMultiply(b);
    }

    // -------------------------------------------------------------------------

    // to be implemented by real classes
    abstract DecompositionSolver makeSolver(RealMatrix As);

    @Override
    public RealVector solve(RealVector b) throws SingularMatrixException {
        RealVector bw = getWeightedTargetVector(b); //ws.ebeMultiply(b);
        return solver.solve(bw);
    }

    @Override
    public RealMatrix solve(RealMatrix realMatrix) throws SingularMatrixException {
        throw new UnsupportedOperationException(
                "Not supported: solve(RealMatrix realMatrix)");
        // return solver.solve(realMatrix);
    }

    @Override
    public boolean isNonSingular() {
        return solver.isNonSingular();
    }

    @Override
    public RealMatrix getInverse() throws SingularMatrixException {
        return solver.getInverse();
    }
}
