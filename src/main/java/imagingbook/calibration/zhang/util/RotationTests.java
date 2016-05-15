package imagingbook.calibration.zhang.util;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;

import imagingbook.lib.math.Matrix;
import imagingbook.lib.settings.PrintPrecision;

public class RotationTests {

	public static void main(String[] args) {
		PrintPrecision.set(6);

		double[] axis = {-2.5, 0.5, 0.5}; //{1,1,1};
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
		
		double[] rv1 = toRodriguesVector0(R1);
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
	 * For comparison, this version uses Apchache CM.
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
	 * NOT CORRECT!!
	 * @param R
	 * @return
	 */
	static double[] toRodriguesVector0(double[][] R) {
		double tr = Matrix.trace(R);
		double[] RR = {
				R[1][2] - R[2][1], 
				R[2][0] - R[0][2], 
				R[0][1] - R[1][0]};
		double[] rv = Matrix.multiply(-2.0 / (1.0 + tr), RR);
		return rv;
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
