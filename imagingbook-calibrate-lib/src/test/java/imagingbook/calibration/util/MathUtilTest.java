/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.util;

import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import org.junit.Test;

import static imagingbook.calibration.util.MathUtil.fromRowPackedVector;
import static imagingbook.calibration.util.MathUtil.getRowPackedVector;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MathUtilTest {
    // TODO: check more examples!

    @Test
    public void testHomogeneousSystem1() {
        // Lay, Linear Algebra (5ed), p. 44 (works)
        // n = 3, r = 2: exact solution
        double[][] A = {
                {3, 5, -4},
                {-3, -2, 4},
                {6, 1, -8}};
        RealMatrix M = MatrixUtils.createRealMatrix(A);
        RealVector x = MathUtil.solveHomogeneousSystem(M);
        assertNotNull("solveHomogeneousSystem() found no solution", x);
        // check M * x = 0
        RealVector ax = M.operate(x);
        assertArrayEquals(Matrix.zeroVector(ax.getDimension()), ax.toArray(), 1e-6);
    }

    @Test
    public void testHomogeneousSystem2() {
        // n = 3, r = 2: exact solution
        double[][] A = {
                {1, 3, -2},
                {2, -1, 4},
                {1, -11, 14}};

        RealMatrix M = MatrixUtils.createRealMatrix(A);
        RealVector x = MathUtil.solveHomogeneousSystem(M);
        assertNotNull("solveHomogeneousSystem() found no solution", x);
        // check M * x = 0
        RealVector ax = M.operate(x);
        assertArrayEquals(Matrix.zeroVector(ax.getDimension()), ax.toArray(), 1e-6);
    }

    @Test
    public void testHomogeneousSystem3() {
        // n = 4, r = 3: least squares solution
        double[][] A = {
                {1, -1, -1, 3},
                {1, 1, -2, 1},
                {4, -2, 4, 1}};

        RealMatrix M = MatrixUtils.createRealMatrix(A);
        RealVector x = MathUtil.solveHomogeneousSystem(M);
        assertNotNull("solveHomogeneousSystem() found no solution", x);
    }

    // @Test(expected = IllegalArgumentException.class)
    // public void test4() {
    //     // n = 4, r = 3: least squares solution
    //     double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}};
    //     RealMatrix M = MatrixUtils.createRealMatrix(A);
    //     RealVector x = MathUtil.solveHomogeneousSystem(M);
    // }

    // @Test(expected = IllegalArgumentException.class)
    // public void test5() {
    //     // n = 4, r = 3: least squares solution
    //     double[][] A = {{1, 2, 3}, {4, 5, 6}, {9, 8, 0}, {-3, 7, 2}};
    //     RealMatrix M = MatrixUtils.createRealMatrix(A);
    //     RealVector x = MathUtil.solveHomogeneousSystem(M);
    // }

    @Test
    public void testHomogeneousSystem6() {
        // n = 4, r = 3: Mathematica Nullspace example
        double[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};

        RealMatrix M = MatrixUtils.createRealMatrix(A);
        RealVector x = MathUtil.solveHomogeneousSystem(M);
        assertNotNull("solveHomogeneousSystem() found no solution", x);

        // System.out.println("x = " + x.mapMultiply(1/x.getEntry(2)));
        RealVector ax = M.operate(x);
        assertArrayEquals(Matrix.zeroVector(ax.getDimension()), ax.toArray(), 1e-6);
    }

    // --- Quaternion stuff --------------------------------------------

    // Lerp public static double[] Lerp(double[] t0, double[] t1, double alpha) {

    @Test
    public void testLerpPlainArray1() {
        double[] ta = {1};
        double[] tb = {5};
        double[] tab = MathUtil.Lerp(ta, tb, 0.3);
        // System.out.println(Arrays.toString(tab));
        assertArrayEquals(new double[] {2.2}, tab, 1e-6);
    }

    @Test
    public void testLerpPlainArray3() {
        double[] ta = {1, -2, 7};
        double[] tb = {5, 0, 3};
        double[] tab = MathUtil.Lerp(ta, tb, 0.3);
        // System.out.println(Arrays.toString(tab));
        assertArrayEquals(new double[] {2.2, -1.4, 5.8}, tab, 1e-6);
    }

    @Test
    public void testLerpPlainArray5() {
        double[] ta = {1, -2, 7, 9, -1};
        double[] tb = {5, 0, 3, 2, 7};
        double[] tab = MathUtil.Lerp(ta, tb, 0.3);
        // System.out.println(Arrays.toString(tab));
        assertArrayEquals(new double[] {2.2, -1.4, 5.8, 6.9, 1.4}, tab, 1e-6);
    }

    @Test
    public void crossProduct3x3Test() {
    }

    @Test
    public void getRowPackedVectorTest1() {
        RealMatrix M = MatrixUtils.createRealMatrix(new double[][]
                {{1, 2, 3},
                 {4, 5, 6},
                 {7, 8, 9}});
        RealVector V = getRowPackedVector(M);
        assertArrayEquals(new double[] {1,2,3,4,5,6,7,8,9}, V.toArray(), 1e-6);
    }

    @Test   // vector shorter than matrix
    public void getRowPackedVectorTest2() {
        RealMatrix M = MatrixUtils.createRealMatrix(new double[][]
                {{1, 2, 3},
                        {4, 5, 6},
                        {7, 8, 9}});
        RealVector V = getRowPackedVector(M, 7);
        assertEquals(7, V.getDimension());
        assertArrayEquals(new double[] {1,2,3,4,5,6,7}, V.toArray(), 1e-6);
    }

    @Test   // vector longer than matrix
    public void getRowPackedVectorTest3() {
        RealMatrix M = MatrixUtils.createRealMatrix(new double[][]
                {{1, 2, 3},
                        {4, 5, 6},
                        {7, 8, 9}});
        RealVector V = getRowPackedVector(M, 11);
        assertEquals(11, V.getDimension());
        assertArrayEquals(new double[] {1,2,3,4,5,6,7,8,9,0,0}, V.toArray(), 1e-6);
    }

    @Test
    public void fromRowPackedVectorTest1() {
        double[] vec = {1,2,3,4,5,6,7,8,9};
        RealMatrix M = fromRowPackedVector(MatrixUtils.createRealVector(vec), 3, 3);
        assertArrayEquals(new double[] {1,2,3}, M.getRow(0), 1e-6);
        assertArrayEquals(new double[] {4,5,6}, M.getRow(1), 1e-6);
        assertArrayEquals(new double[] {7,8,9}, M.getRow(2), 1e-6);
    }

    @Test       // vector too short
    public void fromRowPackedVectorTest2() {
        double[] vec = {1,2,3,4,5,6,7};
        RealMatrix M = fromRowPackedVector(MatrixUtils.createRealVector(vec), 3, 3);
        assertArrayEquals(new double[] {1,2,3}, M.getRow(0), 1e-6);
        assertArrayEquals(new double[] {4,5,6}, M.getRow(1), 1e-6);
        assertArrayEquals(new double[] {7,0,0}, M.getRow(2), 1e-6);
    }

    @Test       // vector too long
    public void fromRowPackedVectorTest3() {
        double[] vec = {1,2,3,4,5,6,7,8,9,10,11,12};
        RealMatrix M = fromRowPackedVector(MatrixUtils.createRealVector(vec), 3, 3);
        assertArrayEquals(new double[] {1,2,3}, M.getRow(0), 1e-6);
        assertArrayEquals(new double[] {4,5,6}, M.getRow(1), 1e-6);
        assertArrayEquals(new double[] {7,8,9}, M.getRow(2), 1e-6);
    }

    // MatrixUtils.
}