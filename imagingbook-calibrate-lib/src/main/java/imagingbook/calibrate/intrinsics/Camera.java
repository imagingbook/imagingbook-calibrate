/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents the internals of a camera, consisting of a linear (affine) transformation matrix and
 * a non-linear lens distortion model.
 */
public abstract class Camera {

    /**
     * The camera's inner transformation matrix:
     * <pre>
     * | alpha  gamma  uc |
     * |     0   beta  vc |</pre>
     */
    final double[][] A;		// 2 x 3 2D affine transformation matrix
    DistortionModel distortion;

    /**
     * Non-public onstructor.
     * Both arguments may be {@code null}, in which case a dummy camera instance is created for
     * later duplication.
     * @param A vector of 5 linear (affine) camera parameters: alpha, beta, gamma, uc, vc (may be {@code null})
     * @param distortion instance of {@link DistortionModel} (may be {@code null})
     */
    Camera(double[] A, DistortionModel distortion) {
        if (A != null && A.length != 5) {
            throw new IllegalArgumentException("required camera parameters length is 5");
        }
        this.A = (A != null) ?
                new double[][] {
                        { A[0], A[2], A[3] },
                        {   0,  A[1], A[4] }} :
                new double[][] {
                        { 1, 0, 0 },
                        { 0, 1, 0 }
                };
        this.distortion = distortion;
    }

    // ---------------------------------------------------------------------------

    /**
     * Returns the total number of linear and non-linear (distortion) camera parameters,
     * which is 5 plus the (variable) number of distortion parameters.
     * @return the total number of parameters for this camera
     */
    public int getParameterCount() {
        return getLinParameterCount() + getDistParameterCount();
    }

    /**
     * Returns the camera's inner, i.e., linear and distortion parameters as one vector
     * (alpha, beta, gamma, uc, vc, k0, k1, ...).
     * @return the camera's inner parameters (linear and distortion parameters)
     */
    public double[] getParameters() {
        double[] lin = getLinearParameters();  // linear parameters
        double[] dist = getDistortionParameters();
        return Matrix.join(lin, dist);  // concatenate linear/nonlinear coefficients into one vector
    }

    /**
     * Returns the number of linear coefficients in the vector returned by
     * {@link #getLinearParameters()}.
     * @return the number of linear parameters
     */
    public abstract int getLinParameterCount();


    /**
     * Returns the number of distortion parameters (coefficients). The result is 0 if this
     * camera has no distortion model attached.
     * @return the number of distortion parameters
     */
    public int getDistParameterCount() {
        return (distortion != null) ? distortion.getParameterCount() : 0;
    }

    /**
     * Returns the camera's linear parameters as a 5-vector
     * (alpha, beta, gamma, uc, vc).
     * @return the camera's linear parameters
     */
    public abstract double[] getLinearParameters();

    /**
     * Returns the {@link DistortionModel} instance attached to this camera.
     * @return the camera's lens distortion model
     */
    public DistortionModel getDistortion() {
        return this.distortion;
    }

    /**
     * Returns the camera's distortion parameters.
     * @return the camera's distortion parameters
     */
    public double[] getDistortionParameters() {
        return (distortion != null) ?
                distortion.getParameters() :
                new double[0];
    }

    // ---------------------------------------------------------------------------

    /**
     * Creates a new {@link StandardCamera} instance from an existing instance using the supplied parameters.
     * @param A a 2x3 affine transformation matrix
     * @return a new StandardCamera instance with the specified parameters and the same type of lens distortion
     * 	 * model as this instance
     */
    public StandardCamera withParameters(RealMatrix A) {
        double alpha = A.getEntry(0, 0);
        double beta = A.getEntry(1, 1);
        double gamma = A.getEntry(0, 1);
        double uc = A.getEntry(0, 2);
        double vc = A.getEntry(1, 2);
        return new StandardCamera(new double[] {alpha, beta, gamma, uc, vc}, this.distortion);
    }

    /**
     * Creates a new {@link StandardCamera} instance from an existing instance using the supplied parameters.
     * Matches method {@link #getParameters()}, that is
     * <pre>{@code
     *     StandardCamera cam2 = cam1.withParameters(cam1.getParameters());
     * }</pre>
     * creates a new camera which is identical to the original.
     * @param params all linear and non-linear camera parameters
     * @return a new StandardCamera instance with the specified parameters and the same type of lens distortion
     * model as this instance
     */
    public Camera withParameters(double[] params) {
        if (params.length < this.getParameterCount())
            throw new IllegalArgumentException("wrong number of camera parameters: " + params.length);
        int P = getLinParameterCount();
        int Q = getDistParameterCount();
        double[] linParams = Arrays.copyOfRange(params, 0, P);    // = [alpha, beta, gamma, uc, vc] or fewer
        double[] distParams = Arrays.copyOfRange(params, P, P + Q);
        return withParameters(linParams, distParams);
    }

    /**
     * Creates a new {@link StandardCamera} instance from an existing instance using the supplied parameters.
     * @param linParams linear camera parameters
     * @param distParams non-linear (distortion) parameters
     * @return a new StandardCamera instance with the specified parameters and the same type of lens distortion
     * model as this instance
     */
    public Camera withParameters(double[] linParams, double[] distParams) {
        if (linParams.length != getLinParameterCount())
            throw new IllegalArgumentException("wrong number of linear camera parameters: " + linParams.length);
        if (distParams.length != getDistParameterCount())
            throw new IllegalArgumentException("wrong number of distortion camera parameters: " + distParams.length);

        DistortionModel newDist = distortion.withParameters(distParams);
        try {
            return this.getClass().getDeclaredConstructor(double[].class, DistortionModel.class)
                    .newInstance(linParams, newDist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // return new SimpleCamera(linParams, this.distortion.withParameters(distParams));
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the camera's alpha value.
     * @return alpha
     */
    public double getAlpha() {
        return A[0][0];
    }

    /**
     * Returns the camera's beta value.
     * @return beta
     */
    public double getBeta() {
        return A[1][1];
    }

    /**
     * Returns the camera's gamma value.
     * @return gamma
     */
    public double getGamma() {
        return A[0][1];
    }

    /**
     * Returns the camera's uc value.
     * @return uc
     */
    public double getUc() {
        return A[0][2];
    }

    /**
     * Returns the camera's vc value.
     * @return vc
     */
    public double getVc() {
        return A[1][2];
    }

    /**
     * Returns the camera's lens distortion coefficients.
     * @return the vector of lens distortion coefficients
     */
    @Deprecated
    public double[] getK() {
        //return new double[] {distortion.getK0(), distortion.getK1()};
        return (distortion != null) ? distortion.getParameters() : null;
    }

    /**
     * Returns a copy of the camera's inner transformation matrix with contents
     * <pre>
     *    | alpha  gamma  uc |
     *    |     0   beta  vc |</pre>
     * @return the camera's inner transformation matrix (2 x 3)
     */
    public RealMatrix getAffineMatrix() {
        return MatrixUtils.createRealMatrix(A);
    }

    /**
     * Returns the inverse of the camera intrinsic matrix A as a 3x3 matrix (without the last row {0,0,1}). This version
     * uses closed form matrix inversion. Used for rectifying images (i.e., removing lens distortion).
     * @return the inverse of the camera intrinsic matrix A
     */
    public RealMatrix getInverseA() {
        double alpha = A[0][0];
        double beta = A[1][1];
        double gamma = A[0][1];
        double uc = A[0][2];
        double vc = A[1][2];
        double[][] Ai = {
                {1.0/alpha, -gamma/(alpha*beta), (gamma*vc - beta*uc)/(alpha*beta)},
                {0,         1.0/beta,            -vc/beta}};
        return MatrixUtils.createRealMatrix(Ai);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Sets or replaces the distortion model used by this camera.
     * @param distortion the new distortion model
     */
    public void setDistortion(DistortionModel distortion) {
        this.distortion = distortion;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Projects the X/Y world point (in the Z = 0 plane) to image coordinates under the given camera view (extrinsic
     * transformation parameters).
     * @param view the extrinsic transformation parameters
     * @param XY a single X/Y world point (with Z = 0)
     * @return the projected 2D image coordinates
     */
    public double[] project(ViewTransform view, Pnt2d XY) {
        double[] XY0 = new double[] {XY.getX(), XY.getY(), 0};
        return this.project(view, XY0);
    }

    /**
     * Projects the X/Y world points (all in the Z = 0 plane) to image coordinates under the given camera view
     * (extrinsic transformation parameters).
     * @param view the extrinsic transformation parameters
     * @param XYs a set of X/Y world points (with Z = 0)
     * @return the projected 2D image coordinates
     */
    public Pnt2d[] project(ViewTransform view, Pnt2d[] XYs) {
        Pnt2d[] imagePoints = new Pnt2d[XYs.length];
        for (int j = 0; j < XYs.length; j++) {
            double[] uv = project(view, XYs[j]);
            imagePoints[j] = Pnt2d.from(uv);
        }
        return imagePoints;
    }

    /**
     * Projects the given 3D point onto the sensor plane of this camera for the provided extrinsic view parameters.
     * @param view the extrinsic camera (view) parameters
     * @param XYZ a point in 3D world coordinates
     * @return the 2D sensor coordinates of the projected point
     */
    public double[] project(ViewTransform view, double[] XYZ) {
        // map to the ideal projection plane (f = 1)
        double[] xy = projectNormalized(view, XYZ);
        // apply radial lens distortion to the ideal projection
        double[] xyd = (distortion == null) ? xy : distortion.warp(xy);
        // apply the intrinsic camera transformation:
        double[] uv = mapToSensorPlane(xyd);
        return uv;
    }

    /**
     * Projects the given 3D point to ideal projection coordinates for the provided extrinsic view
     * parameters. The world point is specified as a 2D coordinate in the Z = 0 plane.
     * @param view the extrinsic camera (view) parameters
     * @param XY a point in 3D world coordinates (Z = 0)
     * @return the 2D ideal projection
     */
    public double[] projectNormalized(ViewTransform view, Pnt2d XY) {
        double[] XY0 = {XY.getX(), XY.getY(), 0};
        return projectNormalized(view, XY0);
    }

    /**
     * Projects the given 3D point to ideal projection coordinates for the provided extrinsic view
     * parameters.
     * @param view the extrinsic camera (view) parameters
     * @param XYZ a point in 3D world coordinates
     * @return the 2D ideal projection
     */
    public double[] projectNormalized(ViewTransform view, double[] XYZ) {
        double[] XYZc = view.applyTo(XYZ);
        // Calculate normalized projection coordinates (f = 1):
        final double x = XYZc[0] / XYZc[2];
        final double y = XYZc[1] / XYZc[2];
        return new double[] {x, y};
    }

    /**
     * Maps from the ideal projection plane to sensor coordinates, using the camera's intrinsic
     * parameters. No lens distortion is applied.
     * @param xy a 2D point on the ideal projection plane
     * @return the resulting 2D sensor coordinate
     */
    public double[] mapToSensorPlane(double[] xy) {
        final double x = xy[0];
        final double y = xy[1];
        final double u = A[0][0] * x + A[0][1] * y + A[0][2];
        final double v =               A[1][1] * y + A[1][2];
        return new double[] {u, v};
    }

    // -------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[alpha=%.2f, beta=%.2f, gamma=%.2f, uc=%.2f, vc=%.2f, %s]",
                this.getClass().getSimpleName(),
                getAlpha(), getBeta(), getGamma(), getUc(), getVc(), getDistortion());
    }
}
