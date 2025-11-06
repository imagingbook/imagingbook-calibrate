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
public interface LensDistortionModel {

    /**
     * Copies an existing distortion model instance.
     * If the correct number of parameters is supplied, a new instance of
     * this class with the new parameters is returned.
     * If no parameters are supplied, the original instance is duplicated.
     * An exception is thrown if any other number of parameters is supplied.
     * @param params a parameter vector of required length
     * @return
     */
    LensDistortionModel copyOf(double... params);

    /**
     * Returns the number of parameters required for this distortion model.
     * @return the number of parameters
     */
    default int getParameterCount() {
        return getParameters().length;
    }

    /**
     * Returns a vector with the parameters of this distortion model.
     * @return a vector of parameters
     */
    double[] getParameters();

    default double getParameter(int i) {
        double[] parameters = getParameters();
        if (i < 0 || i >= parameters.length)
            throw new IllegalArgumentException("invalid distortion parameter index: " + i);
        return parameters[i];
    }

    /**
     * Returns the first (X) of two rows in matrix D required for each observed
     * point (see Eqn. (119) of the documentation).
     * @param x the point's hor. coordinate in normalized image space
     * @param y the point's vert. coordinate in normalized image space
     * @param du the point's hor. distance from the projection center in sensor space
     * @param dv the point's vert. distance from the projection center in sensor space
     * @return an array of the same length as the number of distortion parameters
     */
    double[] getDMatrixRowU(double x, double y, double du, double dv);

    /**
     * Returns the second (Y) of two rows in matrix D required for each observed
     * point (see Eqn. (119) of the documentation).
     * @param x the point's hor. coordinate in normalized image space
     * @param y the point's vert. coordinate in normalized image space
     * @param du the point's hor. distance from the projection center in sensor space
     * @param dv the point's vert. distance from the projection center in sensor space
     * @return an array of the same length as the number of distortion parameters
     */
    double[] getDMatrixRowV(double x, double y, double du, double dv);

    /**
     * Applies lens distortion to a point in the ideal 2D projection.
     * @param xy a 2D point in the ideal projection
     * @return the lens-distorted position in the ideal projection
     */
    double[] warp(double[] xy);

    /**
     * Applies inverse lens distortion to a given point in the ideal image plane.
     * @param xyd a distorted 2D point in the ideal image plane
     * @return the undistorted point
     */
    double[] unwarp(double[] xyd);
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
