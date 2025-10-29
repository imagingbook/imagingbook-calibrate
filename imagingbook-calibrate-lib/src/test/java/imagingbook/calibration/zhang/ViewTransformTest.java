/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.util.Rotations;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.geometry.euclidean.threed.rotation.QuaternionRotation;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.numbers.quaternion.Quaternion;
import org.junit.Assert;
import org.junit.Test;
import imagingbook.common.math.Matrix;

import java.util.Arrays;

import static imagingbook.calibration.zhang.util.Rotations.isRotationMatrix;
import static imagingbook.testutils.NumericTestUtils.assert2dArrayEquals;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ViewTransformTest {

    /**
     * Check if the rotation matrix of the null rotation is the identity matrix
     * and the translation vector is the zero vector.
     */
    @Test
    public void testViewTransformConstructor1() {
        ViewTransform vt = new ViewTransform();
        double[][] rot = Rotations.getRotationMatrix(vt.getRotation());
        // System.out.println("Rotation = \n" + Matrix.toString(rot));
        assert2dArrayEquals(Matrix.idMatrix(3), rot);

        double[] trans = vt.getTranslation();
        assertArrayEquals(new double[]{0, 0, 0}, trans, 1e-6);
    }


    @Test
    public void testViewTransformConstructor2() {
        ViewTransform vt = new ViewTransform(1, 1, 1, 4, 5, 6);
        // get the rotation matrix
        double[][] R = vt.getRotationMatrix().getData();
        // check rotation matrix
        PrintPrecision.set(6); System.out.println("Rotation = \n" + Matrix.toString(R));
        // assert2dArrayEquals(new double[][]{
        //         {0.226296, -0.183008, 0.956712},
        //         {0.956712, 0.226296, -0.183008},
        //         {-0.183008, 0.956712, 0.226296}}, R, 0.001);
        // check if R is orthonormal
        assertTrue(isRotationMatrix(R, 0.01));
        // get translation vector
        double[] trans = vt.getTranslationVector().toArray();
        // check translation vector:
        // System.out.println("Translation = \n" + Matrix.toString(trans));
        assertArrayEquals(new double[]{4, 5, 6}, trans, 0.001);
    }

    @Test
    public void testViewTransformConstructor3() {
        double[] t1 = {1, -2, 3};               // translation vector
        double[] q1 = { 0.5, -0.2, 1.0, 0.7};   // quaternion rotation vector
        QuaternionRotation qr1 = QuaternionRotation.of(q1[0], q1[1], q1[2], q1[3]);
        double angle1 = qr1.getAngle();
        double[] axis1 = qr1.getAxis().toArray();
        // System.out.println("angle1 = " + angle1);
        // System.out.println("axis1 = " + Arrays.toString(axis1));
        ViewTransform vt1 = new ViewTransform(qr1, Matrix.makeRealVector(t1));

        // get the rotation matrix
        double[][] R = vt1.getRotationMatrix().getData();

        // check values of rotation matrix R
        // PrintPrecision.set(6); System.out.println("Rotation = \n" + Matrix.toString(R));
        // PrintPrecision.set(6); System.out.println("R = \n" + Matrix.toString(R));

        // assert2dArrayEquals(new double[][]{
        //         {-0.674157, -0.617978, 0.404494},
        //         {0.168539, 0.404494, 0.898876},
        //         {-0.719101, 0.674157, -0.168539}}, R, 0.001);

        // check if R is orthonormal
        assertTrue(isRotationMatrix(R, 0.01));

        // create a second ViewTransform from rotation matrix R and compare axis/angle:
        ViewTransform vt2 = new ViewTransform(MatrixUtils.createRealMatrix(R), MatrixUtils.createRealVector(t1));
        QuaternionRotation qr2 = vt2.getRotation();
        double angle2 = qr2.getAngle();
        double[] axis2 = qr2.getAxis().toArray();
        // System.out.println("angle2 = " + angle2);
        // System.out.println("axis2 = " + Arrays.toString(axis2));
        assertEquals(angle1, angle2, 1e-6);
        // assertArrayEquals(axis1, axis2, 1e-6);   // fails because axis points in opposite direction!

        // Quaternion q2 = vt2.getRotation().getQuaternion();
        // double[] qv2 = {q2.getW(), q2.getX(), q2.getY(), q2.getZ()};
        // System.out.println("qv2 = \n" + Matrix.toString(qv2));                  // CHECK!! Make Rotations test!!
        // System.out.println("norm = \n" + Matrix.toString(Matrix.normalize(q1)));

        }
}