package imagingbook.extras.calibration.zhang;

import imagingbook.extras.calibration.zhang.util.MathUtil;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;



public class HomographyEstimator {
	
	static int maxLmEvaluations = 1000;
	static int maxLmIterations = 1000;
	static boolean normalize = true;
	
	
	public RealMatrix[] estimateHomographies(Point2D[] modelPts, Point2D[][] obsPoints) {
		final int M = obsPoints.length;
		RealMatrix[] homographies = new RealMatrix[M];
		for (int i = 0; i < M; i++) {
			RealMatrix Hinit = estimateHomography(modelPts, obsPoints[i]);
			RealMatrix H = refineHomography(Hinit, modelPts, obsPoints[i]);
			homographies[i] = H;
		}
		return homographies;
	}
	
	public RealMatrix estimateHomography(Point2D[] ptsA, Point2D[] ptsB) {
		System.out.println("estimating homography");
		int n = ptsA.length;
		
		RealMatrix Na = (normalize) ? getNormalisationMatrix(ptsA) : MatrixUtils.createRealIdentityMatrix(3);
		RealMatrix Nb = (normalize) ? getNormalisationMatrix(ptsB) : MatrixUtils.createRealIdentityMatrix(3);
		
		RealMatrix M = MatrixUtils.createRealMatrix(n * 2, 9);

		for (int j = 0, k = 0; j < ptsA.length; j++, k += 2) {
			final double[] pA = transform(MathUtil.toArray(ptsA[j]), Na);
			final double[] pB = transform(MathUtil.toArray(ptsB[j]), Nb);
			final double xA = pA[0];
			final double yA = pA[1];
			final double xB = pB[0];
			final double yB = pB[1];			
			M.setRow(k + 0, new double[] {xA, yA, 1, 0, 0, 0, -(xA * xB), -(yA * xB), -(xB)});
			M.setRow(k + 1, new double[] {0, 0, 0, xA, yA, 1, -(xA * yB), -(yA * yB), -(yB)});
		}

		// find h, such that M . h = 0:
		double[] h = MathUtil.solveHomogeneousSystem(M).toArray();
		
		// assemble homography matrix H from h:
		RealMatrix H = MatrixUtils.createRealMatrix(new double[][] 
				{{h[0], h[1], h[2]},
				 {h[3], h[4], h[5]},
				 {h[6], h[7], h[8]}} );

		// de-normalize the homography
		H = MatrixUtils.inverse(Nb).multiply(H).multiply(Na);
		
		// rescale M such that H[2][2] = 1 (unless H[2][2] close to 0)
		if (Math.abs(H.getEntry(2, 2)) > 10e-8) {
			H = H.scalarMultiply(1.0 / H.getEntry(2, 2));
		}
		return H;
	}
	

	/**
	 * Refines the initial homography by Levenberg-Marquart nonlinear optimization.
	 * @param Hinit
	 * @param modelPts
	 * @param obsPts
	 * @return
	 */
	public RealMatrix refineHomography(RealMatrix Hinit, Point2D[] modelPts, Point2D[] obsPts) {
		final int M = modelPts.length;		
		double[] observed = new double[2 * M];
		for (int i = 0; i < M; i++) {
			observed[i * 2 + 0] = obsPts[i].getX();
			observed[i * 2 + 1] = obsPts[i].getY();
		}			
		MultivariateVectorFunction value = getValueFunction(modelPts);
		MultivariateMatrixFunction jacobian = getJacobianFunction(modelPts);

		LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
		Optimum result = lm.optimize(LeastSquaresFactory.create(
				LeastSquaresFactory.model(value, jacobian),
				MatrixUtils.createRealVector(observed), 
				MathUtil.getRowPackedVector(Hinit), 
				null,  // ConvergenceChecker
				maxLmEvaluations, 
				maxLmIterations));
		
		RealVector optimum = result.getPoint();
		RealMatrix Hopt = MathUtil.fromRowPackedVector(optimum, 3, 3);
//		System.out.println("LM optimizer iterations " + result.getIterations());
		return Hopt.scalarMultiply(1.0 / Hopt.getEntry(2, 2));
	}
	
	
	private MultivariateVectorFunction getValueFunction(final Point2D[] data1) {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] h) { // throws IllegalArgumentException {
				final double[] result = new double[data1.length * 2];
				for (int i = 0; i < data1.length; i++) {
					final double X1 = data1[i].getX();
					final double Y1 = data1[i].getY();
					final double t2 = X1 * h[6];
					final double t3 = Y1 * h[7];
					final double t4 = h[8] + t2 + t3;
					final double t5 = 1.0 / t4;
					result[i * 2 + 0] = t5 * (h[2] + X1 * h[0] + Y1 * h[1]);
					result[i * 2 + 1] = t5 * (h[5] + X1 * h[3] + Y1 * h[4]);
				}
				return result;
			}
		};
	}
	
	protected MultivariateMatrixFunction getJacobianFunction(final Point2D[] data1) {
		return new MultivariateMatrixFunction() {
			// See Multi-View Geometry in Computer Vision, eq 4.21, p129
			@Override
			public double[][] value(double[] h) {
				final double[][] result = new double[2 * data1.length][9];
				for (int i = 0; i < data1.length; i++) {
					final double X1 = data1[i].getX();
					final double Y1 = data1[i].getY();

					final double t2 = X1 * h[6];
					final double t3 = Y1 * h[7];
					final double t4 = h[8] + t2 + t3;
					final double t5 = 1.0 / t4;
					final double t6 = X1 * h[0];
					final double t7 = Y1 * h[1];
					final double t8 = h[2] + t6 + t7;
					final double t9 = 1.0 / (t4 * t4);
					final double t10 = X1 * t5;
					final double t11 = Y1 * t5;
					final double t12 = X1 * h[3];
					final double t13 = Y1 * h[4];
					final double t14 = h[5] + t12 + t13;
					
					result[i * 2 + 0][0] = t10;
					result[i * 2 + 0][1] = t11;
					result[i * 2 + 0][2] = t5;
					result[i * 2 + 0][6] = -X1 * t8 * t9;
					result[i * 2 + 0][7] = -Y1 * t8 * t9;
					result[i * 2 + 0][8] = -t8 * t9;
					
					result[i * 2 + 1][3] = t10;
					result[i * 2 + 1][4] = t11;
					result[i * 2 + 1][5] = t5;
					result[i * 2 + 1][6] = -X1 * t9 * t14;
					result[i * 2 + 1][7] = -Y1 * t9 * t14;
					result[i * 2 + 1][8] = -t9 * t14;
				}
				return result;
			}
		};
	}
	
	
	private double[] transform(double[] p, RealMatrix M3x3) {
		double[] pA = MathUtil.toHomogeneous(p);
		double[] pAt = M3x3.operate(pA);
		return MathUtil.toCartesian(pAt); // need to de-homogenize, since pAt[2] == 1?
	}
	
	private RealMatrix getNormalisationMatrix(Point2D[] pnts) {
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

		RealMatrix matrixA = MatrixUtils.createRealMatrix(new double[][] {
				{ sx,  0, -sx * meanx},
				{  0, sy, -sy * meany},
				{  0,  0,           1 }});
		
		return matrixA;
	}
	

	// TESTING --------------------------------------------------------
	
	static Random rand = new Random();
	private static Point2D apply(RealMatrix H, Point2D X, double noise) {
		
		double[] Xa = {X.getX(), X.getY(), 1};
		double[] Xb = H.operate(Xa);
		double xn = noise * rand.nextGaussian();
		double yn = noise * rand.nextGaussian();
		return new Point2D.Double(xn + Xb[0]/Xb[2], yn + Xb[1]/Xb[2]);
	}
	
	public static void main(String[] args) {
		RealMatrix Hreal = MatrixUtils.createRealMatrix(new double[][]
				{{3, 2, -1},
				{5, 0, 2},
				{4, 4, 9}});
		
		List<Point2D> pntlistA = new ArrayList<Point2D>();
		pntlistA.add(new Point2D.Double(10, 7));
		pntlistA.add(new Point2D.Double(3, -1));
		pntlistA.add(new Point2D.Double(5, 5));
		pntlistA.add(new Point2D.Double(-6, 13));
		pntlistA.add(new Point2D.Double(0, 1));
		pntlistA.add(new Point2D.Double(2, 3));
		
		List<Point2D> pntlistB = new ArrayList<Point2D>();
		for (Point2D a : pntlistA) {
			pntlistB.add(apply(Hreal, a, 0.1));
		}
		
		Point2D[] pntsA = pntlistA.toArray(new Point2D[0]);
		Point2D[] pntsB = pntlistB.toArray(new Point2D[0]);
		
		for (int i = 0; i < pntsA.length; i++) {
			Point2D a = pntsA[i];
			Point2D b = pntsB[i];
			System.out.format("(%.3f, %.3f) -> (%.3f, %.3f)\n", a.getX(), a.getY(), b.getX(), b.getY());
		}
		System.out.println();
		
		HomographyEstimator hest = new HomographyEstimator();
		RealMatrix H = hest.estimateHomography(pntsA, pntsB);
		
		MathUtil.print("H = ", H); System.out.println();
		
		for (Point2D a : pntlistA) {
			Point2D b = apply(H, a, 0);
			System.out.format("(%.3f, %.3f) -> (%.3f, %.3f)\n", a.getX(), a.getY(), b.getX(), b.getY());
		}
		
	}

}
