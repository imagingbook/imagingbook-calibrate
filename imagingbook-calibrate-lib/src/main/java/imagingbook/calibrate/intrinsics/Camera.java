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

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents the internals of a camera, consisting of a linear (affine) transformation matrix and
 * a non-linear lens distortion model.
 * Instances of {@link Camera} are considered immutable. Various constructors are provided for
 * copying instances with modified parameters.
 */
public class Camera {

	/**
	 * The camera's inner transformation matrix:
	 * <pre>
	 * | alpha  gamma  uc |
	 * |     0   beta  vc |</pre>
	 */
	private final double[][] A;		// 2 x 3 2D affine transformation matrix
	private final DistortionModel distortion;

	/**
	 * Basic constructor.
	 * @param alpha
	 * @param beta
	 * @param gamma
	 * @param uc
	 * @param vc
	 * @param distortion
	 */
	public Camera(double alpha, double beta, double gamma, double uc, double vc, DistortionModel distortion) {
		this(makeAffineMatrix(alpha, beta, gamma, uc, vc), distortion);
	}

	/**
	 * Auxiliary constructor.
	 * @param a vector of 5 linear (affine) camera parameters
	 * @param distortion instance of lens distortion model
	 */
	public Camera(double[] a, DistortionModel distortion) {
		this(makeAffineMatrix(a[0], a[1], a[2], a[3], a[4]), distortion);
	}

	/**
	 * Creates a standard camera from a transformation matrix and a vector of lens distortion coefficients.
	 * @param A the (min.) 2 x 3 matrix holding the intrinsic camera parameters
	 * @param distortion a lens distortion model instance
	 */
	public Camera(RealMatrix A, DistortionModel distortion) {
		this.distortion = distortion; // ? new Radial2TermDistortionModel(0, 0) : new Radial2TermDistortionModel(K);
		this.A = A.getSubMatrix(0, 1, 0, 2).getData();
	}

// --------------------------------------------------------------------------

	/**
	 * Creates a new {@link Camera} instance from a parameter vector.
	 * Matches method {@link #getParameters()}, that is
	 * <pre>{@code
	 *     Camera cam2 = cam1.fromParameters(cam1.getParameters());
	 * }</pre>
	 * creates a new camera which is identical to the original.
	 * @param params all linear and non-linear camera parameters
	 * @return a new Camera instance with the specified parameters and the same type of lens distortion
	 * model as this instance
	 */
	public Camera fromParameters(double[] params) {
		if (params.length < this.getParameterCount())
			throw new IllegalArgumentException("wrong number of camera parameters: " + params.length);
		int P = this.distortion.getParameterCount();
		double[] linParams = Arrays.copyOfRange(params, 0, 5);    // = [alpha, beta, dgamma, uc, vc]
		double[] distParams = Arrays.copyOfRange(params, 5, 5 + P);
		return new Camera(linParams, this.distortion.fromParameters(distParams));
	}

	/**
	 * Creates an affine 2x3 transformation matrix from 5 intrinsic camera parameters.
	 * @param alpha
	 * @param beta
	 * @param gamma
	 * @param uc
	 * @param vc
	 * @return the 2D affine transformation matrix
	 */
	private static RealMatrix makeAffineMatrix(double alpha, double beta, double gamma, double uc, double vc) {
		return new Array2DRowRealMatrix(new double[][] {
				{alpha, gamma, uc},
				{    0,  beta, vc}}, false);
	}

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
	 * Projects the given 3D point to ideal projection coordinates for the provided extrinsic view parameters. The world
	 * point is specified as a 2D coordinate in the Z = 0 plane.
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
		// Compute normalized projection coordinates (f = 1):
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

	/**
	 * Returns the camera's inner (linear and distortion parameters as one vector
	 * (alpha, beta, gamma, uc, vc, distortion-params ...).
	 * @return the camera's inner parameters (linear and distortion parameters)
	 */
	public double[] getParameters() {
		double[] lin = new double[] {getAlpha(), getBeta(),	getGamma(), getUc(), getVc()};  // linear parameters
		double[] dist = distortion.getParameters();
		return Matrix.join(lin, dist);  // concatenate linear/nonlinear coefficients into one vector
	}

	/**
	 * Returns the {@link DistortionModel} instance attached to this camera.
	 * @return the camera's lens distortion model
	 */
	public DistortionModel getDistortion() {
		return this.distortion;
	}

	/**
	 * Returns the total number of linear and non-linear (distortion) camera parameters,
	 * which is 5 pluy the (variable) number of distortion parameters.
	 * @return the total number of parameters for this camera
	 */
	public int getParameterCount() {
		return 5 + distortion.getParameterCount();
	}

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

	/**
	 * Returns the homography for the given view as a 3 x 3 matrix.
	 * @param view the extrinsic view parameters
	 * @return the homography matrix
	 */
	public RealMatrix getHomography(ViewTransform view) {
		RealMatrix RT = view.getRotationMatrix();
		RealVector T = view.getTranslationVector();
		RT.setColumnVector(2, T);

		RealMatrix AM = MatrixUtils.createRealMatrix(3, 3);
		AM.setSubMatrix(A, 0, 0);
		AM.setEntry(2, 2, 1);

		RealMatrix H = AM.multiply(RT);
		return H.scalarMultiply(1.0 / H.getEntry(2, 2));
	}

	// -------------------------------------------------------------------

	@Override
	public String toString() {
		return String.format(Locale.US, "%s[alpha=%.2f, beta=%.2f, gamma=%.2f, uc=%.2f, vc=%.2f, %s]",
				this.getClass().getSimpleName(),
				getAlpha(), getBeta(), getGamma(), getUc(), getVc(), getDistortion());
	}

	//---------------------------------------------------------------------

//	public static void main(String[] args) {
//		ViewTransform view = new ViewTransform();
//
//		System.out.println("Camera 1:");
//		Camera camera1 = new Camera (
//				832.5, 832.53, 0.204494, 	// alpha, beta, gamma,
//				303.959, 206.585,			// c_x, c_y
//				-0.228601, 0.190353);		// k0, k1
//		System.out.format("k0=%.4f, k1=%.4f\n", camera1.getK()[0], camera1.getK()[1]);
//		double[] XYZ1 = {40, 70, 800};
//		double[] uv1 = camera1.project(view, XYZ1);
//		System.out.print(Matrix.toString(XYZ1) + " -> ");
//		System.out.format("u=%.4f, u=%.4f\n", uv1[0], uv1[1]);
//
//		double r = 0.95;
//		double rr = camera1.warp(r);
//		System.out.format("radial distortion: r=%.4f -> rr=%.4f\n", r, rr);
//		r = camera1.unwarp(rr);
//		System.out.format("inv. radial distortion: rr=%.4f -> r=%.4f\n", rr, r);
//
//		System.out.println();
//
//		// distorted camera
//		System.out.println("Camera 2:");
//		RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
//				{832.5, 0.204494, 303.959},
//				{  0.0, 832.53, 206.585},
//				{  0.0,   0.0,     1.0}});
//		Camera camera2 = new Camera(A, new double[] {-0.2, 0.190353});
//		System.out.format("k0=%.4f, k1=%.4f\n", camera2.getK()[0], camera2.getK()[1]);
//
//		double[] XYZ2 = {40, 70, 800};
//		double[] uv2 = camera2.project(view, XYZ2);
//		System.out.print(Matrix.toString(XYZ2) + " -> ");
//		System.out.format("u=%.4f, u=%.4f\n", uv2[0], uv2[1]);
//
//		r = 0.95;
//		rr = camera2.warp(r);
//		System.out.format("radial distortion: r=%.4f -> rr=%.4f\n", r, rr);
//		r = camera2.unwarp(rr);
//		System.out.format("inv. radial distortion: rr=%.4f -> r=%.4f\n", rr, r);
//
//		System.out.println("\nTesting radial lens distortion:");
//		double[] xy2 = {0.3, -0.7};
//		System.out.format("original x=%.4f, y=%.4f\n", xy2[0], xy2[1]);
//		double[] xy2d = camera2.warp(xy2);
//		System.out.format("distorted x=%.4f, y=%.4f\n", xy2d[0], xy2d[1]);
//		double[] xy2u = camera2.unwarp(xy2d);
//		System.out.format("undistorted x=%.4f, y=%.4f\n", xy2u[0], xy2u[1]);
//
//		System.out.println("\nTesting only radial lens distortion fun:");
//		double ra = 0.10;
//		double rb = camera2.warp(ra);
//		double rc = camera2.unwarp(rb);
//		System.out.format("ra=%.4f, rb=%.4f, rc=%.4f\n", ra, rb, rc);
//	}

}