/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.util;

import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.junit.Test;

import static imagingbook.calibration.zhang.util.Rotations.isRotationMatrix;
import static imagingbook.calibration.zhang.util.Rotations.normalizeAngle;
import static imagingbook.calibration.zhang.util.Rotations.toRodriguesVector;
import static imagingbook.calibration.zhang.util.Rotations.toRodriguesVectorACM;
import static imagingbook.calibration.zhang.util.Rotations.toRotationMatrix;
import static imagingbook.calibration.zhang.util.Rotations.toRotationMatrixACM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RotationsTest {

    static double threshold = 1e-6;
    static {
        PrintPrecision.set(6);
    }

    @Test   // Rodrigues vector -> Rotation matrix
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

    @Test   // Rodrigues vector -> Rotation matrix
    public void testIsRotationMatrix() {
        double[][] R1 = {
                {0.283662, 0.000000, 0.958924},
                {0.000000, 1.000000, 0.000000},
                {-0.958924, 0.000000, 0.283662}};
        assertTrue(isRotationMatrix(R1, threshold));
        assertTrue(isRotationMatrix(Matrix.transpose(R1), threshold));
        assertTrue(isRotationMatrix(Matrix.inverse(R1), threshold));
        assertFalse(isRotationMatrix(Matrix.multiply(2.0, R1), threshold));
    }

    @Test   // Rodrigues vector <-> Rotation matrix
    public void testRodriguesToMatrix() {
        double[][] R1 = {
                {0.283662, 0.000000, 0.958924},
                {0.000000, 1.000000, 0.000000},
                {-0.958924, 0.000000, 0.283662}};

        double[] rv1 = toRodriguesVector(R1);
        double[][] R2 = toRotationMatrix(rv1);

        for (int i = 0; i < R1.length; i++) {
            assertArrayEquals(R1[i], R2[i], 1e-6);
        }

        double[] rv2 = toRodriguesVector(R2);
        assertArrayEquals(rv1, rv2, 1e-6);
    }

    @Test   // Rodrigues vector <-> Rotation matrix (Apache)
    public void testRodriguesToMatrixACM() {
        double[][] R1 = {
                {0.283662, 0.000000, 0.958924},
                {0.000000, 1.000000, 0.000000},
                {-0.958924, 0.000000, 0.283662}};

        double[] rv1 = toRodriguesVectorACM(R1);
        double[][] R2 = toRotationMatrixACM(rv1);

        for (int i = 0; i < R1.length; i++) {
            assertArrayEquals(R1[i], R2[i], 1e-6);
        }

        double[] rv2 = toRodriguesVectorACM(R2);
        assertArrayEquals(rv1, rv2, 1e-6);
    }
}
