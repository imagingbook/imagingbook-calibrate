/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Pnt2d.PntDouble;
import imagingbook.common.geometry.mappings.Mapping2D;
import org.apache.commons.math3.linear.RealMatrix;


/**
 * This class represents the 2D geometric transformation for an image taken with some camera A to an image taken with
 * another camera B.
 *
 * @author W. Burger
 * @version 2016-06-01
 */
public class InterCameraMapping implements Mapping2D {

	private final Camera camA, camB;
	private final RealMatrix Abi;    // inverse of the intrinsic camera b matrix (2 x 3)

	public InterCameraMapping(Camera camA, Camera camB) {
//		this.isInverseFlag = true;	// maps target -> source
		this.camA = camA;        // camera A (used to produce the source image)
		this.camB = camB;        // camera B (determines the geometry the target image)
		this.Abi = camB.getInverseA();
	}

	@Override
	public Pnt2d applyTo(Pnt2d uv) {
		// (u,v) is an observed sensor point
		// apply the inverse camera mapping to get the distorted (x,y) point:
		double[] xy = Abi.operate(MathUtil.toHomogeneous(uv.toDoubleArray()));

		// remove the lens distortion of camera b:
		double[] xyu = camB.unwarp(xy);

		// apply the lens distortion of camera a:
		double[] xyd = camA.warp(xyu);

		// apply the (forward) camera mapping to get the undistorted sensor point (u',v'):
		return PntDouble.from(camA.mapToSensorPlane(xyd));
	}

}
