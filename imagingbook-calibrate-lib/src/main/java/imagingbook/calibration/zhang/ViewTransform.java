package imagingbook.calibration.zhang;

import java.io.StringWriter;
import java.util.Arrays;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import imagingbook.common.math.Matrix;

/**
 * Instances of this class represent extrinsic camera (view) parameters.
 * @author WB
 *
 */
public class ViewTransform {
	
	private static final double OrthogonalityThreshold = 0.01;
	
	private final Rotation rotation;
	private final double[] translation;
	
	// ----------------------------------------------------------------------------------
	
	public ViewTransform() {
		this.rotation = Rotation.IDENTITY;
		this.translation = new double[3];
	}
	
	public ViewTransform(double rX, double rY, double rZ, double tX, double tY, double tZ) {
		this.rotation = makeRotation(new double[] {rX, rY, rZ});
		this.translation = new double[] {tX, tY, tZ};
	}
	
	public ViewTransform(Rotation rot, double[] t) {
		this.rotation = rot;
		translation = t;
	}
	
	public ViewTransform(RealMatrix RT) {	// RT is of size 3 x 4 (a homography)
		if (RT.getRowDimension() != 3 || RT.getColumnDimension() != 4) {
			throw new IllegalArgumentException("View transform matrix must be 3 x 4");
		}
		RealMatrix R = RT.getSubMatrix(0, 2, 0, 2);
		rotation = new Rotation(R.getData(), OrthogonalityThreshold);
		translation = RT.getColumnVector(3).toArray();
	}
	
	public ViewTransform(RealMatrix R, RealVector t) {	// R is of size 3 x 3 , t of size 3 x 1
		this(new Rotation(R.getData(), OrthogonalityThreshold), t.toArray());
//		if (R.getRowDimension() != 3 || R.getColumnDimension() != 3) {
//			throw new IllegalArgumentException("Rotation matrix must be 3 x 3");
//		}
//		if (t.getDimension() != 3) {
//			throw new IllegalArgumentException("Translation vector must be of dimension 3");
//		}
//		rotation = new Rotation(R.getData(), OrthogonalityThreshold);
//		translation = t.toArray();
	}
	
	public ViewTransform(double[] w) {
		this.rotation = makeRotation(w);
		this.translation = Arrays.copyOfRange(w, 3, 6);
	}
	
	// ----------------------------------------------------------------------------------
	
	private Rotation makeRotation(double[] w) {
		Vector3D axis = new Vector3D(w[0], w[1], w[2]);
		double angle = axis.getNorm();
		//return new Rotation(axis, angle);
		return new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
	}
	
	protected double[] getParameters() {
		//double[] rotAxis = rotation.getAxis().toArray();
		double[] rotAxis = rotation.getAxis(RotationConvention.VECTOR_OPERATOR).toArray();
		double rotAngle = rotation.getAngle();
		return new double[] {
			rotAxis[0] * rotAngle,
			rotAxis[1] * rotAngle,
			rotAxis[2] * rotAngle,
			translation[0], translation[1], translation[2]};
	}
	
	public Rotation getRotation() {
		return rotation;
	}
	
	public double[] getRotationAxis() {
		//double[] rotAxis = rotation.getAxis().toArray();
		double[] rotAxis = rotation.getAxis(RotationConvention.VECTOR_OPERATOR).toArray();
		double rotAngle = rotation.getAngle();
		rotAxis[0] *= rotAngle;
		rotAxis[1] *= rotAngle;
		rotAxis[2] *= rotAngle;
		return rotAxis;
	}
	
	public RealMatrix getRotationMatrix() {
		double[][] R = rotation.getMatrix();
		return MatrixUtils.createRealMatrix(R);
	}
	
	public double[] getTranslation() {
		return translation;
	}
	
	public RealVector getTranslationVector() {
		return MatrixUtils.createRealVector(translation);
	}
	
	// ----------------------------------------------------------------------------------
	
	/**
	 * Moves point XYZ from 3D world coordinates to 3D camera coordinates,
	 * as specified by the transformations of this view.
	 * No projection is involved.
	 * @param XYZ a 3D (world) point
	 * @return the given world point mapped to 3D camera coordinates
	 */
	protected double[] applyTo(double[] XYZ) {	// 3D vector XYZ assumed
		double[] XYZc = new double[3];
		rotation.applyTo(XYZ, XYZc);
		for (int i = 0; i < 3; i++) {
			XYZc[i] = XYZc[i] + translation[i];
		}
		return XYZc;
	}
	
	public String toString() {
		RealMatrix R = this.getRotationMatrix();
		RealVector T = this.getTranslationVector();
		StringWriter writer = new StringWriter();
		writer.append("R = \n");
		writer.append(Matrix.toString(R.getData()));
		writer.append("\n");
		writer.append("T = ");
		writer.append(Matrix.toString(T.toArray()));
		return writer.toString();
	}
	
	// ------------------------------------------------------------------

/*	public static void main(String[] args) {
		double[] w1 = {1.2, -0.5, 0.9, 10, 20, 30};
		System.out.println("w1 = " + Matrix.toString(w1));
		ViewTransform view = new ViewTransform(w1);
		double[] w2 = view.getParameters();
		System.out.println("w2 = " + Matrix.toString(w2));
	}*/

	
}
