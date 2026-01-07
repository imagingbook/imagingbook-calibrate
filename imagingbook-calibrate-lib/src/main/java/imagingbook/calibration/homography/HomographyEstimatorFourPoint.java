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
import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;

import java.util.Arrays;

import static imagingbook.common.math.Matrix.fromRowPackedVector;
import static imagingbook.common.math.Matrix.getRowPackedVector;

/**
 * Homography estimator based on solving a 3x3 homogeneous linear system.
 */
public class HomographyEstimatorFourPoint extends HomographyEstimator {


	public HomographyEstimatorFourPoint() {
		this(true, true);
	}

	public HomographyEstimatorFourPoint(boolean normalizePoints, boolean doRefinement) {
		super(normalizePoints, doRefinement);
	}

	// ------------------------------------------------------------

	/**
	 * Estimates the homography (projective) transformation from two given 2D point sets.
	 * The correspondence between the points is assumed to be known.
	 * The estimate is obtained by solving a homogeneous linear system over all 9 elements
	 * of the homography matrix.
	 *
	 * @param ptsA the 1st sequence of 2D points
	 * @param ptsB the 2nd sequence of 2D points
	 * @return the estimated homography (3 x 3 matrix)
	 */
	@Override
	Homography estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
		System.out.println("HomographyEstimatorFourPoint.estimateHomography() " + normalizePoints + " " + doRefinement);
		// System.out.println("ptsA = " + Arrays.toString(ptsA));
		// System.out.println("ptsB = " + Arrays.toString(ptsB));


		if (ptsA.length != ptsB.length)
			throw new IllegalArgumentException("point sequences A, B have different lengths");
		if (ptsA.length < 4)
			throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");
		int n = ptsA.length;

		// always normalize point sets:
		RealMatrix Na = getNormalisationMatrix(ptsA);
		RealMatrix Nb = getNormalisationMatrix(ptsB);
		Pnt2d[] ptsAn = normalizePointSet(ptsA, Na);
		Pnt2d[] ptsBn = normalizePointSet(ptsB, Nb);
		// System.out.println("ptsAn = " + Arrays.toString(ptsAn));
		// System.out.println("ptsBn = " + Arrays.toString(ptsBn));

		RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);
		for (int i = 0; i < ptsA.length; i++) {
			double[] pA = ptsAn[i].toDoubleArray();
			double[] pB = ptsBn[i].toDoubleArray();
			double xA = pA[0];
			double yA = pA[1];
			double xB = pB[0];
			double yB = pB[1];
			M.setRow(2 * i + 0, new double[]{xA, yA, 1, 0, 0, 0, -xA * xB, -yA * xB, -xB});
			M.setRow(2 * i + 1, new double[]{0, 0, 0, xA, yA, 1, -xA * yB, -yA * yB, -yB});
		}

		// find h, such that M . h = 0:
		double[] h = MathUtil.solveHomogeneousSystem(M).toArray();
		System.out.println("   FourPoint: h (normalized) = " + Matrix.toString(h));
		System.out.println("   FourPoint: |h| (normalized) = " + Matrix.normL2(h));

		// assemble homography matrix H from h:
		RealMatrix Hinit = MatrixUtils.createRealMatrix(new double[][]
				{{h[0], h[1], h[2]},
				{h[3], h[4], h[5]},
				{h[6], h[7], h[8]}});

		System.out.println("   FourPoint: H (initial, normalized) = \n" + Matrix.toString(Hinit));
		System.out.println("   initial reproj. error (normalized) = " + getReprojectionError(ptsAn, ptsBn, Hinit));

		RealMatrix HinitDen = MatrixUtils.inverse(Nb).multiply(Hinit).multiply(Na);
		System.out.println("   FourPoint: Hinit denormalized = \n" + Matrix.toString(new Homography(HinitDen)));
		System.out.println("   initial reproj. error (denormalized) = " + getReprojectionError(ptsA, ptsB, HinitDen));

		// --------------------
		RealMatrix Hn = refine(Hinit, ptsAn, ptsBn, 1000, 100);
		// --------------------
		System.out.println("   FourPoint: H (refined, normalized) = \n" + Matrix.toString(Hn));
		System.out.println("   normalized reproj. error = " + getReprojectionError(ptsAn, ptsBn, Hn));


		RealMatrix H = MatrixUtils.inverse(Nb).multiply(Hn).multiply(Na);
		System.out.println("FourPoint: H (refined, denormalized) = \n" + Matrix.toString(H));
		System.out.println("   denormalized reproj. error = " + getReprojectionError(ptsA, ptsB, H));

		// rescale M such that H[2][2] = 1 (unless H[2][2] close to 0)
		if (Math.abs(H.getEntry(2, 2)) > 10e-8) {
			H = H.scalarMultiply(1.0 / H.getEntry(2, 2));
		}

		return new Homography(H);
	}

	// ---------------------------------------------------



	// NON-LINEAR REFINEMENT (using all 9 homography parameters) ----------------------------------
	// deactivate default refinement:
	@Override
	Homography refineHomography(Homography Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB, int maxLmEvaluations, int maxLmIterations) {
		return Hinit;
	}


	/**
	 * Internal refinement using 4-corner algorithm and normalized point sets.
	 * @param Hinit
	 * @param pntsA
	 * @param pntsB
	 * @param maxLmEvaluations
	 * @param maxLmIterations
	 * @return
	 */
	RealMatrix refine(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB, int maxLmEvaluations, int maxLmIterations) {
		System.out.println("REFINING now ...");
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
		System.out.println("CC = " + Arrays.toString(CC));
		double[] pInit = getParameterVector(CC);

		// --------------------------------------------

		MultivariateVectorFunction valueFun = p -> {
			Pnt2d[] Cm = pointsFromParameters(p);
			RealMatrix Hm = get4PointHomography(C, Cm);
			Pnt2d[] Am = projectPoints(pntsA, Hm);
            double[] Y = flattenPointVector(Am);	// current 'value'
            return Y;
        };

		MultivariateMatrixFunction jacobianFun = p -> {
			double[] Y = valueFun.value(p);
			double[][] J = new double[2 * N][8];
			double epsilon = 1e-6; // The "nudge"
			for (int k = 0; k < 8; k++) {	// for each parameter pk
				// 1. Create a copy of the parameters to perturb
				double[] pk = p.clone();
				pk[k] += epsilon;
				// 2. Call your value function
				double[] Yk = valueFun.value(pk);
				// 3. Fill the k-th column of the Jacobian
				for (int j = 0; j < 2 * N; j++) {
					J[j][k] = (Yk[j] - Y[j]) / epsilon;
				}
			}
            return J;
        };



		// // --------------------------------------
		// // 1. Create the combined Jacobian function
		// MultivariateJacobianFunction modelFun = new MultivariateJacobianFunction() {
		// 	@Override
		// 	public Pair<RealVector, RealMatrix> value(RealVector point) {
		// 		double[] p = point.toArray();
		// 		// calculate tha current 'value' part:
		// 		Pnt2d[] Cm = pointsFromParameters(p);
		// 		RealMatrix Hcur = get4PointHomography(C, Cm);
		// 		Pnt2d[] Am = projectPoints(pntsA, Hcur);
		// 		double[] Y = flattenPointVector(Am);	// current 'value'
		//
		// 		// calculate the Jacobian for p = Hcur:
		// 		double[][] jacobian = new double[2 * N][8];
		// 		double epsilon = 1e-6; // The "nudge"
		// 		for (int k = 0; k < 8; k++) {	// for each parameter pk
		// 			// 1. Create a copy of the parameters to perturb
		// 			double[] perturbedP = p.clone();
		// 			perturbedP[k] += epsilon;
		//
		// 			// 2. Call your value function
		// 			double[] perturbedY = valueFun.value(perturbedP);
		//
		// 			// 3. Fill the j-th column of the Jacobian
		// 			for (int i = 0; i < numObs; i++) {
		// 				jacobian[i][k] = (perturbedY[i] - currentY[i]) / epsilon;
		// 			}
		// 		}
		//
		//
		// 		return new Pair<>(
		// 				new ArrayRealVector(Y, false),
		// 				MatrixUtils.createRealMatrix(jacobian)
		// 		);
		// 	}
		// };


		// -----------------------

		LeastSquaresProblem problem = new LeastSquaresBuilder()
				.model(valueFun, jacobianFun)
				// .model(modelFun)
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
		Pnt2d[] Copt = pointsFromParameters(pOpt);
		RealMatrix Hopt = get4PointHomography(C, Copt);

		System.out.println("LM optimizer iterations = " + result.getIterations());
		System.out.println("LM optimizer total |residual| = " + (result.getResiduals().getNorm()));
		System.out.println("LM optimizer avg |residual| = " + (result.getResiduals().getNorm()/N));

		// calculate residual check:
		double[] res = new double[2*N];
		double[] Yopt = valueFun.value(pOpt);
		double[] R =  Matrix.subtract(Yopt, Z);
		double r = Matrix.normL2(R);
		System.out.println("LM optimizer CHECK residual = " + r);

		// if (Math.abs(Hopt.getEntry(2, 2)) > 10e-8) {
		// 	Hopt = Hopt.scalarMultiply(1.0 / Hopt.getEntry(2, 2));
		// }
		System.out.println("FINISHED REFINING! ");
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
			System.out.println("Iteration " + iteration + " residuals: " + current.getResiduals().getNorm());
			System.out.println("point = " + Matrix.toString(current.getPoint().toArray()));
			return delegate.converged(iteration, previous, current);
		}
	}

	Pnt2d[] projectPoints(Pnt2d[] pnts, RealMatrix H) {
		Pnt2d[] pntsProj = new Pnt2d[pnts.length];
		for (int i = 0; i < pnts.length; i++) {
			pntsProj[i] = Pnt2d.from(map2dHomogeneous(pnts[i].toDoubleArray(), H));
		}
		return pntsProj;
	}

		/**
	 * Maps between n &gt; 4 point pairs, finds a least-squares solution
	 * for the homography parameters.
	 * NOTE: this is UNFINISHED code! check against DLT estimation of homography
	 * @param P sequence of points (source)
	 * @param Q sequence of points (target)
	 * @return a new projective mapping
	 */
	static RealMatrix get4PointHomography(Pnt2d[] P, Pnt2d[] Q) {
		final int n = P.length;
		if (n < 4) {
			throw new IllegalArgumentException(": fromNPoints() needs at least 4 points pairs");
		}
		double[] ba = new double[2 * n];
		double[][] Ma = new double[2 * n][];
		for (int i = 0; i < n; i++) {
			double x = P[i].getX();
			double y = P[i].getY();
			double u = Q[i].getX();
			double v = Q[i].getY();
			ba[2 * i + 0] = u;
			ba[2 * i + 1] = v;
			Ma[2 * i + 0] = new double[] { x, y, 1, 0, 0, 0, -u * x, -u * y };
			Ma[2 * i + 1] = new double[] { 0, 0, 0, x, y, 1, -v * x, -v * y };
		}

		RealMatrix M = new Array2DRowRealMatrix(Ma, false); //MatrixUtils.createRealMatrix(Ma);
		RealVector b = new ArrayRealVector(ba, false); //MatrixUtils.createRealVector(ba);
		DecompositionSolver solver = new SingularValueDecomposition(M).getSolver();
		RealVector h = solver.solve(b);
		RealMatrix A = MatrixUtils.createRealMatrix(3, 3);
		A.setEntry(0, 0, h.getEntry(0));
		A.setEntry(0, 1, h.getEntry(1));
		A.setEntry(0, 2, h.getEntry(2));
		A.setEntry(1, 0, h.getEntry(3));
		A.setEntry(1, 1, h.getEntry(4));
		A.setEntry(1, 2, h.getEntry(5));
		A.setEntry(2, 0, h.getEntry(6));
		A.setEntry(2, 1, h.getEntry(7));
		A.setEntry(2, 2, 1.0);
		return A;
	}

	// -------- helper methods --------------------------------------------------------------

	static double[] flattenPointVector(Pnt2d[] pnts) {
		final int N = pnts.length;
		double[] vec = new double[2*N];
		for (int i = 0; i < N; i++) {
			vec[2 * i + 0] = pnts[i].getX();
			vec[2 * i + 1] = pnts[i].getY();
		}
		return vec;
	}

	static double[] getParameterVector(Pnt2d[] pnts) {
		double[] p = new double[8];
		for (int i = 0; i < 4; i++) {
			p[2*i + 0] = pnts[i].getX();
			p[2*i + 1] = pnts[i].getY();
		}
		return p;
	}

	static Pnt2d[] pointsFromParameters(double[] p) {
		Pnt2d[] pnts = new Pnt2d[4];
		for (int i = 0; i < 4; i++) {
			pnts[i] = Pnt2d.from(p[2*i], p[2*i + 1]);
		}
		return pnts;
	}

	static double[] homographyFromParameters(double[] p) {

		return null;
	}


}
