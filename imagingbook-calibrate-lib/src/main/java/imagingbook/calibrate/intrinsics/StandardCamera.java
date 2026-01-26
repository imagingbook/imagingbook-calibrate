/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import imagingbook.calibrate.distortion.DistortionModel;



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
	 * @param a vector of 5 linear (affine) camera parameters: alpha, beta, gamma, uc, vc (may be {@code null})
	 * @param distortion instance of {@link DistortionModel} (may be {@code null})
	 */
	public StandardCamera(double[] a, DistortionModel distortion) {
		super(a, distortion);
		checkLength(a, 5);
	}

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

}