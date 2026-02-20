/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.common.geometry.basic.Pnt2d;

// TODO: align with Mapping2D interface!
/**
 * Lens model with radial-only distortion.
 */
public abstract class RadialDistortion extends DistortionModel {

    RadialDistortion(double[]  parameters) {
        super(parameters);
    }

    // RadialDistortion(double[]  parameters, double scale) {
    //     super(parameters, scale);
    // }

    /**
     * Forward radial distortion function.  Returns the distorted
     * radius R from the distorted radius r, both
     * measured from the center = (0,0) of the ideal projection plane.
     * @param r the original radius of a point in the ideal projection plane
     * @return the distorted radius
     */
    public abstract double fRad(double r);

    /**
     * Inverse radial distortion function. Returns the original (undistorted)
     * radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection plane.
     * @param R the distorted radius of a point in the ideal projection plane
     * @return the undistorted radius
     */
    public abstract double fRadInv(double R);

    @Override
    public double[] warp(double[] xy) {
        final double x = xy[0];
        final double y = xy[1];
        final double r = Math.hypot(x, y);  // undistorted radius
        if (r < 1e-6)
            return new double[] {0, 0};
        // final double R = warp(r);        // distorted radius
        final double s = fRad(r) / r;
        return new double[] {s * x, s * y};
    }

    // @Override
    // public Pnt2d warp(Pnt2d xy) {
    //     final double x = xy.getX();
    //     final double y = xy.getY();
    //     final double r = Math.hypot(x, y);  // undistorted radius
    //     if (r < 1e-6)
    //         return Pnt2d.from(0, 0);
    //     // final double R = warp(r);        // distorted radius
    //     final double s = fRad(r) / r;
    //     return Pnt2d.from(s * x, s* y);
    // }



    // ----------------------------------------

    @Override
    public double[] unwarp(double[] xyd) {
        final double xd = xyd[0];
        final double yd = xyd[1];
        final double R = Math.hypot(xd, yd);	// distorted radius
        if (R < 1e-6)
            return new double[] {0, 0};
        final double s = fRadInv(R) / R;
        return new double[] {s * xd, s * yd};
    }

    // @Override
    // public Pnt2d unwarp(Pnt2d xyd) {
    //     final double xd = xyd.getX();
    //     final double yd = xyd.getY();
    //     final double R = Math.hypot(xd, yd);	// distorted radius
    //     if (R < 1e-6)
    //         return Pnt2d.from(0, 0);
    //     final double s = fRadInv(R) / R;
    //     return Pnt2d.from(s * xd, s * yd);
    // }

}
