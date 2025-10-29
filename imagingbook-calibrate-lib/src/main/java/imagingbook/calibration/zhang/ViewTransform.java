/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.geom3d.Rotation;
import imagingbook.calibration.zhang.geom3d.RotationConvention;
import imagingbook.common.math.Matrix;

import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * Instances of this class represent extrinsic camera (view) parameters.
 *
 * @author WB
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
        this.rotation = new Rotation(new double[] {rX, rY, rZ});
        // this.rotation = makeRotation(new double[] {rX, rY, rZ});
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
     * @param w
     */
    public ViewTransform(double[] w) {
        this(w[0], w[1], w[2], w[3], w[4], w[5]);
    }

    // ----------------------------------------------------------------------------------

    private Rotation makeRotation(double[] w) {
        Vector3D axis = Vector3D.of(w[0], w[1], w[2]);
        double angle = axis.norm();
        //return new Rotation(axis, angle);
        return new Rotation(axis, angle, RotationConvention.DEFAULT);
    }

    protected double[] getParameters() {
        //double[] rotAxis = rotation.getAxis().toArray();
        double[] rotAxis = rotation.getAxis(RotationConvention.DEFAULT).toArray();
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

    // public double[] getRotationAxis() {
    //     //double[] rotAxis = rotation.getAxis().toArray();
    //     double[] rotAxis = rotation.getAxis(RotationConvention.DEFAULT).toArray();
    //     double rotAngle = rotation.getAngle();
    //     rotAxis[0] *= rotAngle;
    //     rotAxis[1] *= rotAngle;
    //     rotAxis[2] *= rotAngle;
    //     return rotAxis;
    // }

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
     * Moves point XYZ from 3D world coordinates to 3D camera coordinates, as specified by the transformations of this
     * view. No projection is involved.
     *
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

}
