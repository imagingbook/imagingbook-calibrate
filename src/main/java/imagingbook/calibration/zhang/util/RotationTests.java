package imagingbook.calibration.zhang.util;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import imagingbook.lib.math.Arithmetic;
import imagingbook.lib.math.Matrix;
import imagingbook.lib.settings.PrintPrecision;

public class RotationTests {

	public static void main(String[] args) {
		PrintPrecision.set(6);

		double[] axis = {1.5, 0.25, -0.5}; //{1,1,1};
		double theta = Matrix.normL2(axis); //0.2;
		
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
		System.out.println();
		
		double[] rv1 = toRodriguesVector1(R1);
		System.out.println("rv1 = " + Matrix.toString(rv1));
		System.out.println("theta1 = " + Matrix.normL2(rv1));
		System.out.println();
		
		double[] rv2 = toRodriguesVector2(R1);
		System.out.println("rv2 = " + Matrix.toString(rv2));
		System.out.println("theta2 = " + Matrix.normL2(rv2));
		System.out.println();
	}
	
	static double[] makeRodriguesVector(double[] axis, double theta) {
		double s = Matrix.normL2(axis);
		return Matrix.multiply(theta / s, axis);
	}
	
	// ++++++++++++++   Rodrigues vector --> Rotation matrix  +++++++++++++++++++
	
	/**
	 * Hand-made calculation (no library)
	 * @param rv Rodrigues rotation vector
	 * @return
	 */
	static double[][] toRotationMatrix1(double[] rv) {
		double theta = Matrix.normL2(rv);
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
		double[][] I = Matrix.createIdentityMatrix(3);
		double[][] R1 = Matrix.add(I, Matrix.multiply(Math.sin(theta), W));
		//double[][] R2 = Matrix.multiply(1 - Math.cos(theta),Matrix.multiply(Matrix.transpose(W),W));
		double[][] R2 = Matrix.multiply(1 - Math.cos(theta),Matrix.multiply(W,W));
		double[][] R = Matrix.add(R1, R2);
		
		return R;
	}
	
	/**
	 * For comparison, this version uses Apache CM.
	 * @param rv
	 * @return
	 */
	static double[][] toRotationMatrix2(double[] rv) {
		double angle = Matrix.normL2(rv);
		Vector3D axis = new Vector3D(rv);
		Rotation rotation = new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
		System.out.println("rotation angle3 = " + rotation.getAngle());
		System.out.println("rotation axis3 = " + rotation.getAxis(RotationConvention.VECTOR_OPERATOR));
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
		final double eps = Arithmetic.EPSILON_DOUBLE;
		final double small = Math.sqrt(eps);
		final double tiny = eps * 100;
		
		double[] rho = {
				0.5 * (R[2][1] - R[1][2]), 
				0.5 * (R[0][2] - R[2][0]), 
				0.5 * (R[1][0] - R[0][1])};
		double sn = Matrix.normL2(rho);
		double cs = (Matrix.trace(R) - 1) / 2;
		
		if (sn < eps) {                      // Rotation angle is either 0 or pi
			if (Math.abs(cs - 1) < tiny) {       // c = 1, Rotation angle is 0
				return new double[] {0, 0, 0};
			}
			else if (Math.abs(cs + 1) < tiny) {    // c = -1, Rotation angle is pi
				// Find the column of R + I with greatest norm (for better numerical results)
				//		            Rp = R + eye(3);
				double[][] Rp = Matrix.add(R, Matrix.createIdentityMatrix(3));
				//		            colNorm2 = diag(Rp'*Rp);
				//		            [val, col] = max(colNorm2);
				double[] v = getMaxColumnVector(Rp);
				double val = Matrix.normL2(v);
				if (val < small) {         // Shouldn't really happen: R == -eye(3)
					throw new RuntimeException("R is an inversion, not a rotation");
				}
				//		            v = Rp(:, col);
				//		            u = v / norm(v);
				//		            rOut = Math.PI * hemisphere(u);
				double[] u = Matrix.multiply(1 / val, v);
				return Matrix.multiply(Math.PI, S(u));
			}
			else {                        // How can this be?
				throw new RuntimeException("sin(theta) is zero, bus cos(theta) is neither 1 nor -1!");
			}
		}
		else  {                          // Rotation strictly between 0 and pi
			double[] u = Matrix.multiply(1 / sn, rho);
			double theta = Math.atan2(sn, cs);
			return Matrix.multiply(theta, u);
		}
	}
	
	/**
	 * Changes the sign of a unit vector u so that it is on the proper half of
	 * the unit sphere.
	 * @param u unit vector
	 * @return same or inverted unit vector
	 */
	private static double[] S(double[] u) {
//		if ((u[0] == 0 && u[1] == 0 && u[2] < 0) || (u[0] == 0 && u[1] < 0) || u[0] < 0) 
		if ((isZero(u[0]) && isZero(u[1]) && u[2] < 0) || (isZero(u[0]) && u[1] < 0) || u[0] < 0)
		{
			return Matrix.multiply(-1, u);
		}
		else {
			return u;
		}
	}
	
	private static boolean isZero(double x) {
		return Math.abs(x) < Arithmetic.EPSILON_DOUBLE;
	}
	
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
