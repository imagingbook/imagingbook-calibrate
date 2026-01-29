/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import imagingbook.calibrate.distortion.DistortionModel;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * A simplified camera model with just one focal length (alpha = beta) and no skew (gamma = 0).
 */
public class SimpleCamera extends Camera {

    /**
     * Constructor.
     * @param A vector of 3 linear camera parameters: alpha=beta, uc, vc
     * @param distortion instance of {@link DistortionModel}
     */
    public SimpleCamera(double[] A, DistortionModel distortion) {
        // alpha, beta, gamma, uc, vc
        super(new double[] {A[0], A[0], 0, A[1], A[2]}, distortion);
        checkLength(A, 3);
    }

    // // create a dummy camera with no distortion
    // public SimpleCamera() {
    //     super(null, null);
    // }

    @Override
    public StandardCamera withParameters(RealMatrix A) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public int getLinParameterCount() {
        return 3;
    }

    /**
     * Returns the camera's linear parameters as a 3-vector
     * (alpha=beta, uc, vc).
     * @return the camera's linear parameters
     */
    @Override
    public double[] getLinearParameters() {
        // alpha=beta, uc, vc
        return new double[] { A[0][0], A[0][2], A[1][2] };
    }

    @Override
    public int getParameterIdxAlpha() { return 0;}

    @Override
    public int getParameterIdxBeta() { return -1; }

    @Override
    public int getParameterIdxGamma() { return -1; }

    @Override
    public int getParameterIdxUc() { return 1; }

    @Override
    public int getParameterIdxVc() { return 2; }

    // -------------------------------------------------------------------

    public static SimpleCamera fromHomographies(RealMatrix[] homographies, int imgWidth, int imgHeight) {
        IntrinsicsEstimator estimator = new IntrinsicsEstimatorConstrained(imgWidth, imgHeight);
        RealMatrix A = estimator.estimateIntrinsics(homographies);
        double alpha = A.getEntry(0, 0);
        double beta = A.getEntry(1, 1);
        double gamma = A.getEntry(0, 1);
        double uc = A.getEntry(0, 2);
        double vc = A.getEntry(1, 2);
        double ab = (alpha + beta) / 2;             // TODO: temporary fix, should only be one value from estimator!
        double[] params = new double[] {ab, uc, vc};
        return new SimpleCamera(params, null);
    }

}
