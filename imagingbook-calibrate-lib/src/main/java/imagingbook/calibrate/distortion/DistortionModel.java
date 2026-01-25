/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.math.Matrix;

import java.util.Locale;

/**
 * The mother of all radial distortion models.
 */
public abstract class DistortionModel {

    final double[] parameters;      // variable number of distortion parameters

    DistortionModel(double[] parameters) {
        this.parameters = parameters;
    }

    public static DistortionModel create(DistortionModelType type) {
        switch (type) {
            case Radial2Term ->     { return new Radial2TermDistortion(); }
            case Radial3Term ->     { return new Radial3TermDistortion(); }
            case RadialLateral ->   { return new RadialLateralDistortion(); }
            case PtLens ->          { return new PtLensDistortion(); }
            default -> throw new  IllegalArgumentException("Unknown DistortionModel type");
        }
    }

    public static DistortionModel create(DistortionModelType type, Camera cam, int imgWidth, int imgHeight) {
        switch (type) {
            case Radial2Term ->     { return new Radial2TermDistortion(); }
            case Radial3Term ->     { return new Radial3TermDistortion(); }
            case RadialLateral ->   { return new RadialLateralDistortion(); }
            case PtLens ->          { return new PtLensDistortion(cam, imgWidth, imgHeight); }
            default -> throw new  IllegalArgumentException("Unknown DistortionModel type");
        }
    }

    /**
     * Copies this distortion model instance with modified parameters.
     * Passing {@code null} to {@code params} creates a zero-value parameter vector.
     * @param params a parameter vector of required length
     * @return a new distortion model instance of the same type as the original
     */
    public abstract DistortionModel withParameters(double[]  params);

    // -------------------------------------------------------------------------------

    /**
     * Returns the number of parameters required for this distortion model.
     * @return the number of parameters
     */
    public int getParameterCount() {
        return parameters.length;
    }

    /**
     * Returns a vector with the parameters of this distortion model.
     * @return a vector of parameters
     */
    public final double[] getParameters() {
        return parameters;
    }

    public double getParameter(int i) {
        double[] parameters = getParameters();
        if (i < 0 || i >= parameters.length)
            throw new IllegalArgumentException("invalid distortion parameter index: " + i);
        return parameters[i];
    }

    /**
     * Returns the scale factor applied to normalized projection coordinates for performing
     * the distortion transformation. This method usually returns 1, except for specific
     * distortion models, such as {@link PtLensDistortion}, which must override this method.
     * @return the scale factor for the distortion mapping
     */
    public double getScale() {
        return 1.0;
    }


    // -------------------------------------------------------------------------------

    /**
     * Returns a pair of rows in matrix D required for each observed
     * point (see Eqn. (119) of the documentation).
     * @param x the point's hor. coordinate in normalized image space
     * @param y the point's vert. coordinate in normalized image space
     * @param du the point's hor. distance from the projection center in sensor space
     * @param dv the point's vert. distance from the projection center in sensor space
     * @return a 2D matrix with 2 rows and the number of columns equal to the number of distortion parameters
     */
    abstract double[][] getDMatrixRowsUV(double x, double y, double du, double dv);

    /**
     * Applies lens distortion to a point in the ideal 2D projection.
     * @param xy a 2D point in the ideal projection
     * @return the lens-distorted position in the ideal projection
     */
    public abstract double[] warp(double[] xy);

    /**
     * Applies inverse lens distortion to a given point in the ideal image plane.
     * @param xyd a distorted 2D point in the ideal image plane
     * @return the undistorted point
     */
    public abstract double[] unwarp(double[] xyd);

    // --------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format(Locale.US, "%s%s, %s",
                this.getClass().getSimpleName(),
                Matrix.toString(parameters),
                (this instanceof ScaledDistortionSpace) ? "scale=" + getScale() : "");
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
