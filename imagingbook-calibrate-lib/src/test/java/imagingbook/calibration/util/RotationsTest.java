/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.util;

import imagingbook.common.math.Matrix;
import imagingbook.testutils.NumericTestUtils;
import org.junit.Test;

import static imagingbook.calibration.util.Rotations.isRotationMatrix;
import static imagingbook.calibration.util.Rotations.normalizeAngle;
import static imagingbook.calibration.util.Rotations.toRodriguesVector;
import static imagingbook.calibration.util.Rotations.toRodriguesVectorACM;
import static imagingbook.calibration.util.Rotations.toRotationMatrix;
import static imagingbook.calibration.util.Rotations.toRotationMatrixACM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RotationsTest {

    static double threshold = 1e-6;


    static final double[][] R1 = {  // some orthogonal rotation matrix
            {0.283662, 0.000000, 0.958924},
            {0.000000, 1.000000, 0.000000},
            {-0.958924, 0.000000, 0.283662}};

    // --------------------------------------------------------------------------

    @Test   // Check workings of Rotations.normalizeAngle()
    public void testNormalizeAngle() {
        double TOL = 1e-12;
        assertEquals(0.0, normalizeAngle(0.0), TOL);
        assertEquals(1.0, normalizeAngle(1.0), TOL);
        assertEquals(-1.0, normalizeAngle(-1.0), TOL);

        assertEquals(-Math.PI/2, normalizeAngle(5.5 * Math.PI), TOL);
        assertEquals(0.0, normalizeAngle(6 * Math.PI), TOL);
        assertEquals(Math.PI/2, normalizeAngle(6.5 * Math.PI), TOL);

        assertEquals(Math.PI/2, normalizeAngle(-9.5 * Math.PI), TOL);
        assertEquals(0.0, normalizeAngle(-10 * Math.PI), TOL);
        assertEquals(-Math.PI/2, normalizeAngle(-10.5 * Math.PI), TOL);
    }

    @Test   // Check workings of Rotations.isRotationMatrix()
    public void testIsRotationMatrix() {
        assertTrue(isRotationMatrix(Matrix.idMatrix(3)));
        assertTrue(isRotationMatrix(R1, threshold));
        assertTrue(isRotationMatrix(Matrix.transpose(R1), threshold));
        assertTrue(isRotationMatrix(Matrix.inverse(R1), threshold));
        assertFalse(isRotationMatrix(Matrix.multiply(2.0, R1), threshold));
    }

    @Test   // Check conversion of Rodrigues rotation vector to 3D rotation matrix
    public void testRodriguesToMatrix() {
        // rotation vector from matrix R1
        double[] rv1 = toRodriguesVector(R1);
        // check values of rv1
        // System.out.println("rv1 = " + Arrays.toString(rv1));
        assertArrayEquals(new double[] {0.0, 1.283185407, 0.0}, rv1, 1e-6); // sure?
        // rotation matrix back from vector rv1
        double[][] R2 = toRotationMatrix(rv1);
        // R2 must be a rotation matrix
        assertTrue(isRotationMatrix(R2, threshold));
        // R1, R2 must be the same
        NumericTestUtils.assert2dArrayEquals(R1, R2, 1e-6);
    }


    @Test   // same as testRodriguesToMatrix() but using commons math methods
    public void testRodriguesToMatrixACM() {
        // rotation vector from matrix R1
        double[] rv1 = toRodriguesVectorACM(R1);
        // check values of rv1
        // System.out.println("rv1 = " + Arrays.toString(rv1));
        assertArrayEquals(new double[] {0.0, 1.283185407, 0.0}, rv1, 1e-6);   // different sign!
        // rotation matrix back from vector rv1
        double[][] R2 = toRotationMatrixACM(rv1);
        // R2 must be a rotation matrix
        assertTrue(isRotationMatrix(R2, threshold));
        // R1, R2 must be the same
        NumericTestUtils.assert2dArrayEquals(R1, R2, 1e-6);
    }


}
