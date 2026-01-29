/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import imagingbook.calibrate.distortion.DistortionModel;
import org.apache.commons.math4.legacy.linear.RealMatrix;


/**
 * Represents the internals of a camera, consisting of a linear (affine) transformation matrix and
 * a non-linear lens distortion model.
 * Instances of {@link StandardCamera} are considered immutable. Various constructors are provided for
 * copying instances with modified parameters.
 */
public class StandardCamera extends Camera {

	/**
	 * Constructor. Both arguments may be {@code null}, in which case a dummy camera instance is
	 * created for later duplication.
	 * @param A vector of 5 linear (affine) camera parameters: alpha, beta, gamma, uc, vc (may be {@code null})
	 * @param distortion instance of {@link DistortionModel} (may be {@code null})
	 */
	public StandardCamera(double[] A, DistortionModel distortion) {
		super(A, distortion);
		checkLength(A, 5);
	}

	// -------------------------------------------------------------------

	/**
	 * Creates a new {@link StandardCamera} instance from an existing instance using the supplied parameters.
	 * @param A a 2x3 affine transformation matrix
	 * @return a new StandardCamera instance with the specified parameters and the same type of lens distortion
	 * 	 * model as this instance
	 */
	@Deprecated
	public StandardCamera withParameters(RealMatrix A) {
		double alpha = A.getEntry(0, 0);
		double beta = A.getEntry(1, 1);
		double gamma = A.getEntry(0, 1);
		double uc = A.getEntry(0, 2);
		double vc = A.getEntry(1, 2);
		return new StandardCamera(new double[] {alpha, beta, gamma, uc, vc}, this.distortion);
	}

	@Override
	public int getParameterIdxAlpha() { return 0;}

	@Override
	public int getParameterIdxBeta() { return 1; }

	@Override
	public int getParameterIdxGamma() { return 2; }

	@Override
	public int getParameterIdxUc() { return 3; }

	@Override
	public int getParameterIdxVc() { return 4; }

	// -------------------------------------------------------------------

	/**
	 * Returns the number of linear coefficients in the vector returned by
	 * {@link #getLinearParameters()}.
	 * @return the number of linear parameters
	 */
	public int getLinParameterCount() {
		return 5;
	}

	/**
	 * Returns the camera's linear parameters as a 5-vector
	 * (alpha, beta, gamma, uc, vc).
	 * @return the camera's linear parameters
	 */
	public double[] getLinearParameters() {
		return new double[] { A[0][0], A[1][1], A[0][1], A[0][2], A[1][2] };
	}

	// -------------------------------------------------------------------

	public static StandardCamera fromHomographies(RealMatrix[] homographies, int imgWidth, int imgHeight) {
		IntrinsicsEstimator estimator = new IntrinsicsEstimatorConstrained(imgWidth, imgHeight);
		RealMatrix A = estimator.estimateIntrinsics(homographies);
		double alpha = A.getEntry(0, 0);
		double beta = A.getEntry(1, 1);
		double gamma = A.getEntry(0, 1);
		double uc = A.getEntry(0, 2);
		double vc = A.getEntry(1, 2);
		double[] params = new double[] {alpha, beta, gamma, uc, vc};
		return new StandardCamera(params, null);
	}

}