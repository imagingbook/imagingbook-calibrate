/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.intrinsics;

import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Estimates intrinsic camera parameters assuming that there is no skew (gamma = 0) and
 * the principal projection point is at the image center. This method is numerically
 * much more robust than the full-parameter estimation used in
 * {@link IntrinsicsEstimatorZhang}.
 */
public class IntrinsicsEstimatorConstrained implements IntrinsicsEstimator {

    private final double uc;
    private final double vc;
    private final RealMatrix T;

    /**
     * Constructor.
     * @param width the image width (in pixels)
     * @param height the image height (in pixels)
     */
    public IntrinsicsEstimatorConstrained(int width, int height) {
        System.out.println("IntrinsicsEstimatorConstrained(): width=" + width + ", height=" + height);
        this.uc = 0.5 * width;
        this.vc = 0.5 * height;
        this.T = new Array2DRowRealMatrix(new double[][]
                {{1, 0, -uc},
                 {0, 1, -vc},
                 {0, 0, 1}}, false);
    }

    @Override
    public RealMatrix estimate(RealMatrix[] homographies) {
        final int M = homographies.length;
        double[][] V = new double[2 * M][];
        double[] c = new double[2 * M];
        double PRECOND = 1e6;    // constant to precondition the linear system (probably not needed)

        for (int k = 0; k < M; k++) {
            checkIfNormalized(homographies[k]);
            double[][] H = T.multiply(homographies[k]).getData();    // translate homography
            V[2 * k + 0] = new double[] {H[0][0] * H[0][1], H[1][0] * H[1][1]};
            V[2 * k + 1] = new double[] {sqr(H[0][0]) - sqr(H[0][1]), sqr(H[1][0]) - sqr(H[1][1])};
            c[2 * k + 0] = -H[2][0] * H[2][1] * PRECOND;
            c[2 * k + 1] = (sqr(H[2][1]) - sqr(H[2][0])) * PRECOND;
        }

        // PrintPrecision.setTo(10);
        // System.out.println("IntrinsicsEstimatorConstrained: V = \n" + Matrix.toString(V));
        // System.out.println("IntrinsicsEstimatorConstrained: c = " + Matrix.toString(c));

        RealMatrix VV = new Array2DRowRealMatrix(V, false);
        RealVector cc = new ArrayRealVector(c, false);
        // System.out.println("IntrinsicsEstimatorConstrained: cond(V) = " +
        //         Matrix.getConditionNumber(VV));

        // solve V.w = c
        DecompositionSolver solver = new QRDecomposition(VV).getSolver();
        RealVector w = solver.solve(cc);
        // System.out.println("IntrinsicsEstimatorConstrained: w = " + Matrix.toString(w));

        double wa = w.getEntry(0);
        double wb = w.getEntry(1);
        if  (wa < 0 || wb < 0) {
            throw new RuntimeException("wa, wb must both be positive: " + wa + "/" + wb);
        }

        double alpha = Math.sqrt(PRECOND / w.getEntry(0));
        double beta  = Math.sqrt(PRECOND / w.getEntry(1));
        return new Array2DRowRealMatrix(new double[][]
                {{alpha, 0, uc},
                 {0,  beta, vc},
                 {0,   0,   1}}, false);
    }

    private static void checkIfNormalized(RealMatrix homography) {
        if (Double.compare(homography.getEntry(2, 2), 1.0) != 0) {
            throw new RuntimeException("homography matrix must be normalized");
        }
    }
}
