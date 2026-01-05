/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
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
 */
public class HomographyEstimatorSimple extends HomographyEstimator {

    /**
     * Constructor.
     * @param normalizePoints
     * @param doRefinement
     */
    public HomographyEstimatorSimple(boolean normalizePoints, boolean doRefinement) {
        super(normalizePoints, doRefinement);
    }

    @Override
    Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        System.out.println("HomographyEstimatorSimple.estimateHomography() " + normalizePoints + " " + doRefinement);
        if (ptsA.length != ptsB.length)
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        if (ptsA.length < 4)
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");
        int n = ptsA.length;

        // matrices for statistical normalization
        RealMatrix Na = (normalizePoints) ? getNormalisationMatrix(ptsA) : null;
        RealMatrix Nb = (normalizePoints) ? getNormalisationMatrix(ptsB) : null;

        double[] ba = new double[2 * n];
        double[][] Ma = new double[2 * n][];
        for (int i = 0; i < n; i++) {
            double[] pA = (normalizePoints) ?
                    map2dHomogeneous(ptsA[i].toDoubleArray(), Na) : ptsA[i].toDoubleArray();
            double[] pB = (normalizePoints) ?
                    map2dHomogeneous(ptsB[i].toDoubleArray(), Nb) : ptsB[i].toDoubleArray();
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

        // de-normalize the homography
        if (normalizePoints) {
            H = MatrixUtils.inverse(Nb).multiply(H).multiply(Na);
        }
        Homography hom = new Homography(H);
        System.out.println("   HomographyEstimatorSimple: initial = \n" + hom);

        return hom;
    }

    // NON-LINEAR REFINEMENT (using only 8 homography parameters, keeping h22 = 1 fixed) ----------

    @Override
    Homography refineHomography(Homography Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB, int maxLmEvaluations, int maxLmIterations) {
        final int M = pntsA.length;
        double[] observed = new double[2 * M];
        for (int i = 0; i < M; i++) {
            observed[i * 2 + 0] = pntsB[i].getX();
            observed[i * 2 + 1] = pntsB[i].getY();
        }
        MultivariateVectorFunction value = getValueFunction(pntsA);
        MultivariateMatrixFunction jacobian = getJacobianFunction(pntsA);

        double[] hstart = Arrays.copyOf(getRowPackedVector(Hinit).toArray(), 8);   // only first 8 values
        System.out.println("HomographyEstimatorSimple.refine(): hstart = \n" + Matrix.toString(hstart));

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(value, jacobian)
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

        int iterations = result.getIterations();
        if (iterations >= maxLmIterations) {
            throw new RuntimeException("refineHomography(): max. number of iterations exceeded");
        }
        System.out.println("   LM optimizer iterations = " + result.getIterations());
        System.out.println("   LM optimizer avg |residual| = " + (result.getResiduals().getNorm()/M));
        return new Homography(Hopt);
    }

    private static MultivariateVectorFunction getValueFunction(Pnt2d[] X) {
        return new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] h) {
                double[] Y = new double[X.length * 2];
                for (int j = 0; j < X.length; j++) {
                    double x = X[j].getX();
                    double y = X[j].getY();
                    double w = h[6] * x + h[7] * y + 1; // h[8];
                    Y[j * 2 + 0] = (h[0] * x + h[1] * y + h[2]) / w;
                    Y[j * 2 + 1] = (h[3] * x + h[4] * y + h[5]) / w;
                }
                return Y;
            }
        };
    }

    private static MultivariateMatrixFunction getJacobianFunction(Pnt2d[] X) {
        return new MultivariateMatrixFunction() {
            @Override
            public double[][] value(double[] h) {
                double[][] J = new double[2 * X.length][];
                for (int i = 0; i < X.length; i++) {
                    double x = X[i].getX();
                    double y = X[i].getY();
                    double sx = h[0] * x + h[1] * y + h[2];
                    double sy = h[3] * x + h[4] * y + h[5];
                    double w =  h[6] * x + h[7] * y + 1;         // h[8] = 1 (fixed);
                    double w2 = w * w;
                    J[2 * i + 0] = new double[]{x/w, y/w, 1/w, 0, 0, 0, -sx * x/w2, -sx * y/w2};
                    J[2 * i + 1] = new double[]{0, 0, 0, x/w, y/w, 1/w, -sy * x/w2, -sy * y/w2};
                }
                return J;
            }
        };
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
            System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
            System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
            return delegate.converged(iteration, previous, current);
        }
    }

}
