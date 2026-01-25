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
public class SimpleCamera extends AbstractCamera {

    /**
     * Constructor.
     * @param a vector of 3 linear camera parameters: alpha=beta, uc, vc
     * @param distortion instance of {@link DistortionModel}
     */
    public SimpleCamera(double[] a, DistortionModel distortion) {
        // alpha, beta, gamma, uc, vc
        super(new double[] {a[0], a[0], 0, a[1], a[2]}, distortion);
    }

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

    // --------------------------------------------------------------------------------------------


}
