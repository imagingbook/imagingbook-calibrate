/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate;

import imagingbook.calibrate.extrinsics.ViewTransform;
import org.junit.Test;
import imagingbook.common.math.Matrix;

import static imagingbook.calibrate.util.Rotations.isRotationMatrix;
import static imagingbook.testutils.NumericTestUtils.assert2dArrayEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class ViewTransformTest {

    /**
     * Check if the rotation matrix of the null rotation is the identity matrix
     * and the translation vector is the zero vector.
     */
    @Test
    public void testViewTransformConstructor1() {
        ViewTransform vt = new ViewTransform();
        double[][] rot = vt.getRotation().getMatrix();    // = vt.getRotationMatrix().getData();
        assert2dArrayEquals(Matrix.idMatrix(3), rot);
        double[] trans = vt.getTranslationVector().toArray();
        assertArrayEquals(new double[] {0, 0, 1}, trans, 1e-6);
    }

    @Test
    public void testViewTransformConstructor2A() {
        ViewTransform vt = new ViewTransform(1, 1, 1, 4, 5, 6);
        // get the rotation matrix
        double[][] R = vt.getRotation().getMatrix();
        // check rotation matrix
        // PrintPrecision.set(6); System.out.println("Rotation = \n" + Matrix.toString(R));
        assert2dArrayEquals(new double[][]{
                {0.226296, -0.183008, 0.956712},
                {0.956712, 0.226296, -0.183008},
                {-0.183008, 0.956712, 0.226296}}, R, 0.001);
        // check if R is orthonormal
        assertTrue(isRotationMatrix(R, 0.01));
        // get translation vector
        double[] trans = vt.getTranslationVector().toArray();
        // check translation vector:
        // System.out.println("Translation = \n" + Matrix.toString(trans));
        assertArrayEquals(new double[]{4, 5, 6}, trans, 0.001);
    }

    @Test
    public void testViewTransformConstructor2B() {
        ViewTransform vt = new ViewTransform(new double[] {1, 1, 1, 4, 5, 6});
        // get the rotation matrix
        double[][] R = vt.getRotation().getMatrix();
        // check rotation matrix
        // PrintPrecision.set(6); System.out.println("Rotation = \n" + Matrix.toString(R));
        assert2dArrayEquals(new double[][]{
                {0.226296, -0.183008, 0.956712},
                {0.956712, 0.226296, -0.183008},
                {-0.183008, 0.956712, 0.226296}}, R, 0.001);
        // check if R is orthonormal
        assertTrue(isRotationMatrix(R, 0.01));
        // get translation vector
        double[] trans = vt.getTranslationVector().toArray();
        // check translation vector:
        // System.out.println("Translation = \n" + Matrix.toString(trans));
        assertArrayEquals(new double[]{4, 5, 6}, trans, 0.001);
    }

    // TODO: test for 'ViewTransform.from()' method

}