/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;

import java.util.Arrays;

import static imagingbook.common.math.Matrix.getRowPackedVector;

/**
 * Homography estimator based on solving a 3x3 non-homogeneous linear system.
 * Refinement is done with a Levenberg-Marquart optimizer directly on the 8 original homography
 * parameters, keeping the scale fixed. This is numerically less than clean but
 * nevertheless seems to work well.
 */
public class HomographyEstimatorSimplistic extends HomographyEstimator {

    private int maxLmEvaluations = 1000;
    private int maxLmIterations = 100;

    /**
     * Constructor.
     * @param normalizePoints
     * @param doRefinement
     * @param maxLmEvaluations
     * @param maxLmIterations
     */
    public HomographyEstimatorSimplistic(boolean normalizePoints, boolean doRefinement, int maxLmEvaluations, int maxLmIterations) {
        super(normalizePoints, doRefinement);
        this.maxLmEvaluations = maxLmEvaluations;
        this.maxLmIterations = maxLmIterations;
    }

    @Override
    RealMatrix estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        // System.out.println("HomographyEstimatorSimplistic.estimateHomography() " + normalizePoints + " " + doRefinement);
        int n = ptsA.length;
        double[] ba = new double[2 * n];
        double[][] Ma = new double[2 * n][];
        for (int i = 0; i < n; i++) {
            double[] pA = ptsA[i].toDoubleArray();
            double[] pB = ptsB[i].toDoubleArray();
            double xA = pA[0];
            double yA = pA[1];
            double xB = pB[0];
            double yB = pB[1];
            ba[2 * i + 0] = xB;
            ba[2 * i + 1] = yB;
            Ma[2 * i + 0] = new double[] { xA, yA, 1, 0, 0, 0, -xB * xA, -xB * yA };
            Ma[2 * i + 1] = new double[] { 0, 0, 0, xA, yA, 1, -yB * xA, -yB * yA };
        }

        RealMatrix M = MatrixUtils.createRealMatrix(Ma);
        RealVector b = MatrixUtils.createRealVector(ba);
        DecompositionSolver solver = new SingularValueDecomposition(M).getSolver();
        // find least-squares solution to M * h = b :
        RealVector h = solver.solve(b);

        RealMatrix H = MatrixUtils.createRealMatrix(3, 3);
        H.setEntry(0, 0, h.getEntry(0));
        H.setEntry(0, 1, h.getEntry(1));
        H.setEntry(0, 2, h.getEntry(2));
        H.setEntry(1, 0, h.getEntry(3));
        H.setEntry(1, 1, h.getEntry(4));
        H.setEntry(1, 2, h.getEntry(5));
        H.setEntry(2, 0, h.getEntry(6));
        H.setEntry(2, 1, h.getEntry(7));
        H.setEntry(2, 2, 1.0);

        // System.out.println("   HomographyEstimatorSimplistic: reproj. error = " + getReprojectionError(ptsA, ptsB, H));
        // System.out.println("   HomographyEstimatorSimplistic: initial = \n" + Matrix.toString(H));
        return H;
    }

    // NON-LINEAR REFINEMENT (using only 8 homography parameters, keeping h22 = 1 fixed) ----------

    @Override
    RealMatrix refineHomography(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {
        final int M = pntsA.length;
        double[] observed = new double[2 * M];
        for (int i = 0; i < M; i++) {
            observed[i * 2 + 0] = pntsB[i].getX();
            observed[i * 2 + 1] = pntsB[i].getY();
        }

        MultivariateVectorFunction valueFun = h -> {
            double[] Y = new double[2 * M];
            for (int j = 0; j < M; j++) {
                double x = pntsA[j].getX();
                double y = pntsA[j].getY();
                double w = h[6] * x + h[7] * y + 1; // h[8];
                Y[j * 2 + 0] = (h[0] * x + h[1] * y + h[2]) / w;
                Y[j * 2 + 1] = (h[3] * x + h[4] * y + h[5]) / w;
            }
            return Y;
        };

        MultivariateMatrixFunction jacobianFun = h -> {
                double[][] J = new double[2 * M][];
                for (int i = 0; i < M; i++) {
                    double x = pntsA[i].getX();
                    double y = pntsA[i].getY();
                    double sx = h[0] * x + h[1] * y + h[2];
                    double sy = h[3] * x + h[4] * y + h[5];
                    double w =  h[6] * x + h[7] * y + 1;         // h[8] = 1 (fixed);
                    double w2 = w * w;
                    J[2 * i + 0] = new double[]{x/w, y/w, 1/w, 0, 0, 0, -sx * x/w2, -sx * y/w2};
                    J[2 * i + 1] = new double[]{0, 0, 0, x/w, y/w, 1/w, -sy * x/w2, -sy * y/w2};
                }
                return J;
            };

        double[] hstart = Arrays.copyOf(getRowPackedVector(Hinit).toArray(), 8);   // only first 8 values
        // System.out.println("HomographyEstimatorSimplistic.refine(): hstart = \n" + Matrix.toString(hstart));

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(valueFun, jacobianFun)
                .target(MatrixUtils.createRealVector(observed))
                .start(new ArrayRealVector(hstart))
                .checker(new LoggingChecker(new EvaluationRmsChecker(1e-6, 1e-6)))
                .maxIterations(maxLmIterations)
                .maxEvaluations(maxLmEvaluations)
                .build();

        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        RealVector optimum = result.getPoint();
        double[] opt = optimum.toArray();
        RealMatrix Hopt = MatrixUtils.createRealMatrix(3, 3); // MathUtil.fromRowPackedVector(optimum, 3, 3);
        Hopt.setEntry(0, 0, opt[0]); Hopt.setEntry(0, 1, opt[1]); Hopt.setEntry(0, 2, opt[2]);
        Hopt.setEntry(1, 0, opt[3]); Hopt.setEntry(1, 1, opt[4]); Hopt.setEntry(1, 2, opt[5]);
        Hopt.setEntry(2, 0, opt[6]); Hopt.setEntry(2, 1, opt[7]); Hopt.setEntry(2, 2, 1.0);

        // System.out.println("   LM optimizer iterations = " + result.getIterations());
        // System.out.println("   LM optimizer avg |residual| = " + (result.getResiduals().getNorm()/M));
        return Hopt;
    }

    /**
     * Convergence checker which allows logging of residuals etc.
     */
    static class LoggingChecker implements ConvergenceChecker<LeastSquaresProblem.Evaluation> {
        private final ConvergenceChecker<LeastSquaresProblem.Evaluation> delegate;

        public LoggingChecker(ConvergenceChecker<LeastSquaresProblem.Evaluation> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean converged(int iteration, LeastSquaresProblem.Evaluation previous, LeastSquaresProblem.Evaluation current) {
            // Log residuals here
            // System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
            // System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
            return delegate.converged(iteration, previous, current);
        }
    }

}
