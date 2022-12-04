/*
 *  This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge.
 * All rights reserved. Visit https://imagingbook.com for additional details.
 */
package imagingbook.calibration.zhang.util;

import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilTest {
    // TODO: check more examples!

    @Test
    public void test1() {
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
    public void test2() {
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
    public void test3() {
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
    public void test6() {
        // n = 4, r = 3: Mathematica Nullspace example
        double[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};

        RealMatrix M = MatrixUtils.createRealMatrix(A);
        RealVector x = MathUtil.solveHomogeneousSystem(M);
        assertNotNull("solveHomogeneousSystem() found no solution", x);

        // System.out.println("x = " + x.mapMultiply(1/x.getEntry(2)));
        RealVector ax = M.operate(x);
        assertArrayEquals(Matrix.zeroVector(ax.getDimension()), ax.toArray(), 1e-6);
    }
}