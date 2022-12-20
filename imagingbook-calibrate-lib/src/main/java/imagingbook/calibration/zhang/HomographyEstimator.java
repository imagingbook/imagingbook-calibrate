/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * This class defines methods for estimating the homography (projective) transformation between pairs of 2D point sets.
 *
 * @author WB
 */
public class HomographyEstimator {

	public static int MaxLmEvaluations = 1000;
	public static int MaxLmIterations = 1000;

	private final boolean normalizePointCoordinates;
	private final boolean doNonlinearRefinement;

	// ------------------------------------------------------------

	public HomographyEstimator() {
		this(true, true);
	}

	public HomographyEstimator(boolean normalizePointCoordinates, boolean doNonlinearRefinement) {
		this.normalizePointCoordinates = normalizePointCoordinates;
		this.doNonlinearRefinement = doNonlinearRefinement;
	}

	// ------------------------------------------------------------

	/**
	 * Estimates the homographies between a fixed set of 2D model points and multiple observations (image point sets).
	 * The correspondence between the points is assumed to be known.
	 *
	 * @param modelPts a sequence of 2D points on the model (calibration target)
	 * @param obsPoints a sequence 2D image point sets (one set per view).
	 * @return the sequence of estimated homographies (3 x 3 matrices), one for each view
	 */
	public RealMatrix[] estimateHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints) {
		final int M = obsPoints.length;
		RealMatrix[] homographies = new RealMatrix[M];
		for (int i = 0; i < M; i++) {
			RealMatrix Hinit = estimateHomography(modelPts, obsPoints[i]);
			RealMatrix H = doNonlinearRefinement ?
					refineHomography(Hinit, modelPts, obsPoints[i]) : Hinit;
			homographies[i] = H;
		}
		return homographies;
	}

	/**
	 * Estimates the homography (projective) transformation from two given 2D point sets. The correspondence between the
	 * points is assumed to be known.
	 *
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (3 x 3 matrix)
	 */
	public RealMatrix estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
		int n = ptsA.length;

		RealMatrix Na = (normalizePointCoordinates) ? getNormalisationMatrix(ptsA) : MatrixUtils.createRealIdentityMatrix(3);
		RealMatrix Nb = (normalizePointCoordinates) ? getNormalisationMatrix(ptsB) : MatrixUtils.createRealIdentityMatrix(3);
		RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);

		for (int j = 0, r = 0; j < ptsA.length; j++) {
			final double[] pA = transform(MathUtil.toArray(ptsA[j]), Na);
			final double[] pB = transform(MathUtil.toArray(ptsB[j]), Nb);
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

		// rescale M such that H[2][2] = 1 (unless H[2][2] close to 0)
		if (Math.abs(H.getEntry(2, 2)) > 10e-8) {
			H = H.scalarMultiply(1.0 / H.getEntry(2, 2));
		}

		return doNonlinearRefinement ? refineHomography(H, ptsA, ptsB) : H;
	}


	/**
	 * Refines the initial homography by non-linear (Levenberg-Marquart) optimization.
	 *
	 * @param Hinit the initial (estimated) homography matrix
	 * @param pntsA the 1st sequence of 2D points
	 * @param pntsB the 2nd sequence of 2D points
	 * @return the refined homography matrix
	 */
	public RealMatrix refineHomography(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {
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
		Optimum result = lm.optimize(problem);

		RealVector optimum = result.getPoint();
		RealMatrix Hopt = MathUtil.fromRowPackedVector(optimum, 3, 3);
		int iterations = result.getIterations();
		if (iterations >= MaxLmIterations) {
			throw new RuntimeException("refineHomography(): max. number of iterations exceeded");
		}
		// System.out.println("LM optimizer iterations " + iterations);

		return Hopt.scalarMultiply(1.0 / Hopt.getEntry(2, 2));
	}


	private MultivariateVectorFunction getValueFunction(final Pnt2d[] X) {
		// System.out.println("MultivariateVectorFunction getValueFunction");
		return new MultivariateVectorFunction() {
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

	protected MultivariateMatrixFunction getJacobianFunction(final Pnt2d[] X) {
		return new MultivariateMatrixFunction() {
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

	static double[] transform(double[] p, RealMatrix M3x3) {
		if (p.length != 2) {
			throw new IllegalArgumentException("transform(): vector p must be of length 2 but is " + p.length);
		}
		double[] pA = MathUtil.toHomogeneous(p);
		double[] pAt = M3x3.operate(pA);
		return MathUtil.toCartesian(pAt); // need to de-homogenize, since pAt[2] == 1?
	}

	private RealMatrix getNormalisationMatrix(Pnt2d[] pnts) {
		final int N = pnts.length;
		double[] x = new double[N];
		double[] y = new double[N];

		for (int i = 0; i < N; i++) {
			x[i] = pnts[i].getX();
			y[i] = pnts[i].getY();
		}

		// calculate the means in x/y
		double meanx = MathUtil.mean(x);
		double meany = MathUtil.mean(y);

		// calculate the variances in x/y
		double varx = MathUtil.variance(x);
		double vary = MathUtil.variance(y);

		double sx = Math.sqrt(2 / varx);
		double sy = Math.sqrt(2 / vary);

		RealMatrix matrixA = MatrixUtils.createRealMatrix(new double[][]{
				{sx, 0, -sx * meanx},
				{0, sy, -sy * meany},
				{0, 0, 1}});

		return matrixA;
	}

}
