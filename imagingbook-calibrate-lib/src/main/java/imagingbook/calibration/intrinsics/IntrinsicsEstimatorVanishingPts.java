/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.intrinsics;

import org.apache.commons.math4.legacy.linear.RealMatrix;

public class IntrinsicsEstimatorVanishingPts implements IntrinsicsEstimator {

    @Override
    public RealMatrix estimate(RealMatrix[] homographies) {
        return null;
    }

}
