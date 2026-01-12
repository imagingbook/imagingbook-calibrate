/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.extrinsics;

import imagingbook.calibrate.math3legacy.Rotation;
import imagingbook.calibrate.math3legacy.RotationConvention;
import imagingbook.calibrate.util.MathUtil;
import imagingbook.common.math.Matrix;

import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.io.StringWriter;

/**
 * Instances of this class represent extrinsic camera (view) parameters.
 * @author WB
 */
public class ViewTransform {

    private static final double OrthogonalityThreshold = 0.01;
    private final Rotation rotation;
    private final double[] translation;
    public static final int PARAMETER_COUNT = 6;    // 3 rotation + 3 translation parameters

    // ----------------------------------------------------------------------------------

    public ViewTransform() {
        this.rotation = Rotation.IDENTITY;
        this.translation = new double[] {0, 0, 0};
    }

    public ViewTransform(double rX, double rY, double rZ, double tX, double tY, double tZ) {
        double[] r = {rX, rY, rZ};
        this.rotation = new Rotation(Vector3D.of(r), Matrix.normL2(r), RotationConvention.DEFAULT);
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
    }

    /**
     * Creates a ViewTransform instance from a 1D vector
     * w = (r0, r1, r2, t0, t1, t2), where
     * (r0, r1, r2) is a 3D (Rodrigues) rotation vector and
     * (t0, t1, t2) is a 3D translation vector.
     * @param w a vector with 6 view parameters
     */
    public ViewTransform(double[] w) {
        this(w[0], w[1], w[2], w[3], w[4], w[5]);
    }

    /**
     * Calculates and returns the view transform for given
     * camera intrinsics (A) and a homography between
     * model points and observed sensor points (H).
     * @param A intrinsic camera parameters
     * @param H homography
     * @return a new ViewTransform instance
     */
    public static ViewTransform from(RealMatrix A,  RealMatrix H) {
        RealVector h0 = H.getColumnVector(0);
        RealVector h1 = H.getColumnVector(1);
        RealVector h2 = H.getColumnVector(2);

        RealMatrix A_inv = MatrixUtils.inverse(A);    // a bit wasteful to invert for every view
        double lambda = 1 / A_inv.operate(h0).getNorm();
        // System.out.format("lambda = %f\n", lambda);

        // compute the columns in the rotation matrix
        RealVector r0 = A_inv.operate(h0).mapMultiplyToSelf(lambda);
        RealVector r1 = A_inv.operate(h1).mapMultiplyToSelf(lambda);
        RealVector r2 = MathUtil.crossProduct3x3(r0, r1);
        RealVector t = A_inv.operate(h2).mapMultiplyToSelf(lambda);
        // System.out.println("r0 = " + Matrix.toString(r0.toArray()));
        // System.out.println("r1 = " + Matrix.toString(r1.toArray()));
        // System.out.println("r2 = " + Matrix.toString(r2.toArray()));
        // System.out.println("t = " + Matrix.toString(t.toArray()));
        RealMatrix R = MatrixUtils.createRealMatrix(3, 3);
        R.setColumnVector(0, r0);
        R.setColumnVector(1, r1);
        R.setColumnVector(2, r2);
        // System.out.println("Rinit = \n" + Matrix.toString(R.getData()));
        // Matrix R is probably not a proper rotation matrix. So find
        // the closest real rotation matrix (ViewTransform takes care of this):
        return new ViewTransform(R, t);
    }

    // ----------------------------------------------------------------------------------

    /**
     * This has been delegated to {@link ViewTransform} constructor,
     * @param w
     * @return
     */
    private static Rotation makeRotation(double[] w) {
        Vector3D axis = Vector3D.of(w[0], w[1], w[2]);
        double angle = axis.norm();
        //return new Rotation(axis, angle);
        return new Rotation(axis, angle, RotationConvention.DEFAULT);
    }

    public double[] getParameters() {
        //double[] rotAxis = rotation.getAxis().toArray();
        double[] axis = rotation.getAxis(RotationConvention.DEFAULT).toArray();
        double angle = rotation.getAngle();
        return new double[] {
                axis[0] * angle, axis[1] * angle, axis[2] * angle,
                translation[0], translation[1], translation[2]};
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    // public double[] getRotationAxis() {
    //     //double[] rotAxis = rotation.getAxis().toArray();
    //     double[] rotAxis = rotation.getAxis(RotationConvention.DEFAULT).toArray();
    //     double rotAngle = rotation.getAngle();
    //     rotAxis[0] *= rotAngle;
    //     rotAxis[1] *= rotAngle;
    //     rotAxis[2] *= rotAngle;
    //     return rotAxis;
    // }

    /**
     * Returns the rotation part of this ViewTransform as a 3x3 matrix.
     * @return a 3x3 rotation matrix
     */
    public RealMatrix getRotationMatrix() {
        return MatrixUtils.createRealMatrix(this.rotation.getMatrix());
    }

    /**
     * Returns the translation part of this ViewTransform.
     * @return a 3-element translation vector
     */
    public double[] getTranslation() {
        return this.translation;
    }

    /**
     * Returns the translation part of this ViewTransform.
     * @return a 3-element translation vector
     */
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
    public double[] applyTo(double[] XYZ) {	// 3D vector XYZ assumed
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

}
