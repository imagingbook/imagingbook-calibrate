/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;

/**
 * Homography estimator based on solving a 3x3 homogeneous linear system.
 */
public class HomographyEstimatorFourCorners extends HomographyEstimator {

	private int maxLmEvaluations = 1000;
	private int maxLmIterations = 100;

	public HomographyEstimatorFourCorners() {
		super(true, true);
	}

	public HomographyEstimatorFourCorners(boolean normalizePoints, boolean doRefinement,
										  int maxLmEvaluations, int maxLmIterations) {
		super(normalizePoints, doRefinement);
		this.maxLmEvaluations = maxLmEvaluations;
		this.maxLmIterations = maxLmIterations;
	}

	/**
	 * Estimates the homography (projective) transformation from two given 2D point sets,
	 * which are normalized if the {@code normalizePoints} flag is set in the constructor.
	 * The two point sequences must be in correspondence.
	 * The initial estimate is obtained by solving a homogeneous linear system over all 9 elements
	 * of the homography matrix (also known as "Direct Linear Transform" or DLT method).
	 * If the supplied point sets are normalized, the returned homography matrix must be
	 * re-adjusted accordingly. See {@link HomographyEstimator#getHomography(Pnt2d[], Pnt2d[])}.
	 * Not that point set normalization is performed in super-class {@link HomographyEstimator},
	 * thus the supplied point sets are typically normalized already.
	 *
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (3 x 3 matrix)
	 */
	@Override
	RealMatrix estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
		final int n = ptsA.length;
		RealMatrix M = MatrixUtils.createRealMatrix(2 * n, 9);
		for (int i = 0; i < ptsA.length; i++) {
			double[] pA = ptsA[i].toDoubleArray();
			double[] pB = ptsB[i].toDoubleArray();
			double xA = pA[0];
			double yA = pA[1];
			double xB = pB[0];
			double yB = pB[1];
			M.setRow(2 * i + 0, new double[]{xA, yA, 1, 0, 0, 0, -xA * xB, -yA * xB, -xB});
			M.setRow(2 * i + 1, new double[]{0, 0, 0, xA, yA, 1, -xA * yB, -yA * yB, -yB});
		}
		// find h, such that M . h = 0:
		double[] h = MathUtil.solveHomogeneousSystem(M).toArray();
		// System.out.println("   FourPoint: h (normalized) = " + Matrix.toString(h));
		// System.out.println("   FourPoint: |h| (normalized) = " + Matrix.normL2(h));

		// assemble homography matrix H from h:
		RealMatrix Hinit = MatrixUtils.createRealMatrix(new double[][]
				{{h[0], h[1], h[2]},
				 {h[3], h[4], h[5]},
				 {h[6], h[7], h[8]}});
		// System.out.println("   FourPoint: H (initial, normalized) = \n" + Matrix.toString(Hinit));
		// System.out.println("   initial reproj. error (normalized) = " + getReprojectionError(ptsA, ptsB, Hinit));
		return Hinit;
	}

	/**
	 * Performs homography refinement using the four-corner method, with projected corner
	 * coordinates as the parameters of the optimizer.
	 * Note that all coordinates are assumed to be in normalized coordinate space and
	 * also {@code Hinit} refers to normalized coordinates!
	 * @param Hinit	the initial homography
	 * @param pntsA the first point sequence
	 * @param pntsB the second point sequence
	 * @return the refined homography
	 */
	RealMatrix refineHomography(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB) {
		final int N = pntsA.length;
		// 2. Choose "virtual" corner points in source domain:
		Pnt2d[] C = {
				Pnt2d.from(-2, 2),
				Pnt2d.from(2, 2),
				Pnt2d.from(2, -2),
				Pnt2d.from(-2, -2)
		};
		// 3. Set up the (fixed) target vector from observed image points:
		double[] Z = flattenPointVector(pntsB);
		// 4. Set Hcur <- Hinit
		RealMatrix Hcur = Hinit;
		// 5. Project corner points to image domain and set up initial parameter vector p:
		Pnt2d[] CC = projectPoints(C, Hcur);
		// System.out.println("CC = " + Arrays.toString(CC));
		double[] pInit = flattenPointVector(CC);

		// --------------------------------------------

		FourCornerHomography fph = new FourCornerHomography(C);

		MultivariateVectorFunction valueFun = p -> {
			Pnt2d[] Cm = cornersFromParameters(p);
			RealMatrix Hm = fph.getHom(Cm);
			Pnt2d[] Am = projectPoints(pntsA, Hm);
            return flattenPointVector(Am);	// current 'value' vector Y
        };

		MultivariateMatrixFunction jacobianFun = p -> {
			double[] Y = valueFun.value(p);
			double[][] J = new double[2 * N][8];
			double epsilon = 1e-6; // The "nudge"
			for (int k = 0; k < 8; k++) {	// for each parameter pk
				// 1. Create a copy of the parameters to perturb
				double[] pk = p.clone();
				pk[k] += epsilon;
				// 2. Call the value function
				double[] Yk = valueFun.value(pk);
				// 3. Fill the k-th column of the Jacobian
				for (int j = 0; j < 2 * N; j++) {
					J[j][k] = (Yk[j] - Y[j]) / epsilon;
				}
			}
            return J;
        };

		LeastSquaresProblem problem = new LeastSquaresBuilder()
				.model(valueFun, jacobianFun)
				.target(MatrixUtils.createRealVector(Z))
				.start(new ArrayRealVector(pInit))
				.checker(new LoggingChecker(new EvaluationRmsChecker(1e-6, 1e-6)))
				.maxIterations(maxLmIterations)
				.maxEvaluations(maxLmEvaluations)
				.lazyEvaluation(true)
				.build();

		LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
		LeastSquaresOptimizer.Optimum result = lm.optimize(problem);

		double[] pOpt = result.getPoint().toArray();
		Pnt2d[] Copt = cornersFromParameters(pOpt);
		RealMatrix Hopt = fph.getHom(Copt);
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
		public boolean converged(int iteration,
								 LeastSquaresProblem.Evaluation previous,
								 LeastSquaresProblem.Evaluation current) {
			// Log residuals here
			// System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
			// System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
			return delegate.converged(iteration, previous, current);
		}
	}

	// -------- helper methods --------------------------------------------------------------

	private static double[] flattenPointVector(Pnt2d[] pnts) {
		final int N = pnts.length;
		double[] vec = new double[2 * N];
		for (int i = 0; i < N; i++) {
			vec[2 * i + 0] = pnts[i].getX();
			vec[2 * i + 1] = pnts[i].getY();
		}
		return vec;
	}

	private  Pnt2d[] cornersFromParameters(double[] p) {
		Pnt2d[] pnts = new Pnt2d[4];
		for (int i = 0; i < 4; i++) {
			pnts[i] = Pnt2d.from(p[2*i], p[2*i + 1]);
		}
		return pnts;
	}

	Pnt2d[] projectPoints(Pnt2d[] pnts, RealMatrix H) {
		Pnt2d[] pntsProj = new Pnt2d[pnts.length];
		for (int i = 0; i < pnts.length; i++) {
			pntsProj[i] = Pnt2d.from(map2dHomogeneous(pnts[i].toDoubleArray(), H));
		}
		return pntsProj;
	}

	/**
	 * Small inner class for efficient calculation of 4-corner homographies (exactly 4
	 * corresponding points).
	 * The matrix elements for the constant x/y coordinates of the source corners are only calculated
	 * once. Only the variable u/v coordinates of the projected corners are filled in each iteration.
	 *
	 * <pre>
	 * Ma[2 * i + 0] = new double[] { x, y, 1, 0, 0, 0, -u * x, -u * y };
	 * Ma[2 * i + 1] = new double[] { 0, 0, 0, x, y, 1, -v * x, -v * y };
	 * </pre>
	 */
	static class FourCornerHomography {

		private final double[][] Ma = new double[8][];
		private final double[] ba = new double[8];

		FourCornerHomography(Pnt2d[] P) {
			// fill in fixed elements of Ma (x/y dependent)
			for (int i = 0, j = 0; i < 4; i++, j+=2) {	// j = 2i
				double x = P[i].getX();
				double y = P[i].getY();
				Ma[j + 0] = new double[] { x, y, 1, 0, 0, 0, 0, 0 };
				Ma[j + 1] = new double[] { 0, 0, 0, x, y, 1, 0, 0 };
			}
		}

		RealMatrix getHom(Pnt2d[] Q) {
			for (int i = 0, j = 0; i < 4; i++, j+=2) {	// j = 2i
				// fill in variable elements of Ma (u/v dependent)
				double u = Q[i].getX();
				double v = Q[i].getY();
				ba[j + 0] = u;
				ba[j + 1] = v;
				double x = Ma[j + 0][0];
				double y = Ma[j + 0][1];
				Ma[j + 0][6] = -u * x; // new double[] { x, y, 1, 0, 0, 0, -u * x, -u * y };
				Ma[j + 0][7] = -u * y;
				Ma[j + 1][6] = -v * x; // new double[] { 0, 0, 0, x, y, 1, -v * x, -v * y };
				Ma[j + 1][7] = -v * y;
			}
			RealMatrix M = new Array2DRowRealMatrix(Ma, false);
			RealVector b = new ArrayRealVector(ba, false);
			DecompositionSolver solver = new LUDecomposition(M).getSolver();
			double[] h = solver.solve(b).toArray();
			double[][] Ha = {
					{h[0], h[1], h[2]},
					{h[3], h[4], h[5]},
					{h[6], h[7],  1  }};
			return new Array2DRowRealMatrix(Ha, false);
		}

	}

}
