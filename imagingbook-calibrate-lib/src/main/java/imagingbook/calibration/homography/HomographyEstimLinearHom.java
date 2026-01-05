/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
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
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;

/**
 * Homography estimator based on solving a 3x3 homogeneous linear system.
 *
 * @author WB
 */
public class HomographyEstimLinearHom extends AbstractHomographyEstimator {


	public HomographyEstimLinearHom() {
		this(true, true);
	}

	public HomographyEstimLinearHom(boolean normalizePoints, boolean doRefinement) {
		super(normalizePoints, doRefinement);
	}

	// ------------------------------------------------------------

	/**
	 * Estimates the homography (projective) transformation from two given 2D point sets. The correspondence between the
	 * points is assumed to be known.
	 *
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (3 x 3 matrix)
	 */
	@Override
	Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
		System.out.println("HomographyEstimLinearHom.estimateHomography() " + normalizePoints + " " + doRefinement);
		if (ptsA.length != ptsB.length)
			throw new IllegalArgumentException("point sequences A, B have different lengths");
		if (ptsA.length < 4)
			throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");
		int n = ptsA.length;

		// matrices for statistical normalization
		RealMatrix Na = (normalizePoints) ? getNormalisationMatrix(ptsA) : null;
		RealMatrix Nb = (normalizePoints) ? getNormalisationMatrix(ptsB) : null;

		RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);
		for (int i = 0, r = 0; i < ptsA.length; i++, r+=2) {
			double[] pA = map2dHomogeneous(ptsA[i].toDoubleArray(), Na);
			double[] pB = map2dHomogeneous(ptsB[i].toDoubleArray(), Nb);
			double xA = pA[0];
			double yA = pA[1];
			double xB = pB[0];
			double yB = pB[1];
			M.setRow(r + 0, new double[]{xA, yA, 1, 0, 0, 0, -xA * xB, -yA * xB, -xB});
			M.setRow(r + 1, new double[]{0, 0, 0, xA, yA, 1, -xA * yB, -yA * yB, -yB});
			// r = r + 2;
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
		Homography hom = new Homography(H);
		System.out.println("   HomographyEstimLinearHom: initial = \n" + hom);
		if (doRefinement) {
			return refine(hom, ptsA, ptsB);
		}
		return hom;
	}

	// NON-LINEAR REFINEMENT (using all 9 homography parameters) ----------------------------------

	public static int maxLmEvaluations = 1000;
	public static int maxLmIterations = 1000;

	/**
	 * Refines the initial homography by non-linear (Levenberg-Marquart)
	 * optimization.
	 * @param Hinit the initial (estimated) homography
	 * @param pntsA the 1st sequence of 2D points (the model points)
	 * @param pntsB the 2nd sequence of 2D points (the observed image points)
	 * @return the refined homography
	 */
	private Homography refine(Homography Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {

		final int M = pntsA.length;
		double[] observed = new double[2 * M];
		for (int i = 0; i < M; i++) {
			observed[i * 2 + 0] = pntsB[i].getX();
			observed[i * 2 + 1] = pntsB[i].getY();
		}
		MultivariateVectorFunction value = getValueFunction(pntsA);
		MultivariateMatrixFunction jacobian = getJacobianFunction(pntsA);

		double[] hstart = MathUtil.getRowPackedVector(Hinit).toArray();	// use all 9 values!
		System.out.println("HomographyEstimLinearHom.refine(): hstart = \n" + Matrix.toString(hstart));

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

		RealMatrix Hopt = MathUtil.fromRowPackedVector(optimum, 3, 3);

		int iterations = result.getIterations();
		if (iterations >= maxLmIterations) {
			throw new RuntimeException("refineHomography(): max. number of iterations exceeded");
		}
		System.out.println("   LM optimizer iterations = " + result.getIterations());
		System.out.println("   LM optimizer avg |residual| = " + (result.getResiduals().getNorm()/M));

		if (Math.abs(Hopt.getEntry(2, 2)) > 10e-8) {
			Hopt = Hopt.scalarMultiply(1.0 / Hopt.getEntry(2, 2));
		}
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
					double w = h[6] * x + h[7] * y + h[8];
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
					double w =  h[6] * x + h[7] * y + h[8];         // h[8] = 1 (fixed)?
					double w2 = w * w;
					// J[2 * i + 0] = new double[]{x/w, y/w, 1/w, 0, 0, 0, -sx * x/w2, -sx * y/w2};
					J[2 * i + 0] = new double[]{x / w, y / w, 1 / w, 0, 0, 0, -sx * x / w2, -sx * y / w2, -sx / w2};
					// J[2 * i + 1] = new double[]{0, 0, 0, x/w, y/w, 1/w, -sy * x/w2, -sy * y/w2};
					J[2 * i + 1] = new double[]{0, 0, 0, x / w, y / w, 1 / w, -sy * x / w2, -sy * y / w2, -sy / w2};
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
		public boolean converged(int iteration,
								 LeastSquaresProblem.Evaluation previous,
								 LeastSquaresProblem.Evaluation current) {
			// Log residuals here
			System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
			System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
			return delegate.converged(iteration, previous, current);
		}
	}


}
