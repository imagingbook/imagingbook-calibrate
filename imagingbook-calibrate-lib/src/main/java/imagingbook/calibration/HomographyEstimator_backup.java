/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.points.ProjectiveFit2d;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem.Evaluation;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;

import java.util.Arrays;

// TODO: check implementation of normalizePoints!
class HomographyEstimator_backup {

    /**
     * Maximum number of Levenberg-Marquardt evaluations.
     */
    public static int MaxLmEvaluations = 1000;

    /**
     * Maximum number of Levenberg-Marquardt iterations.
     */
    public static int MaxLmIterations = 100;

    private final boolean normalizePoints;
    private final boolean doRefinement;

    /**
     * The only constructor.
     * @param normalizePoints
     * @param doRefinement
     */
    public HomographyEstimator_backup(boolean normalizePoints, boolean doRefinement) {
        this.normalizePoints = normalizePoints;
        this.doRefinement = doRefinement;
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D
     * point sequences assumed to be in correspondence (and of same length).
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 1st sequence of 2D points
     * @return
     */
    protected Homography getHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        if (ptsA.length != ptsB.length)
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        if (ptsA.length < 4)
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");

        double[][] Ha = new ProjectiveFit2d(ptsA, ptsB).getTransformationMatrix();
        Homography hom = new Homography(Ha);                 // this does normalization
        // System.out.println("   HomographyEstimator: initial = \n" + hom);

        if (doRefinement) {
            hom = refineHomography(hom, ptsA, ptsB);
            // System.out.println("   HomographyEstimatior: refined = \n" + hom);
        }
        return new Homography(hom);
    }

    // -------------------------------------------------------------------------

    /**
     * Refines the initial homography by non-linear (Levenberg-Marquart)
     * optimization.
     *
     * @param Hinit the initial (estimated) homography
     * @param pntsA the 1st sequence of 2D points
     * @param pntsB the 2nd sequence of 2D points
     * @return the refined homography
     */
    private Homography refineHomography(Homography Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {
        final int M = pntsA.length;
        double[] observed = new double[2 * M];
        for (int i = 0; i < M; i++) {
            observed[i * 2 + 0] = pntsB[i].getX();
            observed[i * 2 + 1] = pntsB[i].getY();
        }
        MultivariateVectorFunction value = getValueFunction(pntsA);
        MultivariateMatrixFunction jacobian = getJacobianFunction(pntsA);

        double[] hstart = Arrays.copyOf(MathUtil.getRowPackedVector(Hinit).toArray(), 8);   // only first 8 values

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(value, jacobian)
                .target(MatrixUtils.createRealVector(observed))
                .start(new ArrayRealVector(hstart))
                .checker(new LoggingChecker(new EvaluationRmsChecker(1e-6, 1e-6)))
                .maxIterations(MaxLmIterations)
                .maxEvaluations(MaxLmIterations)
                .build();

        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        RealVector optimum = result.getPoint();
        double[] opt = optimum.toArray();
        RealMatrix Hopt = MatrixUtils.createRealMatrix(3, 3);// MathUtil.fromRowPackedVector(optimum, 3, 3);
        Hopt.setEntry(0, 0, opt[0]); Hopt.setEntry(0, 1, opt[1]); Hopt.setEntry(0, 2, opt[2]);
        Hopt.setEntry(1, 0, opt[3]); Hopt.setEntry(1, 1, opt[4]); Hopt.setEntry(1, 2, opt[5]);
        Hopt.setEntry(2, 0, opt[6]); Hopt.setEntry(2, 1, opt[7]); Hopt.setEntry(2, 2, 1.0);

        int iterations = result.getIterations();
        if (iterations >= MaxLmIterations) {
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
                    double w = h[6] * x + h[7] * y + 1;         // h[8] = 1 (fixed);
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
    static class LoggingChecker implements ConvergenceChecker<Evaluation> {
        private final ConvergenceChecker<Evaluation> delegate;

        public LoggingChecker(ConvergenceChecker<Evaluation> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean converged(int iteration, Evaluation previous, Evaluation current) {
            // Log residuals here
            // System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
            // System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
            return delegate.converged(iteration, previous, current);
        }
    }


    // helper method (may be used in tests too)
    protected static double[] mapPoint(RealMatrix M3x3, double[] p) {
        if (p.length != 2) {
            throw new IllegalArgumentException("vector p must be of length 2 but is " + p.length);
        }
        double[] pA = MathUtil.toHomogeneous(p);
        double[] pAt = M3x3.operate(pA);
        return MathUtil.toCartesian(pAt); // need to de-homogenize, since pAt[2] == 1?
    }

//    /**
//     * Estimate homographies for a single sequence of model points but multiple sequence
//     * of observed points. All point sequences must be in correspondence and of same length.
//     * @param modelPts
//     * @param obsPoints
//     * @param normalizePoints
//     * @param doRefinement
//     * @return
//     */
//    @Deprecated
//    public static Homography[] estimateHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints, boolean normalizePoints, boolean doRefinement) {
//        final int M = obsPoints.length;
//        Homography[] homographies = new Homography[M];
//            for(int i = 0; i < M; i++) {
//                homographies[i] = Homography.from(modelPts, obsPoints[i], normalizePoints, doRefinement);
//        }
//        return homographies;
//    }
}
