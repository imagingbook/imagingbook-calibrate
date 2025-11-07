/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

/**
 * Lens model with radial-only distortion.
 */
public interface RadialDistortion extends LensDistortion {

    /**
     * Forward radial distortion function.  Returns the distorted
     * radius R from the distorted radius r, both
     * measured from the center = (0,0) of the ideal projection plane.
     * @param r the original radius of a point in the ideal projection plane
     * @return the distorted radius
     */
    double fRad(double r);

    /**
     * Inverse radial distortion function. Returns the original (undistorted)
     * radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection plane.
     * @param R the distorted radius of a point in the ideal projection plane
     * @return the undistorted radius
     */
    double fRadInv(double R);

    @Override
    default double[] warp(double[] xy) {
        final double x = xy[0];
        final double y = xy[1];
        final double r = Math.sqrt(x * x + y * y);  // undistorted radius
        if (r < 1e-6)
            return new double[] {0, 0};
        // final double R = warp(r);        // distorted radius
        final double s = fRad(r) / r;
        return new double[] {s * x, s* y};
    }

    @Override
    default double[] unwarp(double[] xyd) {
        final double xd = xyd[0];
        final double yd = xyd[1];
        final double R = Math.sqrt(xd * xd + yd * yd);	// distorted radius
        if (R < 1e-6)
            return new double[] {0, 0};
        // final double r = unwarp(R);					// undistorted radius
        final double s = fRadInv(R) / R;
        return new double[] {s * xd, s * yd};
    }

}
