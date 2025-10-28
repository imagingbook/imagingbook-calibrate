/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.util;

import imagingbook.calibration.zhang.geom3d.NotARotationMatrixException;
import imagingbook.common.math.Arithmetic;
import imagingbook.common.math.Matrix;

// import org.apache.commons.geometry.euclidean.threed.Vector3D;
// import org.apache.commons.numbers.quaternion.Quaternion;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.rotation.QuaternionRotation;
import org.apache.commons.geometry.euclidean.threed.AffineTransformMatrix3D;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.numbers.quaternion.Quaternion;


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

    public static final double DefaultOrthogonalityThreshold = 1e-6;
    static final double TWO_PI = 2 * Math.PI;

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
        Vector3D axis = Vector3D.of(rv);
        QuaternionRotation rot = QuaternionRotation.fromAxisAngle(axis, angle);
        Quaternion q = rot.getQuaternion();
        double[] qv = {q.getW(), q.getX(), q.getY(), q.getZ(), }; // quaternion components
        // System.out.println("toRotationMatrixACM: qv = " + Arrays.toString(qv));
        // Rotation rotation = new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR);
        // System.out.println("rotation angle2 = " + rotation.getAngle());
        // System.out.println("rotation axis2 = " + rotation.getAxis(RotationConvention.VECTOR_OPERATOR));
        // return rotation.getMatrix();
        // adaptation to commons-math4:
        double[] a = rot.toMatrix().toArray();
        return new double[][] {     // requires transpose?
                { a[0], a[4], a[8]},
                { a[1], a[5], a[9]},
                { a[2], a[6], a[10]}
        };
        // return new double[][] {
        //         { a[0], a[1], a[2]},
        //         { a[4], a[5], a[6]},
        //         { a[8], a[9], a[10]}
        // };
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
        double[] qv = makeRotation(R, 0.01);   // replacement for 'Rotation' constructor
        // System.out.println("toRodriguesVectorACM: qv = " + Arrays.toString(qv));  // OK!
        QuaternionRotation rot = QuaternionRotation.of(qv[0], qv[1], qv[2], qv[3]); // = (w, x, y, z)

        double angle = rot.getAngle();
        Vector3D axis = rot.getAxis();
        // System.out.println("toRodriguesVectorACM: angle = " + angle);
        // System.out.println("toRodriguesVectorACM: axis = " + Arrays.toString(axis.toArray()));

        double[] rv = axis.multiply(angle / axis.norm()).toArray();
        //double[] rv = axis.scalarMultiply(angle / axis.getNorm()).toArray();
        return rv;
    }

    /**
     * Changes the sign of a unit vector u so that it is on the proper half of the unit sphere.
     *
     * @param x unit vector
     * @return same or inverted unit vector
     */
    static double[] normalizeSign(double[] x) {
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
    static double[] getMaxColumnVector(double[][] A) {
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

    /**
     * Checks if the specified matrix is a rotation matrix under the given orthogonality threshold.
     *
     * @param R the matrix to be checked
     * @param threshold the orthogonality threshold
     * @return
     */
    public static boolean isRotationMatrix(double[][] R, double threshold) {
        try {
            double[] rot = makeRotation(R, threshold);
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

    // --------------------------------------------------------------------------------

    /**
     * Creates a QuaternionRotation instance from a given rotation matrix, which
     * must be orthogonal.
     *
     * @param R 3x3 rotation matrix
     * @param threshold orthogonality threshold
     * @return
     */
    public static QuaternionRotation makeRotation(RealMatrix R, double threshold) {
        double[] rv = makeRotation(R.getData(), threshold);
        // System.out.println("QuaternionRotation makeRotation: rv = " + Matrix.toString(rv));
        return QuaternionRotation.of(rv[0], rv[1], rv[2], rv[3]);
    }

    /**
     * Build a rotation (quaternion) from a 3X3 matrix.
     * Ported from org.apache.commons.math3.geometry.euclidean.threed.Rotation.java
     *
     * <p>Rotation matrices are orthogonal matrices, i.e. unit matrices
     * (which are matrices for which m.m<sup>T</sup> = I) with real
     * coefficients. The module of the determinant of unit matrices is
     * 1, among the orthogonal 3X3 matrices, only the ones having a
     * positive determinant (+1) are rotation matrices.</p>
     *
     * <p>When a rotation is defined by a matrix with truncated values
     * (typically when it is extracted from a technical sheet where only
     * four to five significant digits are available), the matrix is not
     * orthogonal anymore. This constructor handles this case
     * transparently by using a copy of the given matrix and applying a
     * correction to the copy in order to perfect its orthogonality. If
     * the Frobenius norm of the correction needed is above the given
     * threshold, then the matrix is considered to be too far from a
     * true rotation matrix and an exception is thrown.</p>
     *
     * @param m rotation matrix
     * @param threshold convergence threshold for the iterative
     * orthogonality correction (convergence is reached when the
     * difference between two steps of the Frobenius norm of the
     * correction is below this threshold)
     *
     * @exception NotARotationMatrixException if the matrix is not a 3X3
     * matrix, or if it cannot be transformed into an orthogonal matrix
     * with the given threshold, or if the determinant of the resulting
     * orthogonal matrix is negative
     */
    public static double[] makeRotation(double[][] m, double threshold)
            throws NotARotationMatrixException {
                // dimension check
        if ((m.length != 3) || (m[0].length != 3) ||
                (m[1].length != 3) || (m[2].length != 3)) {
            throw new NotARotationMatrixException("Rotation matrix is not 3x3");
        }
        // compute a "close" orthogonal matrix
        double[][] ort = orthogonalizeMatrix(m, threshold);
        //System.out.println("makeRotation: ort = \n" + Matrix.toString(ort));

        // check the sign of the determinant
        double det =
                ort[0][0] * (ort[1][1] * ort[2][2] - ort[2][1] * ort[1][2]) -
                ort[1][0] * (ort[0][1] * ort[2][2] - ort[2][1] * ort[0][2]) +
                ort[2][0] * (ort[0][1] * ort[1][2] - ort[1][1] * ort[0][2]);
        if (det < 0.0) {
            throw new NotARotationMatrixException("Closest orthogonal matrix has negative determinant: " + det);
        }
        // double[] quat = mat2quat(ort);
        // q0 = quat[0]; q1 = quat[1]; q2 = quat[2]; q3 = quat[3];
        return mat2quat(ort);
    }


    /** Convert an orthogonal rotation matrix to a quaternion.
     * Ported from org.apache.commons.math3.geometry.euclidean.threed.Rotation.java
     * @param ort orthogonal rotation matrix
     * @return quaternion corresponding to the matrix
     */
    private static double[] mat2quat(final double[][] ort) {
        final double[] quat = new double[4];

        // There are different ways to compute the quaternions elements
        // from the matrix. They all involve computing one element from
        // the diagonal of the matrix, and computing the three other ones
        // using a formula involving a division by the first element,
        // which unfortunately can be zero. Since the norm of the
        // quaternion is 1, we know at least one element has an absolute
        // value greater or equal to 0.5, so it is always possible to
        // select the right formula and avoid division by zero and even
        // numerical inaccuracy. Checking the elements in turn and using
        // the first one greater than 0.45 is safe (this leads to a simple
        // test since qi = 0.45 implies 4 qi^2 - 1 = -0.19)
        double s = ort[0][0] + ort[1][1] + ort[2][2];
        if (s > -0.19) {
            // compute q0 and deduce q1, q2 and q3
            quat[0] = 0.5 * Math.sqrt(s + 1.0);
            double inv = 0.25 / quat[0];
            quat[1] = inv * (ort[1][2] - ort[2][1]);
            quat[2] = inv * (ort[2][0] - ort[0][2]);
            quat[3] = inv * (ort[0][1] - ort[1][0]);
        } else {
            s = ort[0][0] - ort[1][1] - ort[2][2];
            if (s > -0.19) {
                // compute q1 and deduce q0, q2 and q3
                quat[1] = 0.5 * Math.sqrt(s + 1.0);
                double inv = 0.25 / quat[1];
                quat[0] = inv * (ort[1][2] - ort[2][1]);
                quat[2] = inv * (ort[0][1] + ort[1][0]);
                quat[3] = inv * (ort[0][2] + ort[2][0]);
            } else {
                s = ort[1][1] - ort[0][0] - ort[2][2];
                if (s > -0.19) {
                    // compute q2 and deduce q0, q1 and q3
                    quat[2] = 0.5 * Math.sqrt(s + 1.0);
                    double inv = 0.25 / quat[2];
                    quat[0] = inv * (ort[2][0] - ort[0][2]);
                    quat[1] = inv * (ort[0][1] + ort[1][0]);
                    quat[3] = inv * (ort[2][1] + ort[1][2]);
                } else {
                    // compute q3 and deduce q0, q1 and q2
                    s = ort[2][2] - ort[0][0] - ort[1][1];
                    quat[3] = 0.5 * Math.sqrt(s + 1.0);
                    double inv = 0.25 / quat[3];
                    quat[0] = inv * (ort[0][1] - ort[1][0]);
                    quat[1] = inv * (ort[0][2] + ort[2][0]);
                    quat[2] = inv * (ort[2][1] + ort[1][2]);
                }
            }
        }
        return quat;
    }

    /** Perfect orthogonality on a 3X3 matrix.
     * Ported from org.apache.commons.math3.geometry.euclidean.threed.Rotation.java
     *
     * @param m initial matrix (not exactly orthogonal)
     * @param threshold convergence threshold for the iterative
     * orthogonality correction (convergence is reached when the
     * difference between two steps of the Frobenius norm of the
     * correction is below this threshold)
     * @return an orthogonal matrix close to m
     * @exception NotARotationMatrixException if the matrix cannot be
     * orthogonalized with the given threshold after 10 iterations
     */
    private static double[][] orthogonalizeMatrix(double[][] m, double threshold)
            throws NotARotationMatrixException {
        double[] m0 = m[0];
        double[] m1 = m[1];
        double[] m2 = m[2];
        double x00 = m0[0];
        double x01 = m0[1];
        double x02 = m0[2];
        double x10 = m1[0];
        double x11 = m1[1];
        double x12 = m1[2];
        double x20 = m2[0];
        double x21 = m2[1];
        double x22 = m2[2];
        double fn = 0;
        double fn1;

        double[][] o = new double[3][3];
        double[] o0 = o[0];
        double[] o1 = o[1];
        double[] o2 = o[2];

        // iterative correction: Xn+1 = Xn - 0.5 * (Xn.Mt.Xn - M)
        int i = 0;
        while (++i < 11) {

            // Mt.Xn
            double mx00 = m0[0] * x00 + m1[0] * x10 + m2[0] * x20;
            double mx10 = m0[1] * x00 + m1[1] * x10 + m2[1] * x20;
            double mx20 = m0[2] * x00 + m1[2] * x10 + m2[2] * x20;
            double mx01 = m0[0] * x01 + m1[0] * x11 + m2[0] * x21;
            double mx11 = m0[1] * x01 + m1[1] * x11 + m2[1] * x21;
            double mx21 = m0[2] * x01 + m1[2] * x11 + m2[2] * x21;
            double mx02 = m0[0] * x02 + m1[0] * x12 + m2[0] * x22;
            double mx12 = m0[1] * x02 + m1[1] * x12 + m2[1] * x22;
            double mx22 = m0[2] * x02 + m1[2] * x12 + m2[2] * x22;

            // Xn+1
            o0[0] = x00 - 0.5 * (x00 * mx00 + x01 * mx10 + x02 * mx20 - m0[0]);
            o0[1] = x01 - 0.5 * (x00 * mx01 + x01 * mx11 + x02 * mx21 - m0[1]);
            o0[2] = x02 - 0.5 * (x00 * mx02 + x01 * mx12 + x02 * mx22 - m0[2]);
            o1[0] = x10 - 0.5 * (x10 * mx00 + x11 * mx10 + x12 * mx20 - m1[0]);
            o1[1] = x11 - 0.5 * (x10 * mx01 + x11 * mx11 + x12 * mx21 - m1[1]);
            o1[2] = x12 - 0.5 * (x10 * mx02 + x11 * mx12 + x12 * mx22 - m1[2]);
            o2[0] = x20 - 0.5 * (x20 * mx00 + x21 * mx10 + x22 * mx20 - m2[0]);
            o2[1] = x21 - 0.5 * (x20 * mx01 + x21 * mx11 + x22 * mx21 - m2[1]);
            o2[2] = x22 - 0.5 * (x20 * mx02 + x21 * mx12 + x22 * mx22 - m2[2]);

            // correction on each elements
            double corr00 = o0[0] - m0[0];
            double corr01 = o0[1] - m0[1];
            double corr02 = o0[2] - m0[2];
            double corr10 = o1[0] - m1[0];
            double corr11 = o1[1] - m1[1];
            double corr12 = o1[2] - m1[2];
            double corr20 = o2[0] - m2[0];
            double corr21 = o2[1] - m2[1];
            double corr22 = o2[2] - m2[2];
            // Frobenius norm of the correction
            fn1 = corr00 * corr00 + corr01 * corr01 + corr02 * corr02 +
                    corr10 * corr10 + corr11 * corr11 + corr12 * corr12 +
                    corr20 * corr20 + corr21 * corr21 + corr22 * corr22;

            // convergence test
            if (Math.abs(fn1 - fn) <= threshold) {
                return o;
            }
            // prepare next iteration
            x00 = o0[0];
            x01 = o0[1];
            x02 = o0[2];
            x10 = o1[0];
            x11 = o1[1];
            x12 = o1[2];
            x20 = o2[0];
            x21 = o2[1];
            x22 = o2[2];
            fn  = fn1;

        }
        // the algorithm did not converge after 10 iterations
        throw new NotARotationMatrixException("Unable to orthogonalize matrix.");
    }

    /**
     * Helper method, converts a (Commons Math 4) QuaternionRotation to a 3x3 double array.
     * Note that AffineTransformMatrix3D natively only returns a 1D double vector.
     * @param rot the QuaternionRotation
     * @return a 3x3 2D double array
     */
    public static double[][] getRotationMatrix(QuaternionRotation rot) {
        AffineTransformMatrix3D transform = rot.toMatrix();
        double[] a = transform.toArray();   // 1D vector!
        double[][] R = {
                { a[0], a[1], a[2] },
                { a[4], a[5], a[6] },
                { a[8], a[9], a[10] }
        };
        // double[][] R = {    // must be transposed!!
        //         { a[0], a[4], a[2] },
        //         { a[1], a[5], a[9] },
        //         { a[2], a[6], a[10] }};
        return R; // MatrixUtils.createRealMatrix(R);
    }


}
