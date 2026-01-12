/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.analysis.MultivariateMatrixFunction;
import org.apache.commons.math4.legacy.analysis.MultivariateVectorFunction;

import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract super-class for non-linear optimizers used for final, overall optimization of calibration parameters. The
 * actual optimization is performed by the sub-classes.
 *
 * @author WB
 */
public abstract class NonlinearOptimizer {

    protected static int maxEvaluations = 1000;
    protected static int maxIterations  = 1000;

	// @Deprecated
    // protected final Pnt2d[]  modelPts;
	protected final Pnt2d[][] modPts;
    protected final Pnt2d[][] obsPts;
    protected final int M;                // number of views
	// @Deprecated
    // protected final int N;                // number of model points
	protected final int pointCount;
    protected final int camParCount;      // number of camera parameters (7+)
    protected final int viewParCount;     // number of view parameters (6)

	protected final Camera initCam;
    protected Camera finalCamera;
    protected ViewTransform[] finalViews;

    /**
     * Super-constructor, invoked by constructors of inheriting classes.
     * @param initCam the initial camera parameters
     * @param modPntSet the 3D model points
     * @param obsPntSet the observed sensor points
     */
	NonlinearOptimizer(Camera initCam, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.initCam = initCam;
        this.camParCount = initCam.getParameterCount();
        this.viewParCount = ViewTransform.PARAMETER_COUNT;
		// this.modelPts = modPntSet.get(0);	// TODO: fix!
		this.modPts = modPntSet.toArray(new Pnt2d[0][]);
		this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
		this.M = obsPntSet.size();
		// this.N = modPts[0].length;	// TODO: not constant!!
		this.pointCount = getTotalPointCount();
	}

	private int getTotalPointCount() {
		int total = 0;
		for (Pnt2d[] p : modPts) {
			total += p.length;
		}
		return total;
	}

	/**
     * Performs Levenberg-Marquardt non-linear optimization to get better
     * estimates of the parameters.
     *
     * @param initViews the initial view transforms
     */
    public void optimize(ViewTransform[] initViews) {
		MultivariateVectorFunction V = makeValueFun();
		MultivariateMatrixFunction J = makeJacobianFun();

		RealVector start = makeInitialParameters(initViews);
		RealVector observed = makeObservedVector();

		MultivariateJacobianFunction model = LeastSquaresFactory.model(V, J);
		LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
		Optimum result = lm.optimize(LeastSquaresFactory.create(
				model,
				observed,
				start,
				null,
				maxEvaluations,
				maxIterations));

//		System.out.println(NonlinearOptimizer.class.getSimpleName() + "; iterations = " + result.getIterations());
		updateEstimates(result.getPoint());
	}

	/**
	 * To be implemented by subclasses.
	 * @return a vector value function
	 */
	abstract MultivariateVectorFunction makeValueFun();

	/**
	 * To be implemented by subclasses.
	 * @return a Jacobian function
	 */
	abstract MultivariateMatrixFunction makeJacobianFun();

	/**
	 * Common value function for optimizers defined in sub-classes,
     * implemented as a non-static class to access data of
     * enclosing class.
	 */
	class ValueFun implements MultivariateVectorFunction {

		@Override
		public double[] value(double[] params) {
			final double[] a = Arrays.copyOfRange(params, 0, camParCount);
			final Camera cam = initCam.copyOf(a);
			final double[] Y = new double[2 * pointCount];
			int r = 0;
			for (int k = 0; k < M; k++) {
				int q = camParCount + k * viewParCount;
				double[] w = Arrays.copyOfRange(params, q, q + viewParCount);
				ViewTransform view = new ViewTransform(w);
				for (int i = 0; i < modPts[k].length; i++) {
					double[] uv = cam.project(view, modPts[k][i]);
					Y[r * 2 + 0] = uv[0];
					Y[r * 2 + 1] = uv[1];
					r = r + 1;
				}
			}
			return Y;
		}
	}

	// ---------------------------------------------------------------------

	private RealVector makeInitialParameters(ViewTransform[] initViews) {
		double[] s = initCam.getParameterVector();
		double[] c = new double[s.length + M * viewParCount];

		// insert camera parameters at beginning of c
		System.arraycopy(s, 0, c, 0, s.length);

		// insert M view parameters
		int start = s.length;
		for (int i = 0; i < M; i++) {
			double[] w = initViews[i].getParameters();
			System.arraycopy(w, 0, c, start, w.length);
			start = start + w.length;
		}
		return new ArrayRealVector(c);
	}


	/**
	 * Stack the observed image coordinates of the calibration pattern points into a vector.
	 *
	 * @return the observed vector
	 */
	RealVector makeObservedVector() {
		double[] obs = new double[2 * pointCount];
		for (int k = 0, r = 0; k < M; k++) {
			for (int i = 0; i < obsPts[k].length; i++, r++) {
				obs[r * 2 + 0] = obsPts[k][i].getX();
				obs[r * 2 + 1] = obsPts[k][i].getY();
			}
		}
		// obs = [u_{0,0}, v_{0,0}, u_{0,1}, v_{0,1}, ..., u_{M-1,N-1}, v_{M-1,N-1}]
		return new ArrayRealVector(obs);
	}

	private void updateEstimates(RealVector parameters) {
		double[] c = parameters.toArray();
		double[] s = Arrays.copyOfRange(c, 0, camParCount);
		finalCamera = initCam.copyOf(s);

		finalViews = new ViewTransform[M];
		int start = s.length;
		for (int i = 0; i < M; i++) {
			double[] w = Arrays.copyOfRange(c, start, start + viewParCount);
			finalViews[i] = new ViewTransform(w);
			start = start + w.length;
		}
	}

	/**
	 * Returns the optimized camera parameters.
	 * @return the optimized camera parameters
	 */
    public Camera getFinalCamera() {
		return finalCamera;
	}

	/**
	 * Returns the optimized view parameters.
	 * @return the optimized view parameters
	 */
    public ViewTransform[] getFinalViews() {
		return finalViews;
	}

}
