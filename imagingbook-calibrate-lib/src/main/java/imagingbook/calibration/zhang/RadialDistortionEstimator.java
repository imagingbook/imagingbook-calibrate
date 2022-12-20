/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2022 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * This class defines methods for estimating the radial lens distortion parameters
 *
 * @author WB
 */
public class RadialDistortionEstimator {

	/**
	 * Estimates the lens distortion from multiple views, starting from an initial (linear) camera model.
	 *
	 * @param cam the initial (linear) camera model
	 * @param views a sequence of extrinsic view transformations
	 * @param modelPts the set of 2D model points (on the planar calibration target)
	 * @param obsPts a sequence of 2D image point sets, one set for each view
	 * @return a vector of lens distortion coefficients
	 */
	protected double[] estimateLensDistortion(Camera cam, ViewTransform[] views, Pnt2d[] modelPts, Pnt2d[][] obsPts) {
		final int M = views.length;		// the number of views
		final int N = modelPts.length;	// the number of model points

		final double uc = cam.getUc();
		final double vc = cam.getVc();

		RealMatrix D = MatrixUtils.createRealMatrix(2 * M * N, 2);
		RealVector d = new ArrayRealVector(2 * M * N);

		int l = 0;
		for (int i = 0; i < M; i++) {
			Pnt2d[] obs = obsPts[i];

			for (int j = 0; j < N; j++) {
				// determine the radius in the ideal image plane
				double[] xy = cam.projectNormalized(views[i], modelPts[j]);
				double x = xy[0];
				double y = xy[1];
				double r2 = x * x + y * y;
				double r4 = r2 * r2;
				
				// project model point to image
				double[] uv = cam.project(views[i], modelPts[j]);
				double u = uv[0];
				double v = uv[1];
				double du = u - uc;	// distance to estim. projection center
				double dv = v - vc;
				
				D.setEntry(l * 2 + 0, 0, du * r2);
				D.setEntry(l * 2 + 0, 1, du * r4);
				D.setEntry(l * 2 + 1, 0, dv * r2);
				D.setEntry(l * 2 + 1, 1, dv * r4);
				
				// observed image point
				Pnt2d UV = obs[j];
				double U = UV.getX();
				double V = UV.getY();
				
				d.setEntry(l * 2 + 0, U - u);
				d.setEntry(l * 2 + 1, V - v);
				l++;
			}
		}
		
		DecompositionSolver solver = new SingularValueDecomposition(D).getSolver();
		RealVector k = solver.solve(d);
		
//		double err1 = D.operate(new ArrayRealVector(new double[] {0,0})).subtract(d).getNorm();
//		double err2 = D.operate(k).subtract(d).getNorm();
//		System.out.format("err1=%.2f, err2=%.2f \n", err1, err2);
		
		return k.toArray();
	}

}
