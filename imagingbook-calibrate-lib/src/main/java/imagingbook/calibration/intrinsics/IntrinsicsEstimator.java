/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.intrinsics;

import org.apache.commons.math4.legacy.linear.RealMatrix;

public interface IntrinsicsEstimator {

    public RealMatrix estimate(RealMatrix[] homographies);

    // version without transpose
    default double[] getVpq(RealMatrix H, int p, int q) {
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
