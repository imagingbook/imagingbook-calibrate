package imagingbook.calibration.zhang.util;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static imagingbook.lib.math.Arithmetic.EPSILON_DOUBLE;
import static imagingbook.lib.math.Arithmetic.isZero;

import static imagingbook.lib.math.Matrix.normL2;
import static imagingbook.lib.math.Matrix.zeroVector;
import static imagingbook.lib.math.Matrix.idMatrix;
import static imagingbook.lib.math.Matrix.add;
import static imagingbook.lib.math.Matrix.multiply;

import imagingbook.lib.math.Matrix;
import imagingbook.lib.settings.PrintPrecision;

public class RotationTests {

	public static void main(String[] args) {
		PrintPrecision.set(6);

		double[] axis = {0, -5, 0}; //{1,1,1};
		double theta = normL2(axis); //0.2;
		
		System.out.println("*** Rodrigues vector -> Rotation matrix:\n");
		
		double[] rv = makeRodriguesVector(axis, theta);
		System.out.println("original rv = " + Matrix.toString(rv));
		System.out.println();
		
		double[][] R1 = toRotationMatrix1(rv);
		System.out.println("R1 = \n" + Matrix.toString(R1));
		System.out.println();
		
		double[][] R2 = toRotationMatrix2(rv);
		System.out.println("R2 = \n" + Matrix.toString(R2));
		System.out.println();
		
		//-- going back:
		System.out.println("*** Rotation matrix -> Rodrigues vector:\n");
		
		double[] rv1 = toRodriguesVector1(R1);
		System.out.println("rv1 = " + Matrix.toString(rv1));
		System.out.println("theta1 = " + normL2(rv1));
		System.out.println();
		
		double[] rv2 = toRodriguesVector2(R1);
		System.out.println("rv2 = " + Matrix.toString(rv2));
		System.out.println("theta2 = " + normL2(rv2));
		System.out.println();
	}
	
	static double[] makeRodriguesVector(double[] axis, double theta) {
		double s = normL2(axis);
		return multiply(theta / s, axis);
	}
	
	// ++++++++++++++   Rodrigues vector --> Rotation matrix  +++++++++++++++++++
	
	/**
	 * Hand-made calculation (no library)
	 * @param rv Rodrigues rotation vector
	 * @return
	 */
	static double[][] toRotationMatrix1(double[] rv) {
		double theta = normL2(rv);
		double rx = rv[0] / theta;
		double ry = rv[1] / theta;
		double rz = rv[2] / theta;
		
		System.out.println("rotation angle1 = " + theta);
		System.out.println("rotation axis1 = " + Matrix.toString(new double[] {rx, ry, rz}));
		
		double[][] W = {
				{0, -rz, ry},
				{rz, 0, -rx},
				{-ry, rx, 0}};
		//System.out.println("W = \n" + Matrix.toString(W));
		double[][] I = idMatrix(3);
		double[][] R1 = add(I, multiply(Math.sin(theta), W));
		double[][] R2 = multiply(1 - Math.cos(theta), multiply(W,W));
		double[][] R = add(R1, R2);
		
		return R;
	}
	
	/**
	 * For comparison, this version uses Apache CM.
	 * @param rv
	 * @return
	 */
	static double[][] toRotationMatrix2(double[] rv) {
		double angle = normL2(rv);
		Vector3D axis = new Vector3D(rv);
		Rotation rotation = new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
		System.out.println("rotation angle2 = " + rotation.getAngle());
		System.out.println("rotation axis2 = " + rotation.getAxis(RotationConvention.VECTOR_OPERATOR));
		return rotation.getMatrix();
	}
	
	// ++++++++++++++   Rotation matrix --> Rodrigues vector +++++++++++++++++++
	
	/**
	 * from "`Vector Representation of Rotations"', Carlo Tomasi
	 * https://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.pdf
	 * Matlab code: http://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.m
	 * @param R
	 * @return
	 */
	static double[] toRodriguesVector1(double[][] R) {
		final double eps = EPSILON_DOUBLE;
		
		double[] p = {
				0.5 * (R[2][1] - R[1][2]), 
				0.5 * (R[0][2] - R[2][0]), 
				0.5 * (R[1][0] - R[0][1])};
		double s = normL2(p);
		double c = 0.5 * (Matrix.trace(R) - 1);
		
		if (isZero(s)) {                      // Rotation angle is either 0 or pi
			if (isZero(c - 1)) {       // Case 1: c = 1, Rotation angle is 0
				return zeroVector(3);
			}
			else if (isZero(c + 1)) {    // Case 2: c = -1, Rotation angle is pi
				// find the column of R + I with greatest norm (for better numerical results)
				double[][] Rp = add(R, idMatrix(3));
				double[] v = getMaxColumnVector(Rp);
				double vn = normL2(v);
				if (isZero(vn)) {   // this shouldn't really happen
					throw new RuntimeException("R is an inversion, not a rotation");
				}
				double[] u = multiply(1 / vn, v);	// unit vector
				return multiply(Math.PI, S(u));
			}
			else {                  // how can this be?
				throw new RuntimeException("sin(theta) is zero, bus cos(theta) is neither 1 nor -1!");
			}
		}
		else  {   // (s != 0) : rotation strictly between 0 and pi
			double[] u = multiply(1 / s, p);	// unit vector
			double theta = Math.atan2(s, c);
			return multiply(theta, u);
		}
	}
	
	/**
	 * Changes the sign of a unit vector u so that it is on the proper half of
	 * the unit sphere.
	 * @param x unit vector
	 * @return same or inverted unit vector
	 */
	private static double[] S(double[] x) {
//		if ((u[0] == 0 && u[1] == 0 && u[2] < 0) || (u[0] == 0 && u[1] < 0) || u[0] < 0) 
		if ((x[0] < 0) ||
			(isZero(x[0]) && x[1] < 0) ||
			(isZero(x[0]) && isZero(x[1]) && x[2] < 0))	{
			return multiply(-1, x);
		}
		else {
			return x;
		}
	}
	
//	private static boolean isZero(double x) {
//		return Math.abs(x) < EPSILON_DOUBLE;
//	}
	
	private static double[] getMaxColumnVector(double[][] A) {
		final int rows = A.length;
		final int cols = A[0].length;
		int maxCol = 0;
		double maxNorm = Double.NEGATIVE_INFINITY;
		for (int c = 0; c < cols; c++) {
			double csum = 0;
			for (int r = 0; r < rows; r++) {
				csum = csum + A[r][c] * A[r][c];
			}
			if (csum > maxNorm) {
				maxNorm = csum;
				maxCol = c;
			}
		}
		return Matrix.getColumn(A, maxCol);
	}
	
	// http://math.stackexchange.com/questions/83874/efficient-and-accurate-numerical-implementation-of-the-inverse-rodrigues-rotatio
	
	
	/**
	 * Correct!
	 * @param R
	 * @return
	 */
	static double[] toRodriguesVector2(double[][] R) {
		Rotation rot = new Rotation(R, 0.01);
		double angle = rot.getAngle();
		Vector3D axis = rot.getAxis( RotationConvention.VECTOR_OPERATOR);	
		double[] rv = axis.scalarMultiply(angle/axis.getNorm()).toArray();
		return rv;
	}
	


}
