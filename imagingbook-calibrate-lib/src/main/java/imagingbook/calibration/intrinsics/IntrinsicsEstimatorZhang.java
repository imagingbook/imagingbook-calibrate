/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.intrinsics;

import imagingbook.calibration.util.MathUtil;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.linear.CholeskyDecomposition;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;

/**
 * Version 1 (Zhang's original closed form solution). Estimates intrinsic camera parameters from multiple
 * homographies.
 */
public class IntrinsicsEstimatorZhang implements IntrinsicsEstimator {

    @Override
    public RealMatrix estimate(RealMatrix[] homographies) {
        final int M = homographies.length;
        int rows = 2 * M + 1;
        double[][] V = new double[rows][];

        for (int i = 0; i < M; i++) {
            RealMatrix Hi = homographies[i];
            V[2*i + 0] = getVpq(Hi, 0, 1); // v01
            V[2*i + 1] = Matrix.subtract(getVpq(Hi, 0, 0), getVpq(Hi, 1, 1)); // v00-v11
        }
        // impose skewless constraint (gamma = 0) by default
        V[rows - 1] = new double[] { 0, 1, 0, 0, 0, 0 };

        // if (M == 2) {
        // 	V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
        // }

        RealMatrix VM = MatrixUtils.createRealMatrix(V);
        double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0

        // *************************************************************************************
        PrintPrecision.set(8);
        System.out.println("\nVM = " + Matrix.toString(VM));
        SingularValueDecomposition svd = new SingularValueDecomposition(VM);
        System.out.println("\nsingular vals = " + Matrix.toString(svd.getSingularValues()));
        // System.out.println("\ndecomp V = " + Matrix.toString(svd.getV()));
        System.out.println("\nhom. solution b = " + Matrix.toString(b));

        // *************************************************************************************

        RealMatrix B = MatrixUtils.createRealMatrix(new double[][]
                {{b[0], b[1], b[3]},
                 {b[1], b[2], b[4]},
                 {b[3], b[4], b[5]}});

        System.out.println("\nB = " + Matrix.toString(B));

        if (B.getEntry(0, 0) < 0 || B.getEntry(1, 1) < 0 || B.getEntry(2, 2) < 0) {
            B = B.scalarMultiply(-1);	// make sure B is positive definite
        }

        CholeskyDecomposition cd = new CholeskyDecomposition(B);
        RealMatrix L = cd.getL();
        RealMatrix A = MatrixUtils.inverse(L).transpose().scalarMultiply(L.getEntry(2, 2));
        return A;
    }
}
