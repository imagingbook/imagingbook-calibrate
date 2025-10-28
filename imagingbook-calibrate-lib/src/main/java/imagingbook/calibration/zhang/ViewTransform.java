/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.util.Rotations;
import imagingbook.common.math.Matrix;

import org.apache.commons.geometry.euclidean.threed.AffineTransformMatrix3D;
import org.apache.commons.geometry.euclidean.threed.rotation.QuaternionRotation;
// import org.apache.commons.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.numbers.quaternion.Quaternion;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * Instances of this class represent extrinsic camera (view) parameters.
 *
 * @author WB
 */
public class ViewTransform {
	
	private static final double OrthogonalityThreshold = 0.01;
	private final QuaternionRotation rotation;
	private final double[] translation;
	
	// ----------------------------------------------------------------------------------
	
	public ViewTransform() {
		this.rotation = QuaternionRotation.identity(); // Rotation3D.IDENTITY;
		this.translation = new double[3];
	}

    /**
     * Creates a 3D ViewTransform from the 3 elements of a rotation (Rodrigues) vector
     * and the 3 elements of a translation vector.
     * @param rX rotation vector x
     * @param rY rotation vector y
     * @param rZ rotation vector z
     * @param tX translation vector x
     * @param tY translation vector y
     * @param tZ translation vector z
     */
	public ViewTransform(double rX, double rY, double rZ, double tX, double tY, double tZ) {
		this.rotation = makeRotation(new double[] {rX, rY, rZ});
		this.translation = new double[] {tX, tY, tZ};
	}

    /**
     * Creates a 3D ViewTransform from a vector w = (rX, rY, rZ, tX, tY, tY).
     * @param w
     */
    public ViewTransform(double[] w) {
        // this.rotation = makeRotation(w);
        // this.translation = Arrays.copyOfRange(w, 3, 6);
        this(w[0], w[1], w[2], w[3], w[4], w[5]);
    }

    /**
     * Creates a 3D ViewTransform from a QuaternionRotation and a
     * 3D translation vector.
     * @param qr rotation quaternion
     * @param t translation vector
     */
	public ViewTransform(QuaternionRotation qr, RealVector t) {
		this.rotation = qr;
		translation = t.toArray();
	}

    /**
     * Creates a 3D ViewTransform from  a 3x4 homography matrix.
     * @param RT homography matrix
     */
	public ViewTransform(RealMatrix RT) {	// RT is of size 3 x 4 (a homography)
		if (RT.getRowDimension() != 3 || RT.getColumnDimension() != 4) {
			throw new IllegalArgumentException("View transform matrix must be 3 x 4");
		}
		RealMatrix R = RT.getSubMatrix(0, 2, 0, 2);
        this.rotation = Rotations.makeRotation(R, Rotations.DefaultOrthogonalityThreshold);
		this.translation = RT.getColumnVector(3).toArray();
	}

    /**
     * Creates a 3D ViewTransform from a 3x3 rotation matrix and a
     * 3D translation vector.
     * @param R 3x3 rotation matrix
     * @param t 3D translation vector
     */
	public ViewTransform(RealMatrix R, RealVector t) {	// R is of size 3 x 3 , t of size 3 x 1
        // alternatively check private method QuaternionRotation.orthogonalRotationMatrixToQuaternion(..)
		this(Rotations.makeRotation(R, 0.01), t);
	}
	
	// ----------------------------------------------------------------------------------
	
	private QuaternionRotation makeRotation(double[] r) {
		Vector3D axis = Vector3D.of(r[0], r[1], r[2]);
		double angle = axis.norm();
        // double angle = axis.getNorm();
		//return new Rotation(axis, angle);
		//return new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
        return QuaternionRotation.fromAxisAngle(axis, angle);
	}
	
	protected double[] getParameters() {
		//double[] rotAxis = rotation.getAxis().toArray();
		double[] rotAxis = rotation.getAxis().toArray();
		double rotAngle = rotation.getAngle();
		return new double[] {
			rotAxis[0] * rotAngle,
			rotAxis[1] * rotAngle,
			rotAxis[2] * rotAngle,
			translation[0], translation[1], translation[2]};
	}
	
	public QuaternionRotation getRotation() {
		return rotation;
	}
	
	public double[] getRotationAxis() {
		double[] rotAxis = rotation.getAxis().toArray();
		//double[] rotAxis = rotation.getAxis(RotationConvention.VECTOR_OPERATOR).toArray();
		double rotAngle = rotation.getAngle();
		rotAxis[0] *= rotAngle;
		rotAxis[1] *= rotAngle;
		rotAxis[2] *= rotAngle;
		return rotAxis;
	}
	
	public RealMatrix getRotationMatrix() { // TODO: check matrix dimensions!!
        return MatrixUtils.createRealMatrix(Rotations.getRotationMatrix(this.rotation));
	}
	
	public double[] getTranslation() {
		return translation;
	}
	
	public RealVector getTranslationVector() {
		return MatrixUtils.createRealVector(translation);
	}
	
	// ----------------------------------------------------------------------------------

	/**
	 * Moves point XYZ from 3D world coordinates to 3D camera coordinates, as specified by the transformations of this
	 * view. No projection is involved.
	 *
	 * @param XYZ a 3D (world) point
	 * @return the given world point mapped to 3D camera coordinates
	 */
	protected double[] applyTo(double[] XYZ) {	// 3D vector XYZ assumed
		//double[] XYZc = new double[3];
		//rotation.applyTo(XYZ, XYZc);
        double[] XYZc =  rotation.apply(Vector3D.of(XYZ)).toArray();    // TODO: check, stick with Vector3D?
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

}
