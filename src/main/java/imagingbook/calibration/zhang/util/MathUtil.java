package imagingbook.calibration.zhang.util;

import java.awt.geom.Point2D;
import java.util.Locale;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * Utility math methods used for camera calibration.
 * @author WB
 *
 */
public class MathUtil {
	
	static {
		Locale.setDefault(Locale.US);	// TODO: should this be here?
	}
	
	@Deprecated
	public static void print(String name, RealMatrix M) {
		System.out.println(name);
		for (int r = 0; r < M.getRowDimension(); r++) {
			RealVector row = M.getRowVector(r);
			System.out.println("  " + r + " " + row.toString());
		}
	}
	
	@Deprecated
	public static void print(String name, RealVector v) {
		System.out.println(name + v.toString());
	}
	
	// -------------------------------------------------------
	
	public static double[] toArray(Point2D p) {
		return new double[] {p.getX(), p.getY()};
	}
	
	public static Point2D.Double toPoint2D(double[] xy) {
		return new Point2D.Double(xy[0], xy[1]);
	}
	
	public static RealVector toRealVector(Point2D p) {
		return toRealVector(p, false);
	}
	
	public static RealVector toRealVector(Point2D p, boolean makeHomogeneous) {
		if ( makeHomogeneous) {
			return MatrixUtils.createRealVector(new double[] {p.getX(), p.getY(), 1});
		}
		else {
			return MatrixUtils.createRealVector(toArray(p));
		}
	}
	
	public static RealVector[] getColumnVectors(RealMatrix M) {
		final int ncols = M.getColumnDimension();
		RealVector[] colVecs = new  RealVector[ncols];
		for (int col = 0; col < ncols; col++) {
			colVecs[col] = M.getColumnVector(col);
		}
		return colVecs;
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
	
	public static double det(RealMatrix M) {
		return new LUDecomposition(M).getDeterminant();
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


//	/**               
//	 * Finds a nontrivial solution (x) to the homogeneous linear system M . x = 0.
//	 * @param M	
//	 * @param fromRight
//	 * @return
//	 */
//	@Deprecated 	// use the simpler version below
//	public static RealVector solveHomogeneousSystemOLD(RealMatrix M, boolean fromRight) {
//		SingularValueDecomposition svd = new SingularValueDecomposition(M);
//		RealMatrix U = svd.getU();
//		RealMatrix V = svd.getV();
//		
//		// dimensions of the original (decomposed) matrix;
//		int svdnumRows = U.getRowDimension();
//		int svdnumCols = V.getColumnDimension();
//		double[] s = svd.getSingularValues();
//		
//		RealMatrix A = fromRight ? svd.getVT() /*V is transposed!*/ : svd.getU();
//		      
//		// find the row/column index of the smallest singular value (diagonal)
//		int minIndex = -1;
//		if (fromRight && svdnumCols > svdnumRows)
//			minIndex = svdnumCols - 1;
//		else if (!fromRight && svdnumCols < svdnumRows)
//			minIndex = svdnumRows - 1;
//		else {
//			// find the index of the smallest singular value
//			double minValue = Double.MAX_VALUE;
//			for (int i = 0; i < s.length; i++) {
//				if (s[i] < minValue) {
//					minValue = s[i];
//					minIndex = i;
//				}
//			}
//		}
//		//System.out.println("nullspace: smallestIndex = " + minIndex);
//		
//		RealVector nullVec = fromRight ? A.getRowVector(minIndex) : A.getColumnVector(minIndex);
//		return nullVec;
//	}
	
	
	/**               
	 * Finds a nontrivial solution (x) to the homogeneous linear system M . x = 0
	 * by singular-value decomposition. If M has more rows than columns, the
	 * system of equations is overdetermined. In this case the returned solution
	 * minimizes the residual ||M . x|| in the least-squares sense.
	 * 
	 * @param A	the original matrix.
	 * @return the solution vector x.
	 */
	public static RealVector solveHomogeneousSystem(RealMatrix A) {
		SingularValueDecomposition svd = new SingularValueDecomposition(A);
		RealMatrix V = svd.getV();
		RealVector x = V.getColumnVector(V.getColumnDimension() - 1);
		return x;
	}
	
	public static double[] toHomogeneous(double[] cvec) {
		double[] hvec = new double[cvec.length + 1];
		for (int i = 0; i < cvec.length; i++) {
			hvec[i] = cvec[i];
			hvec[hvec.length - 1] = 1;
		}
		return hvec;
	}
	
	public static double[] toCartesian(double[] hvec) {
		double[] cvec = new double[hvec.length - 1];
		final double s = 1 / hvec[hvec.length - 1];	// TODO: check for zero factor
		for (int i = 0; i < hvec.length - 1; i++) {
			cvec[i] = s * hvec[i];
		}
		return cvec;
	}
	
//	/**  
//	 * implements a left (pre-) matrix-vector multiplication:  A . x -> y
//	 * Matrix A is of size (m,n), column vector x is of length n.
//	 * The result y is a vector of length m.
//	 * non-destructive
//	 */
//	public static double[] multiply(final double[][] A, final double[] x) {
//		double[] y = new double[A.length];
//		multiplyD(A, x, y);
//		return y;
//	}
	
//	// destructive
//	public static void multiplyD(final double[][] A, final double[] x, double[] y) {
//		final int m = A.length;
//		final int n = A[0].length;
//		if (x.length != n || y.length != m) 
//			throw new IllegalArgumentException("incompatible matrix-vector dimensions");
//		for (int i = 0; i < m; i++) {
//			double s = 0;
//			for (int j = 0; j < n; j++) {
//				s = s + A[i][j] * x[j];
//			}
//			y[i] = s;
//		}
//	}
	
	public static String getInfo(double[][] A) {
		return String.format("Matrix: rows=%d, columns=%d", A.length, A[0].length);
	}
	
	public static String getInfo(double[] x) {
		return String.format("Vector: dimension=%d", x.length);
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
	 * 
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
	
	
	// Conversions between Double[] and double[]
	
//	// We want to pass a 'Deque' instance, thus not just 'List' parameter!
//	public static double[] toArray(Collection<Double> c) {
//		double[] model = new double[c.size()];
//		int i = 0;
//		for (Double x : c) {
//			System.out.format("Param %d = %.6f\n", i, x);
//			if (x != null) {
//				model[i] = x.doubleValue();
//				i++;
//			}
//		}
//		return model;
//	}
	
//	public static Deque<Double> toList(double[] a) {
//		Deque<Double> A = new ArrayDeque<Double>(a.length);
//		for (int i = 0; i < a.length; i++) {
//			A.add(a[i]);
//		}
//		return A;
//	}
	
	// ---------------------------------------------------------------
	
	public static Quaternion toQuaternion(Rotation r) {
		return new Quaternion(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
	}
	
	public static Rotation toRotation(Quaternion q) {
		return new Rotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), true);
	}
	
	// ---------------------------------------------------------------
	
	/**
	 * Calculates the 'inverse condition number' (RCOND() in Matlab)
	 * of the given matrix (0,...,1, ideally close to 1).
	 * @param Ma the matrix
	 * @return the inverse condition number
	 */
	public static double inverseConditionNumber(double[][] Ma) {
		RealMatrix M = new Array2DRowRealMatrix(Ma);
		SingularValueDecomposition svd = new SingularValueDecomposition(M);
		return svd.getInverseConditionNumber();
	}
	
	
	// ---------------------------------------------------------------
	
	/**
	 * For testing only.
	 * @param args ignored
	 */
	public static void main (String[] args) {
		//double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}};
		double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}, {-3, 7, 2}};
		{
			RealMatrix AM = MatrixUtils.createRealMatrix(A);
			RealVector x = solveHomogeneousSystem(AM);
			System.out.println("Solution x = " + x.toString());
		}
		// Solution x = {0.649964237; -0.7338780288; 0.1974070146}
	}
	

}
