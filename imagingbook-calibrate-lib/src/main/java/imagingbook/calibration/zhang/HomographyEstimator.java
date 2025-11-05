/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.Homography;
import imagingbook.calibration.util.MathUtil;
import imagingbook.calibration.util.PointStatistics;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

public class HomographyEstimator {

    /**
     * Maximum number of Levenberg-Marquardt evaluations.
     */
    public static int MaxLmEvaluations = 1000;

    /**
     * Maximum number of Levenberg-Marquardt iterations.
     */
    public static int MaxLmIterations = 1000;

    private final boolean normalizePoints;
    private final boolean doRefinement;


    /**
     * Constructor
     *
     * @param normalizePoints
     * @param doRefinement
     */
    public HomographyEstimator(boolean normalizePoints, boolean doRefinement) {
        this.normalizePoints = normalizePoints;
        this.doRefinement = doRefinement;
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D
     * point sequences assumed to be in correspondence (and of same length).
     *
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 1st sequence of 2D points
     * @return
     */
    public Homography getHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        if (ptsA.length != ptsB.length)
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        if (ptsA.length < 4)
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");

        final int n = ptsA.length;
        RealMatrix Na = (normalizePoints) ?
                PointStatistics.getNormalisationMatrix(ptsA) : MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix Nb = (normalizePoints) ?
                PointStatistics.getNormalisationMatrix(ptsB) : MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);

        for (int j = 0, r = 0; j < ptsA.length; j++) {
            final double[] pA = mapPoint(Na, ptsA[j].toDoubleArray());
            final double[] pB = mapPoint(Nb, ptsB[j].toDoubleArray());
            final double xA = pA[0];
            final double yA = pA[1];
            final double xB = pB[0];
            final double yB = pB[1];
            M.setRow(r + 0, new double[]{xA, yA, 1, 0, 0, 0, -(xA * xB), -(yA * xB), -(xB)});
            M.setRow(r + 1, new double[]{0, 0, 0, xA, yA, 1, -(xA * yB), -(yA * yB), -(yB)});
            r = r + 2;
        }
        // find h, such that M . h = 0:
        double[] h = MathUtil.solveHomogeneousSystem(M).toArray();
        // assemble homography matrix H from h:
        RealMatrix H = MatrixUtils.createRealMatrix(new double[][]
                {{h[0], h[1], h[2]},
                        {h[3], h[4], h[5]},
                        {h[6], h[7], h[8]}});
        // de-normalize the homography
        H = MatrixUtils.inverse(Nb).multiply(H).multiply(Na);
        Homography hom = new Homography(H); // this does normalization

        if (doRefinement) {
            hom = refineHomography(hom, ptsA, ptsB);
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

        LeastSquaresProblem problem = LeastSquaresFactory.create(
                LeastSquaresFactory.model(value, jacobian),
                MatrixUtils.createRealVector(observed),
                MathUtil.getRowPackedVector(Hinit),
                null,  // ConvergenceChecker
                MaxLmEvaluations,
                MaxLmIterations);

        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

        RealVector optimum = result.getPoint();
        RealMatrix Hopt = MathUtil.fromRowPackedVector(optimum, 3, 3);
        int iterations = result.getIterations();
        if (iterations >= MaxLmIterations) {
            throw new RuntimeException("refineHomography(): max. number of iterations exceeded");
        }
        // System.out.println("LM optimizer iterations " + iterations);
        return new Homography(Hopt);
    }

    private static MultivariateVectorFunction getValueFunction(final Pnt2d[] X) {
        // System.out.println("MultivariateVectorFunction getValueFunction");
        return new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] h) {
                final double[] Y = new double[X.length * 2];
                for (int j = 0; j < X.length; j++) {
                    final double x = X[j].getX();
                    final double y = X[j].getY();
                    final double w = h[6] * x + h[7] * y + h[8];
                    Y[j * 2 + 0] = (h[0] * x + h[1] * y + h[2]) / w;
                    Y[j * 2 + 1] = (h[3] * x + h[4] * y + h[5]) / w;
                }
                return Y;
            }
        };
    }

    private static MultivariateMatrixFunction getJacobianFunction(final Pnt2d[] X) {
        return new MultivariateMatrixFunction() {
            @Override
            public double[][] value(double[] h) {
                final double[][] J = new double[2 * X.length][];
                for (int i = 0; i < X.length; i++) {
                    final double x = X[i].getX();
                    final double y = X[i].getY();
                    final double w = h[6] * x + h[7] * y + h[8];
                    final double w2 = w * w;
                    final double sx = h[0] * x + h[1] * y + h[2];
                    J[2 * i + 0] = new double[]{x / w, y / w, 1 / w, 0, 0, 0, -sx * x / w2, -sx * y / w2, -sx / w2};
                    final double sy = h[3] * x + h[4] * y + h[5];
                    J[2 * i + 1] = new double[]{0, 0, 0, x / w, y / w, 1 / w, -sy * x / w2, -sy * y / w2, -sy / w2};
                }
                return J;
            }
        };
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

    /**
     * Estimate homographies for a single sequence of model points but multiple sequence
     * of observed points. All point sequences must be in correspondence and of same length.
     * @param modelPts
     * @param obsPoints
     * @param normalizePoints
     * @param doRefinement
     * @return
     */
    public static Homography[] estimateHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints, boolean normalizePoints, boolean doRefinement) {
        final int M = obsPoints.length;
        Homography[] homographies = new Homography[M];
            for(int i = 0; i < M; i++) {
                homographies[i] = Homography.from(modelPts, obsPoints[i], normalizePoints, doRefinement);
        }
        return homographies;
    }
}
