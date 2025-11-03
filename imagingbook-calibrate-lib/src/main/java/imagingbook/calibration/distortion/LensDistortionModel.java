/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

/**
 * The mother of all radial  distortion models.
 */
public interface LensDistortionModel {      // extends Copyable<LensDistortionModel>

    /**
     * Copy an existing distortion model instance from a suitable parameter vector.
     * @param params a parameter vector of required length
     * @return
     */
    LensDistortionModel copyOf(double[] params);

    default int getParameterCount() {
        return getParameters().length;
    }

    double[] getParameters();
    double getParameter(int i);

    double[] getDMatrixRowU(double x, double y, double du, double dv);
    double[] getDMatrixRowV(double x, double y, double du, double dv);

    /**
     * Applies lens distortion to a point in the ideal 2D projection.
     *
     * @param xy a 2D point in the ideal projection
     * @return the lens-distorted position in the ideal projection
     */
    double[] warp(double[] xy);


    /**
     * Applies inverse lens distortion to a given point in the ideal image plane.
     *
     * @param xyd a distorted 2D point in the ideal image plane
     * @return the undistorted point
     */
    double[] unwarp(double[] xyd);

    // outdated methods for testing radial warping only! -----------------

    default double warp(double r) {
        double[] xy2 = warp(new double[] {r, 0});
        return  xy2[0];
    }

    default double unwarp(double rr) {
        double[] xy2 = unwarp(new double[] {rr, 0});
        return  xy2[0];
    }
    // ------------------------------------------------------------------

}

/*
https://chatgpt.com/share/690646c8-4ee0-8006-9d14-04d216f841ba

| Model                        | Formula                                | Terms | Tangential? | Notes                                |
| ---------------------------- | -------------------------------------- | ----- | ----------- | ------------------------------------ |
| **Brown–Conrady**            | (r' = r(1 + k_1r^2 + k_2r^4 + k_3r^6)) | 3     | ✅ Yes       | Classical photogrammetric model      |
| **Zhang (EasyCalib)**        | (r' = r(1 + k_0r^2 + k_1r^4))          | 2     | ❌ No        | Simplified Brown–Conrady             |
| **PTLens**                   | (r' = r(1 + a r^2 + b r^4 + c r^6))    | 3     | ❌ No        | Empirical, image-processing oriented |
| **Division (Fitzgibbon)**    | (r' = \frac{r}{1 + \lambda r^2})       | 1     | ❌ No        | Analytically invertible              |
| **Fisheye / Kannala–Brandt** | (r' = f(\theta))                       | 4–8   | ❌ No        | Angle-based, wide FoV                |


“Zhang’s polynomial radial distortion model”
or simply
“Two-coefficient polynomial radial distortion model.”

 */
