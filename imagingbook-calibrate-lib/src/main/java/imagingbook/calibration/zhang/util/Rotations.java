/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.util;

import imagingbook.common.math.Arithmetic;
import imagingbook.common.math.Matrix;
import org.apache.commons.math3.geometry.euclidean.threed.NotARotationMatrixException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static imagingbook.common.math.Arithmetic.isZero;
import static imagingbook.common.math.Matrix.add;
import static imagingbook.common.math.Matrix.idMatrix;
import static imagingbook.common.math.Matrix.multiply;
import static imagingbook.common.math.Matrix.normL2;
import static imagingbook.common.math.Matrix.zeroVector;

/**
 * This class defines methods for converting between Rodrigues rotation vectors and 3D rotation matrices, plus some
 * related utility methods. None of these methods is currently used in other parts of the calibration library.
 */
public class Rotations {

    private static final double TWO_PI = 2 * Math.PI;

    // ++++++++++++++   Rodrigues vector --> Rotation matrix  +++++++++++++++++++

    /**
     * Converts a given Rodrigues rotation vector to the equivalent 3D rotation matrix. Hand-made calculation (uses no
     * library methods).
     *
     * @param rv Rodrigues rotation vector
     * @return the 3D rotation matrix
     */
    public static double[][] toRotationMatrix(double[] rv) {
        double theta = normL2(rv);
        double rx = rv[0] / theta;
        double ry = rv[1] / theta;
        double rz = rv[2] / theta;
        // System.out.println("rotation angle1 = " + theta);
        // System.out.println("rotation axis1 = " + Matrix.toString(new double[] {rx, ry, rz}));
        double[][] W = {
                {0, -rz, ry},
                {rz, 0, -rx},
                {-ry, rx, 0}};
        // System.out.println("W = \n" + Matrix.toString(W));
        double[][] I = idMatrix(3);
        double[][] R1 = add(I, multiply(Math.sin(theta), W));
        double[][] R2 = multiply(1 - Math.cos(theta), multiply(W, W));
        double[][] R = add(R1, R2);
        return R;
    }

    /**
     * Converts a given Rodrigues rotation vector to the equivalent 3D rotation matrix. For comparison, this version
     * uses Apache Commons Math (ACM).
     *
     * @param rv Rodrigues rotation vector
     * @return the 3D rotation matrix
     */
    static double[][] toRotationMatrixACM(double[] rv) {
        double angle = normL2(rv);
        Vector3D axis = new Vector3D(rv);
        Rotation rotation = new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
        System.out.println("rotation angle2 = " + rotation.getAngle());
        System.out.println("rotation axis2 = " + rotation.getAxis(RotationConvention.VECTOR_OPERATOR));
        return rotation.getMatrix();
    }

    // ++++++++++++++   Rotation matrix --> Rodrigues vector +++++++++++++++++++

    /**
     * Converts a 3D rotation matrix (R) to the equivalent Rodrigues rotation vector. From "Vector Representation of
     * Rotations", Carlo Tomasi (https://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.pdf). Matlab code:
     * http://www.cs.duke.edu/courses/fall13/compsci527/notes/rodrigues.m
     *
     * @param R a 3D rotation matrix
     * @return the Rodrigues rotation vector
     */
    public static double[] toRodriguesVector(double[][] R) {
        // final double eps = EPSILON_DOUBLE;
        double[] p = {
                0.5 * (R[2][1] - R[1][2]),
                0.5 * (R[0][2] - R[2][0]),
                0.5 * (R[1][0] - R[0][1])};
        double s = normL2(p);
        double c = 0.5 * (Matrix.trace(R) - 1);
        if (isZero(s)) {                    // Rotation angle is either 0 or pi
            if (isZero(c - 1)) {            // Case 1: c = 1, Rotation angle is 0
                return zeroVector(3);
            } else if (isZero(c + 1)) {        // Case 2: c = -1, Rotation angle is pi
                // find the column of R + I with greatest norm (for better numerical results)
                double[][] Rp = add(R, idMatrix(3));
                double[] v = getMaxColumnVector(Rp);
                double vn = normL2(v);
                if (isZero(vn)) {        // this shouldn't really happen
                    throw new RuntimeException("R is an inversion, not a rotation");
                }
                double[] u = multiply(1 / vn, v);    // unit vector
                return multiply(Math.PI, normalizeSign(u));
            } else {                  // how can this be?
                throw new RuntimeException("sin(theta) is zero, bus cos(theta) is neither 1 nor -1!");
            }
        } else {   // (s != 0) : rotation strictly between 0 and pi
            double[] u = multiply(1.0 / s, p);    // unit vector
            double theta = Math.atan2(s, c);
            return multiply(theta, u);
        }
    }

    // http://math.stackexchange.com/questions/83874/efficient-and-accurate-numerical-implementation-of-the-inverse-rodrigues-rotatio

    /**
     * Converts a 3D rotation matrix (R) to the equivalent Rodrigues rotation vector. For comparison, this version uses
     * Apache Commons Math (ACM).
     *
     * @param R
     * @return
     */
    static double[] toRodriguesVectorACM(double[][] R) {
        Rotation rot = new Rotation(R, 0.01);
        double angle = rot.getAngle();
        Vector3D axis = rot.getAxis(RotationConvention.VECTOR_OPERATOR);
        double[] rv = axis.scalarMultiply(angle / axis.getNorm()).toArray();
        return rv;
    }

    /**
     * Changes the sign of a unit vector u so that it is on the proper half of the unit sphere.
     *
     * @param x unit vector
     * @return same or inverted unit vector
     */
    private static double[] normalizeSign(double[] x) {
        if ((x[0] < 0) ||
                (isZero(x[0]) && x[1] < 0) ||
                (isZero(x[0]) && isZero(x[1]) && x[2] < 0)) {
            return multiply(-1, x);
        } else {
            return x;
        }
    }

    /**
     * Returns the column vector of the given matrix with the greatest norm.
     *
     * @param A a matrix
     * @return the maximum-norm column vector
     */
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

    public static final double DefaultOrthogonalityThreshold = 1e-6;

    /**
     * Checks if the specified matrix is a rotation matrix under the given orthogonality threshold.
     *
     * @param R the matrix to be checked
     * @param threshold the orthogonality threshold
     * @return
     */
    public static boolean isRotationMatrix(double[][] R, double threshold) {
        try {
            Rotation rot = new Rotation(R, threshold);
        } catch (NotARotationMatrixException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the specified matrix is a rotation matrix using the default orthogonality threshold
     * ({@link #DefaultOrthogonalityThreshold}).
     *
     * @param R the matrix to be checked
     * @return
     */
    public static boolean isRotationMatrix(double[][] R) {
        return isRotationMatrix(R, DefaultOrthogonalityThreshold);
    }

    /**
     * Normalized the given angle to [-π,π].
     *
     * @param angle some angle (any finite value)
     * @return the equivalent angle in [-π,π]
     */
    public static double normalizeAngle(double angle) {
        // http://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/util/MathUtils.html
        // return MathUtils.normalizeAngle(angle, 0.0);
        return angle - TWO_PI * Math.floor((angle + Math.PI) / TWO_PI);
    }

    /**
     * Creates a Rodrigues rotation vector from a given 3D rotation axis and angle. The angle (normalized to [0,2π])
     * determines the norm of the resulting vector. Use with care, results are not unique!
     *
     * @param axis a 3D vector representing the rotation axis
     * @param theta the rotation angle
     * @return
     */
    private static double[] makeRodriguesVector(double[] axis, double theta) {
        double s = normL2(axis);
        if (Arithmetic.isZero(s)) {
            throw new IllegalArgumentException("rotation axis vector must have nonzero norm");
        }
        return multiply(theta / s, axis);
    }

}
