package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

/**
 * Concrete implementation of {@link WeightedDecompositionSolver} using
 * QR decomposition for its base solver.
 * Preferred if the design matrix {@code A} has full rank, i.e., is unlikely
 * to be rank-deficient. Generally faster and less memory demanding than
 * {@link WeightedDecompositionSolverSVD}.
 */
public class WeightedDecompositionSolverQR extends WeightedDecompositionSolver {

    /**
     * Constructor (see {@link WeightedDecompositionSolver}).
     * @param A the design matrix {@code A} (unweighted)
     * @param w the original weight vector {@code w} (no square-root values)
     */
    public WeightedDecompositionSolverQR(RealMatrix A, RealVector w) {
        super(A, w);
    }

    @Override
    DecompositionSolver makeSolver(RealMatrix As) {
        return new QRDecomposition(As).getSolver();
    }
}
