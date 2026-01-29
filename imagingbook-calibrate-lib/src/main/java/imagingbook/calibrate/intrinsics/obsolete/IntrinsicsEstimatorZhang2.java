/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics.obsolete;

import imagingbook.calibrate.intrinsics.IntrinsicsEstimator;
import imagingbook.calibrate.util.MathUtil;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * Version 2 (Zhang's corrected closed form solution). Estimates intrinsic camera parameters from multiple
 * homographies.
 */
@Deprecated
public class IntrinsicsEstimatorZhang2 implements IntrinsicsEstimator {

    @Override
    public RealMatrix estimateIntrinsics(RealMatrix[] homographies) {
        final int M = homographies.length;
        int rows = 2 * M;
        double[][] V = new double[rows][];

        for (int i = 0; i < M; i++) {
            RealMatrix H = homographies[i];
            try (var prec = PrintPrecision.set(8)) {
                System.out.println("IntrinsicsEstimatorZhang2: H" + i + " = \n" + Matrix.toString(H));
            }
            V[2*i] = getVpq(H, 0, 1); // v01
            V[2*i + 1] = Matrix.subtract(getVpq(H, 0, 0), getVpq(H, 1, 1)); // v00-v11
        }

        if (M == 2) {
            V[V.length - 1] = new double[] { 0, 1, 0, 0, 0, 0 };
        }

        System.out.println("IntrinsicsEstimatorZhang2: V = \n" + Matrix.toString(V) );

        RealMatrix VM = MatrixUtils.createRealMatrix(V);
        double[] b = MathUtil.solveHomogeneousSystem(VM).toArray();	// solve VM.b=0

        System.out.println("IntrinsicsEstimatorZhang3: b = \n" + Matrix.toString(b) );

        final double vc 	= (b[1] * b[3] - b[0] * b[4]) / (b[0] * b[2] - b[1] * b[1]);
        System.out.println("    vc = " + vc);
        final double lambda = b[5] - (b[3] * b[3] + vc * (b[1] * b[3] - b[0] * b[4])) / b[0];
        System.out.println("    lambda = " + lambda);
        final double alpha 	= Math.sqrt(lambda / b[0]);
        System.out.println("    alpha = " + alpha);
        final double beta 	= Math.sqrt(lambda * b[0] / (b[0] * b[2] - b[1] * b[1]));
        System.out.println("    beta = " + beta);
        final double gamma 	= -b[1] * alpha * alpha * beta / lambda;
        System.out.println("    gamma = " + gamma);
        final double uc 	= gamma * vc / beta - b[3] * alpha * alpha / lambda;	// beta! 1998 report seems correct!
        System.out.println("    uc = " + uc);

        RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
                { alpha, gamma, uc },
                { 0, beta, vc },
                { 0, 0, 1 }
        });

        return A;
    }

    static double[] getVpq(RealMatrix H, int p, int q) {
        return new double[] {
                H.getEntry(0, p) * H.getEntry(0, q),
                H.getEntry(0, p) * H.getEntry(1, q) + H.getEntry(1, p) * H.getEntry(0, q),
                H.getEntry(1, p) * H.getEntry(1, q),
                H.getEntry(2, p) * H.getEntry(0, q) + H.getEntry(0, p) * H.getEntry(2, q),
                H.getEntry(2, p) * H.getEntry(1, q) + H.getEntry(1, p) * H.getEntry(2, q),
                H.getEntry(2, p) * H.getEntry(2, q)
        };
    }
}
