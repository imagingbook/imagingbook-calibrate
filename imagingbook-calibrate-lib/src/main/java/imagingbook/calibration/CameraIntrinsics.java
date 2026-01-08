/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.math.Matrix;

import org.apache.commons.math4.legacy.linear.CholeskyDecomposition;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;

/**
 * This class defines methods for estimating the intrinsic camera parameters from multiple homographies. Alternative
 * versions are provided (only one is actually used though).
 * @author WB
 */
public abstract class CameraIntrinsics {

    /**
     * Estimates intrinsic camera parameters from multiple homographies.
     * @param homographies a set of homography matrices
     * @return the estimated 3 x 3 intrinsic transformation matrix
     */
    public static RealMatrix from(RealMatrix[] homographies) {
        return getCameraIntrinsics(homographies);
		// return getCameraIntrinsicsZhang1(homographies);
		// return getCameraIntrinsicsZhang2(homographies);
		// return getCameraIntrinsicsZhang3(homographies);
    }

	/**
	 * Version 1 (Zhang's original closed form solution). Estimates intrinsic camera parameters from multiple
	 * homographies.
	 *
	 * @param homographies a set of homography matrices
	 * @return the estimated 3 x 3 intrinsic transformation matrix
	 */
	private static RealMatrix getCameraIntrinsicsZhang1(RealMatrix[] homographies) {
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

	/**
	 * Version 2 (Zhang's corrected closed form solution). Estimates intrinsic camera parameters from multiple
	 * homographies.
	 *
	 * @param homographies a set of homography matrices
	 * @return the estimated 3 x 3 intrinsic transformation matrix
	 */
	private static RealMatrix getCameraIntrinsicsZhang2(RealMatrix[] homographies) {
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

	/**
	 * Version 3 (WB's closed form solution). Estimates intrinsic camera parameters from multiple homographies.
	 *
	 * @param homographies a set of homography matrices
	 * @return the estimated 3 x 3 intrinsic transformation matrix
	 */
    private static RealMatrix getCameraIntrinsicsZhang3(RealMatrix[] homographies) {
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
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0
		
		final double w = b[0]*b[2]*b[5] - b[1]*b[1]*b[5] - b[0]*b[4]*b[4] + 2*b[1]*b[3]*b[4] - b[2]*b[3]*b[3];
		final double d = 
				b[0] * b[2] - b[1] * b[1];
		final double uc = 
				(b[1] * b[4] - b[2] * b[3]) / d;
		final double vc = 
				(b[1] * b[3] - b[0] * b[4]) / d;
		final double alpha 	= 
				Math.sqrt(w / (d * b[0]));
		final double beta 	= 
				Math.sqrt(w / (d * d) * b[0]);
		final double gamma 	= 
				  Math.sqrt(w / (d * d * b[0])) * b[1];
		
		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
				{ alpha, gamma, uc },
				{ 0, beta, vc },
				{ 0, 0, 1 }
		});

		return A;
	}


	/**
	 * Final version by WB (this version is used by default). Estimates intrinsic camera parameters from multiple
	 * homographies using a Cholesky decomposition.
	 *
	 * @param homographies a set of homography matrices
	 * @return the estimated 3 x 3 intrinsic transformation matrix
	 */
    private static RealMatrix getCameraIntrinsics( RealMatrix[] homographies) {
		final int M = homographies.length;
		int rows = 2 * M + 1;
		double[][] V = new double[rows][];

		for (int i = 0; i < M; i++) {
			RealMatrix Hi = homographies[i];
			V[2*i + 0] = getVpq(Hi, 0, 1); // v01
			V[2*i + 1] = Matrix.subtract(getVpq(Hi, 0, 0), getVpq(Hi, 1, 1)); // v00-v11
		}
		// impose skewless constraint (gamma = 0) by default
		V[rows - 1] = new double[] { 0, 1, 0, 0, 0, 0 };

		// if (M == 2) {
		// 	V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
		// }

		RealMatrix VM = MatrixUtils.createRealMatrix(V);
		double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0

		// *************************************************************************************
		System.out.println("\nhom. solution b = " + Matrix.toString(b));

		System.out.println("\nVM = " + Matrix.toString(VM));
		SingularValueDecomposition svd = new SingularValueDecomposition(VM);
		System.out.println("\nsingular vals = " + Matrix.toString(svd.getSingularValues()));
		System.out.println("\ndecomp V = " + Matrix.toString(svd.getV()));
		// *************************************************************************************
		// {
		// 	double v0 = (b[1] * b[3] - b[0] * b[4]) / (b[0] * b[2] - b[1] * b[1]);
		// 	double lamda = b[5] - (b[3] * b[3] + v0 * (b[1] * b[3] - b[0] * b[4])) / b[0];
		// 	double alpha = Math.sqrt(lamda / b[0]);
		// 	double beta = Math.sqrt(lamda * b[0] / (b[0] * b[2] - b[1] * b[1]));
		// 	double gamma = -b[1] * alpha * alpha * beta / lamda;
		// 	double u0 = gamma * v0 / beta - b[3] * alpha * alpha / lamda;
		// 	RealMatrix Ainit = MatrixUtils.createRealMatrix(new double[][] {
		// 			{ alpha, gamma, u0 },
		// 			{ 0, beta, v0 },
		// 			{ 0, 0, 1 }
		// 	});
		//
		// 	System.out.println("\nAiniz = " + Matrix.toString(Ainit));
		// }

		//------------------------------------------

		
		RealMatrix B = MatrixUtils.createRealMatrix(new double[][]
				{{b[0], b[1], b[3]},
				 {b[1], b[2], b[4]},
				 {b[3], b[4], b[5]}});

		System.out.println("\nB = " + Matrix.toString(B));

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
	private static double[] getVpq(RealMatrix H, int p, int q) {
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

}
