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
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import java.util.Arrays;

/**
 * Version 3 (WB's closed form solution). Estimates intrinsic camera parameters from multiple homographies.
 */
@Deprecated
public class IntrinsicsEstimatorZhang3 implements IntrinsicsEstimator {

    @Override
    public RealMatrix estimate(RealMatrix[] homographies) {
        final int M = homographies.length;
        int rows = 2 * M;
        double[][] V = new double[rows][];

        for (int i = 0; i < M; i++) {
            RealMatrix H = homographies[i];
            PrintPrecision.set(8);
            System.out.println("IntrinsicsEstimatorZhang3: H" + i + " = \n" + Matrix.toString(H) );
            V[2 * i] = getVpq(H, 0, 1); // v01
            V[2 * i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
        }

        if (M == 2) {
            V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
        }
        System.out.println("IntrinsicsEstimatorZhang3: V = \n" + Matrix.toString(V) );

        RealMatrix VM = MatrixUtils.createRealMatrix(V);
        double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0

        System.out.println("IntrinsicsEstimatorZhang3: b = \n" + Matrix.toString(b) );


        final double w = b[0]*b[2]*b[5] - b[1]*b[1]*b[5] - b[0]*b[4]*b[4] + 2*b[1]*b[3]*b[4] - b[2]*b[3]*b[3];
        final double d = b[0] * b[2] - b[1] * b[1];
        final double uc = (b[1] * b[4] - b[2] * b[3]) / d;
        final double vc = (b[1] * b[3] - b[0] * b[4]) / d;
        final double alpha 	= Math.sqrt(w / (d * b[0]));
        final double beta 	= Math.sqrt(w / (d * d) * b[0]);
        final double gamma 	= Math.sqrt(w / (d * d * b[0])) * b[1];

        RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
                { alpha, gamma, uc },
                { 0, beta, vc },
                { 0, 0, 1 }
        });

        return A;
    }
}
