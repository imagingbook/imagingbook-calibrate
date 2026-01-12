/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import imagingbook.calibration.Camera;
import imagingbook.calibration.ViewTransform;
import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * The mother of all radial  distortion models.
 */
public interface LensDistortionModel {

    /**
     * Copies an existing distortion model instance with modifies parameters.
     * @param params a parameter vector of required length
     * @return
     */
    LensDistortionModel copyOf(double[] params, double error);

    /**
     * Copies an existing distortion model instance with unmodified parameters.
     * @return
     */
    default LensDistortionModel copyOf() {
        return copyOf(getParameters(), getError());
    }

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
     * Returns the average estimation error. May not be implemented.
     * @return the average estimation error
     */
    default double getError() {
        throw new UnsupportedOperationException("getError() not implemented for this type");
    }

    /**
     * Returns a pair of rows in matrix D required for each observed
     * point (see Eqn. (119) of the documentation).
     * @param x the point's hor. coordinate in normalized image space
     * @param y the point's vert. coordinate in normalized image space
     * @param du the point's hor. distance from the projection center in sensor space
     * @param dv the point's vert. distance from the projection center in sensor space
     * @return a 2D matrix with 2 rows and the number of columns equal to the number of distortion parameters
     */
    double[][] getDMatrixRowsUV(double x, double y, double du, double dv);

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

    // -------------------------------------------------------------------------

    // /**
    //  * Stores the average mapping error that occurred when this distortion
    //  * model was estimated (mainly for debugging).
    //  * Note: this method may not be implemented.
    //  * @param error the average error
    //  */
    // default void setAvgError(double error) { }
    //
    // /**
    //  * Retrieves the average error that occurred when this distortion
    //  * model was estimated (mainly for debugging).
    //  * Note: this method may not be implemented.
    //  * @return the average error
    //  */
    // default double getAvgError() {
    //     return 0;
    // }

    // -------------------------------------------------------------------------

    /**
     *  Estimates lens distortion from multiple views, starting from an initial (linear) camera model.
     *  Given an initial estimate of the camera intrinsics (without lens distortion),
     *  the task is to find the optimal distortion parameter vector k = (k0, k1)
     *  by minimum least-squares optimization of
     *  <pre>
     *    D * k = d ,
     *  </pre>
     *  where matrix D is of size 2MN x 2, vector k of size 2, and vector d of size 2MN
     *  (M views with N observed points).
     *  @param cam the initial (linear) camera model
     *  @param views a sequence of extrinsic view transformations
     *  @param modelPntSet the set of 2D model points (on the planar calibration target), one set for each view
     *  @param obsPntSet a sequence of 2D image point sets, one set for each view
     */
    public static LensDistortionModel from(Camera cam, ViewTransform[] views, List<Pnt2d[]> modelPntSet, List<Pnt2d[]> obsPntSet) {
        LensDistortionEstimator estimator = new LensDistortionEstimator(views, modelPntSet, obsPntSet);
        return estimator.getEstimate(cam);
    }
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
