/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.util;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.exception.DivideByZeroException;
import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * Utility math methods used for camera calibration.
 *
 * @author WB
 * @version 2022/12/19
 */
public abstract class MathUtil {

	private MathUtil() {}

	public static double[] toArray(Pnt2d p) {
		return new double[] {p.getX(), p.getY()};
	}
	
	public static Pnt2d toPnt2d(double[] xy) {
		return Pnt2d.from(xy);
	}
	
	public static RealVector crossProduct3x3(RealVector A, RealVector B) {
		final double[] a = A.toArray();
		final double[] b = B.toArray();
		final double[] c = {
				a[1] * b[2] - b[1] * a[2],
				a[2] * b[0] - b[2] * a[0],
				a[0] * b[1] - b[0] * a[1]
		};
		return MatrixUtils.createRealVector(c);
	}
	
	public static RealVector getRowPackedVector(RealMatrix A) {
		double[][] AA = A.getData();
		double[] V = new double[AA.length * AA[0].length];
		int k = 0;
		for (int i = 0; i < AA.length; i++) {
			for (int j = 0; j < AA[0].length; j++) {
				V[k++] = AA[i][j];
			}
		}
		return MatrixUtils.createRealVector(V);
	}
	
	public static RealMatrix fromRowPackedVector(RealVector V, int rows, int columns) {
		double[][] AA = new double[rows][columns];
		double[] data = V.toArray();
		int k = 0;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				AA[i][j] = data[k++];
			}
		}
		return MatrixUtils.createRealMatrix(AA);
	}

	/**
	 * Finds a nontrivial solution (x) to the homogeneous linear system A . x = 0 by singular-value decomposition. If A
	 * has more rows than columns, the system of equations is overdetermined. In this case the returned solution
	 * minimizes the residual ||A . x|| in the least-squares sense.
	 *
	 * @param A the original matrix.
	 * @return the solution vector x.
	 */
	public static RealVector solveHomogeneousSystem(RealMatrix A) {
		SingularValueDecomposition svd = new SingularValueDecomposition(A);
		RealMatrix V = svd.getV();
		// RealVector x = V.getColumnVector(V.getColumnDimension() - 1);
		// return x;
		int minIdx = Matrix.idxMin(svd.getSingularValues());
		return V.getColumnVector(minIdx);
	}

	/**
	 * Converts a Cartesian vector to an equivalent homogeneous vector by attaching an additional 1-element. The
	 * resulting homogeneous vector is one element longer than the specified Cartesian vector. See also
	 * {@link #toCartesian(double[])}.
	 *
	 * @param ac a Cartesian vector
	 * @return an equivalent homogeneous vector
	 */
	public static double[] toHomogeneous(double[] ac) {
		double[] xh = new double[ac.length + 1];
		for (int i = 0; i < ac.length; i++) {
			xh[i] = ac[i];
			xh[xh.length - 1] = 1;
		}
		return xh;
	}

	/**
	 * Converts a homogeneous vector to its equivalent Cartesian vector, which is one element shorter. See also
	 * {@link #toHomogeneous(double[])}.
	 *
	 * @param ah a homogeneous vector
	 * @return the equivalent Cartesian vector
	 * @throws DivideByZeroException if the last vector element is zero
	 */
	public static double[] toCartesian(double[] ah) throws DivideByZeroException {
		double[] xc = new double[ah.length - 1];
		final double s = 1 / ah[ah.length - 1];
		if (!Double.isFinite(s))	// isZero(s)
			throw new DivideByZeroException();
		for (int i = 0; i < ah.length - 1; i++) {
			xc[i] = s * ah[i];
		}
		return xc;
	}
	
	public static double mean(double[] x) {
		final int n = x.length;
		if (n == 0) 
			return 0;
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum = sum + x[i];
		}
		return sum / n;
	}
	
	/**
	 * Returns the variance of the specified values.
	 * @param x a sequence of real values 
	 * @return the variance of the values in x (sigma^2)
	 */
	public static double variance(double[] x) {
		final int n = x.length;
		if (n == 0) 
			return 0;
		double sum = 0;
		double sum2 = 0;
		for (int i = 0; i < x.length; i++) {
			sum = sum + x[i];
			sum2 = sum2 + x[i] * x[i];
		}
		return (sum2 - (sum * sum) / n) / n;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Converts a {@link Rotation} to a {@link Quaternion}.
	 * @param R a rotation
	 * @return the corresponding quaternion
	 */
	public static Quaternion toQuaternion(Rotation R) {
		return new Quaternion(R.getQ0(), R.getQ1(), R.getQ2(), R.getQ3());
	}

	/**
	 * Converts a {@link Quaternion} to a {@link Rotation}.
	 * @param q a quaternion
	 * @return the associated rotation
	 */
	public static Rotation toRotation(Quaternion q) {
		return new Rotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), true);
	}

    /**
     * Linearly interpolate two 3D {@link Rotation} instances.
     * @param R0 first rotation
     * @param R1 second rotation
     * @param alpha the blending factor in [0,1]
     * @return the interpolated rotation
     */
    public static Rotation Lerp(Rotation R0, Rotation R1, double alpha) {
        Quaternion qa = toQuaternion(R0);
        Quaternion qb = toQuaternion(R1);
        return toRotation(Lerp(qa, qb, alpha));
    }

    /**
     * Linearly interpolate two {@link Quaternion} instances.
     * @param Q0 first quaternion
     * @param Q1 second quaternion
     * @param alpha the blending factor in [0,1]
     * @return the interpolated quaternion
     */
    public static Quaternion Lerp(Quaternion Q0, Quaternion Q1, double alpha) {
        return Quaternion.add(Q0.multiply(1 - alpha), Q1.multiply(alpha));
    }

    /**
     * Linearly interpolate two 3D translation vectors.
     * @param t0 first translation vector
     * @param t1 second translation vector
     * @param alpha the blending factor in [0,1]
     * @return the interpolated translation vector
     */
    public static double[] Lerp(double[] t0, double[] t1, double alpha) {
        double[] t01 = t0.clone();
        for (int i = 0; i < t01.length; i++) {
            t01[i] = (1 - alpha) * t0[i] + alpha * t1[i];
        }
        return t01;
    }

	// ---------------------------------------------------------------

	/**
	 * Calculates the 'inverse condition number' (RCOND() in Matlab)
	 * of the given matrix (0,...,1, ideally close to 1).
	 * @param A the matrix
	 * @return the inverse condition number
	 */
	public static double inverseConditionNumber(double[][] A) {
		RealMatrix M = new Array2DRowRealMatrix(A);
		SingularValueDecomposition svd = new SingularValueDecomposition(M);
		return svd.getInverseConditionNumber();
	}


    // ---------------------------------------------------------------
	
	// /**
	//  * For testing only.
	//  * @param args ignored
	//  */
	// public static void main (String[] args) {
	// 	//double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}};
	// 	double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}, {-3, 7, 2}};
	// 	{
	// 		RealMatrix AM = MatrixUtils.createRealMatrix(A);
	// 		RealVector x = solveHomogeneousSystem(AM);
	// 		System.out.println("Solution x = " + x.toString());
	// 	}
	// 	// Solution x = {0.649964237; -0.7338780288; 0.1974070146}
	// }
}
