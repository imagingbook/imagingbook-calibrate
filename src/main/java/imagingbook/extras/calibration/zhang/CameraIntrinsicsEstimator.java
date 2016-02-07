package imagingbook.extras.calibration.zhang;

import ij.IJ;
import imagingbook.extras.calibration.zhang.util.MathUtil;
import imagingbook.lib.math.Matrix;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * @author WB
 *
 */

public class CameraIntrinsicsEstimator {
	
	// Version A (Zhang's original closed form solution)
	protected RealMatrix getIntrinsicsZhang1(RealMatrix[] homographies) {
		final int M = homographies.length;
		int rows = 2 * M;
		double[][] V = new double[rows][];

		for (int i = 0; i < M; i++) {
			RealMatrix H = homographies[i];
			V[2*i] = getVpq(H, 0, 1); // v01
			V[2*i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
		}

		if (M == 2) {
			V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
		}
		
		RealMatrix VM = MatrixUtils.createRealMatrix(V);
//		MathUtil.print("estimateIntrinsics: V = ", VM);//WB
		
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0
		
		final double vc 	= (b[1] * b[3] - b[0] * b[4]) / (b[0] * b[2] - b[1] * b[1]);
		final double lambda = b[5] - (b[3] * b[3] + vc * (b[1] * b[3] - b[0] * b[4])) / b[0];
		final double alpha 	= Math.sqrt(lambda / b[0]);
		final double beta 	= Math.sqrt(lambda * b[0] / (b[0] * b[2] - b[1] * b[1]));
		final double gamma 	= -b[1] * alpha * alpha * beta / lambda;
		final double uc 	= gamma * vc / alpha - b[3] * alpha * alpha / lambda;	// PAMI paper = WRONG!

		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
				{ alpha, gamma, uc },
				{ 0, beta, vc },
				{ 0, 0, 1 }
		});

		return A;
	}
	
	// Version B (Zhang's corrected closed form solution)
	protected RealMatrix getIntrinsicsZhang2(RealMatrix[] homographies) {
		final int M = homographies.length;
		int rows = 2 * M;
		double[][] V = new double[rows][];

		for (int i = 0; i < M; i++) {
			RealMatrix H = homographies[i];
			V[2*i] = getVpq(H, 0, 1); // v01
			V[2*i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
		}

		if (M == 2) {
			V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
		}
		
		RealMatrix VM = MatrixUtils.createRealMatrix(V);
//		MathUtil.print("estimateIntrinsics: V = ", VM);//WB
		
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0
		
		final double vc 	= (b[1] * b[3] - b[0] * b[4]) / (b[0] * b[2] - b[1] * b[1]);
		final double lambda = b[5] - (b[3] * b[3] + vc * (b[1] * b[3] - b[0] * b[4])) / b[0];
		final double alpha 	= Math.sqrt(lambda / b[0]);
		final double beta 	= Math.sqrt(lambda * b[0] / (b[0] * b[2] - b[1] * b[1]));
		final double gamma 	= -b[1] * alpha * alpha * beta / lambda;
		final double uc 	= gamma * vc / beta - b[3] * alpha * alpha / lambda;	// beta! 1998 report seems correct!

		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
				{ alpha, gamma, uc },
				{ 0, beta, vc },
				{ 0, 0, 1 }
		});

		return A;
	}
	
	// Version C (WB's closed form solution)
	protected RealMatrix getIntrinsicsZhang3(RealMatrix[] homographies) {
		final int M = homographies.length;
		int rows = 2 * M;
		double[][] V = new double[rows][];

		for (int i = 0; i < M; i++) {
			RealMatrix H = homographies[i];
			V[2*i] = getVpq(H, 0, 1); // v01
			V[2*i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
		}

		if (M == 2) {
			V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
		}
		
		RealMatrix VM = MatrixUtils.createRealMatrix(V);
//		MathUtil.print("estimateIntrinsics: V = ", VM);//WB
		
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0
		
		final double w = b[0]*b[2]*b[5] - b[1]*b[1]*b[5] - b[0]*b[4]*b[4] + 2*b[1]*b[3]*b[4] - b[2]*b[3]*b[3];
		final double d = 
				b[0] * b[2] - b[1] * b[1];
		final double uc = 
				(b[1] * b[4] - b[2] * b[3]) / d;
		final double vc = 
				(b[1] * b[3] - b[0] * b[4]) / d;
		final double alpha 	= 
//				Math.sqrt((b[0]*b[2]*b[5] - b[1]*b[1]*b[5] - b[0]*b[4]*b[4] + 2*b[1]*b[3]*b[4] - b[2]*b[3]*b[3]) / (b[0] * den));
				Math.sqrt(w / (d * b[0]));
		final double beta 	= 
//				Math.sqrt(b[0]*b[0]*b[2]*b[5] - b[0]*b[1]*b[1]*b[5] -b[0]*b[0]*b[4]*b[4] + 2*b[0]*b[1]*b[3]*b[4] - b[0]*b[2]*b[3]*b[3]) / den;
				Math.sqrt(w / (d * d) * b[0]);
		final double gamma 	= 
//				- b[1] * Math.sqrt(b[0]*b[0]*b[2]*b[5] - b[0]*b[1]*b[1]*b[5] -b[0]*b[0]*b[4]*b[4] + 2*b[0]*b[1]*b[3]*b[4] - b[0]*b[2]*b[3]*b[3]) / (b[0] * den);
				  Math.sqrt(w / (d * d * b[0])) * b[1];
		
//		final double lambda = // not required
//				b[5] - (b[0] * b[4] * b[4] - 2 * b[1] * b[3] * b[4] + b[2] * b[3] * b[3]) / denom;	//maxima
//		IJ.log("lambda = " + lambda);
		
		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
				{ alpha, gamma, uc },
				{ 0, beta, vc },
				{ 0, 0, 1 }
		});

		return A;
	}
	
	// Version D (WB, using Cholesky decomposition)
	protected RealMatrix getIntrinsics(RealMatrix[] homographies) {
		IJ.log("running getIntrinsics - Cholesky");
		final int M = homographies.length;
		int rows = 2 * M;
		double[][] V = new double[rows][];

		for (int i = 0; i < M; i++) {
			RealMatrix H = homographies[i];
			V[2*i] = getVpq(H, 0, 1); // v01
			V[2*i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
		}

		if (M == 2) {
			V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
		}
		
		RealMatrix VM = MatrixUtils.createRealMatrix(V);
//		MathUtil.print("estimateIntrinsics: V = ", VM);//WB
		
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0
		
		RealMatrix B = MatrixUtils.createRealMatrix(new double[][]
				{{b[0], b[1], b[3]},
				 {b[1], b[2], b[4]},
				 {b[3], b[4], b[5]}});
		System.out.println("B = " + B.toString());
		
		if (B.getEntry(0, 0) < 0 || B.getEntry(1, 1) < 0 || B.getEntry(2, 2) < 0) {	
			B = B.scalarMultiply(-1);	// make sure B is positive definite 
		}
		
		CholeskyDecomposition cd = new CholeskyDecomposition(B);
		RealMatrix L = cd.getL();
		RealMatrix A = MatrixUtils.inverse(L).transpose().scalarMultiply(L.getEntry(2, 2));
		return A;
	}

//	private double[] getVpq(RealMatrix H, int p, int q) {
//		H = H.transpose();
//		final double[] vij = new double[] {
//				H.getEntry(p, 0) * H.getEntry(q, 0),
//				H.getEntry(p, 0) * H.getEntry(q, 1) + H.getEntry(p, 1) * H.getEntry(q, 0),
//				H.getEntry(p, 1) * H.getEntry(q, 1),
//				H.getEntry(p, 2) * H.getEntry(q, 0) + H.getEntry(p, 0) * H.getEntry(q, 2),
//				H.getEntry(p, 2) * H.getEntry(q, 1) + H.getEntry(p, 1) * H.getEntry(q, 2),
//				H.getEntry(p, 2) * H.getEntry(q, 2)
//		};
//		return vij;
//	}
	
	// version without transpose
	private double[] getVpq(RealMatrix H, int p, int q) {
		final double[] vpq = new double[] {
				H.getEntry(0, p) * H.getEntry(0, q),
				H.getEntry(0, p) * H.getEntry(1, q) + H.getEntry(1, p) * H.getEntry(0, q),
				H.getEntry(1, p) * H.getEntry(1, q),
				H.getEntry(2, p) * H.getEntry(0, q) + H.getEntry(0, p) * H.getEntry(2, q),
				H.getEntry(2, p) * H.getEntry(1, q) + H.getEntry(1, p) * H.getEntry(2, q),
				H.getEntry(2, p) * H.getEntry(2, q)
		};
		return vpq;
	}
	
	
	
	/// OLD STUFF (why didnt it work?)
	
/*
	private RealMatrix estimateCameraIntrinsics(RealMatrix[] homographies) {
		if (params.assumeZeroSkew) {
			if (homographies.length < 2)
				throw new IllegalArgumentException(
						"At least 2 homographies are required");
		} else if (homographies.length < 3) {
			throw new IllegalArgumentException(
					"At least 3 homographies are required");
		}
		
		RealMatrix V = setupV(homographies, params.assumeZeroSkew);
		if (true) {
			MathUtil.print("getCameraIntrinsics: V = ", V);
		}
		
		RealVector b = MathUtil.getNullVector(V, true);	// solve V.b  =0
		if (true) {
			MathUtil.print("getCameraIntrinsics: b = ", b);	// result has inverted sign!!!
		}
		
		b.mapMultiplyToSelf(1 / Math.abs(b.getMaxValue()));
		if (params.beVerbose) {
			MathUtil.print("getCameraIntrinsics: b (normalized) = ", b);
		}
		RealMatrix A = makeIntrinsicMatrix(b, params.assumeZeroSkew);	// A is 2 x 3
		return A;
	}
	
	private RealMatrix makeIntrinsicMatrix(RealVector b, boolean noskew) {
		// b is of length 5 (noskew 0 true) or 6 (noskew = false)
		double B11, B12, B22, B13, B23, B33;
		if (noskew) {
			B11 = b.getEntry(0);
			B12 = 0;
			B22 = b.getEntry(1);
			B13 = b.getEntry(2);
			B23 = b.getEntry(3);
			B33 = b.getEntry(4);
		}
		else {
			B11 = b.getEntry(0);
			B12 = b.getEntry(1);
			B22 = b.getEntry(2);
			B13 = b.getEntry(3);
			B23 = b.getEntry(4);
			B33 = b.getEntry(5);
		}

		double temp0 = B12 * B13 - B11 * B23;	// see Zhangs report, Appendix B
		double temp1 = B11 * B22 - B12 * B12;

		double v0 = temp0 / temp1;
		double lambda = B33 - (B13 * B13 + v0 * temp0) / B11;	// WB: v0 * temp0 == 1/temp1 !!??
		
//		// Using abs() inside is an adhoc modification to make it more stable
//		// If there is any good theoretical reason for it, that's a pure accident.  Seems
//		// to work well in practice
		double alpha = Math.sqrt(Math.abs(lambda / B11));	// WB: abs() needed when noskew is turned on!!
//		double alpha = Math.sqrt(lambda / B11);
		double beta = Math.sqrt(Math.abs(lambda * B11 / temp1)); // abs() needed when noskew is turned on!!
//		double beta = Math.sqrt(lambda * B11 / temp1);
		double gamma = (noskew) ? 0 : -B12 * beta / B11;					// CHECK!!
		double u0 = gamma * v0 / alpha - B13 / B11;			// CHECK!!
//
		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
				{alpha, gamma, u0},
				{    0,  beta, v0},
				{    0,     0,  1}});
		return A;
	}
	
	
	private RealMatrix setupV(RealMatrix[] homographies, boolean noskew) {
		final int columns = (noskew) ? 5 : 6;
		RealMatrix A = MatrixUtils.createRealMatrix(2 * homographies.length, columns);
		
		for (int i = 0; i < homographies.length; i++) {	// TODO: change this loop and index!
			RealMatrix H = homographies[i];
			RealVector h1 = H.getColumnVector(0);
			RealVector h2 = H.getColumnVector(1);
			
			// normalize H by the max value to reduce numerical error when computing A
			// several numbers are multiplied against each other and could become quite large/small
			double maxVal = Math.max(h1.getMaxValue(), h2.getMaxValue());
			h1.mapMultiplyToSelf(1/maxVal);	// h1 <- h1 / maxVal
			h2.mapMultiplyToSelf(1/maxVal);	// h2 <- h2 / maxVal
			
			RealVector v11 = computeV(h1, h1, noskew);
			RealVector v12 = computeV(h1, h2, noskew);
			RealVector v22 = computeV(h2, h2, noskew);
			RealVector v11m22 = v11.subtract(v22);
			
			A.setRowVector(2 * i, v12);			//	CommonOps.insert(v12   , A, i*2  , 0);
			A.setRowVector(2 * i + 1, v11m22);	//	CommonOps.insert(v11m22, A, i*2+1, 0);
		}
		return A;
	}
	
	private RealVector computeV(RealVector h1, RealVector h2, boolean noskew) {
		final double h10 = h1.getEntry(0);	// TODO: clean/speed up!
		final double h11 = h1.getEntry(1);
		final double h12 = h1.getEntry(2);
		
		final double h20 = h2.getEntry(0);
		final double h21 = h2.getEntry(1);
		final double h22 = h2.getEntry(2);
		
		RealVector v = null;
		if (noskew) {
			v = new ArrayRealVector(5);
			v.setEntry(0, h10 * h20);
			v.setEntry(1, h11 * h21);
			v.setEntry(2, h12 * h20 + h10 * h22);
			v.setEntry(3, h12 * h21 + h11 * h22);
			v.setEntry(4, h12 * h22);
		}
		else {
			v = new ArrayRealVector(6);
			v.setEntry(0, h10 * h20);
			v.setEntry(1, h10 * h21 + h11 * h20);
			v.setEntry(2, h11 * h21);
			v.setEntry(3, h12 * h20 + h10 * h22);
			v.setEntry(4, h12 * h21 + h11 * h22);
			v.setEntry(5, h12 * h22);
		}
		return v;
	}
*/

}
