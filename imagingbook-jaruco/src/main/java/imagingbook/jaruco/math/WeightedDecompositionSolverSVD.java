package imagingbook.jaruco.math;

import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;


/**
 * Concrete implementation of {@link WeightedDecompositionSolver} using
 * Singular Value decomposition for its base solver.
 * Preferred if the design matrix {@code A} is possibly
 * rank-deficient or if the supplied weights vary strongly.
 * Generally slower and more memory demanding than
 * {@link WeightedDecompositionSolverQR}.
 */
public class WeightedDecompositionSolverSVD extends   WeightedDecompositionSolver {

    /**
     * Constructor (see {@link WeightedDecompositionSolver}).
     * @param A the design matrix {@code A} (unweighted)
     * @param w the original weight vector {@code w} (no square-root values)
     */
    public WeightedDecompositionSolverSVD(RealMatrix A, RealVector w) {
        super(A, w);
    }

    @Override
    DecompositionSolver makeSolver(RealMatrix As) {
        return new SingularValueDecomposition(As).getSolver();
    }
}
