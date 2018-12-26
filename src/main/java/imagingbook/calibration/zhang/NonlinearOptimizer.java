package imagingbook.calibration.zhang;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Abstract super-class for non-linear optimizers used for
 * final, overall optimization of calibration parameters.
 * The actual optimization is performed by the sub-classes.
 * @author WB
 */
public abstract class NonlinearOptimizer {

	static int maxEvaluations = 1000;
	static int maxIterations = 1000;
	static boolean beVerbose = false;

	final Point2D[] modelPts;
	final Point2D[][] obsPts;
	final int M;	// number of views
	final int N; 	// number of model points

	private Camera initCam, finalCamera = null;
	private ViewTransform[] initViews, finalViews = null;
	int camParLength = 7;
	int viewParLength = 6;

	protected NonlinearOptimizer(Point2D[] modelPts, Point2D[][] obsPts) {
		this.modelPts = modelPts;
		this.obsPts = obsPts;
		this.M = obsPts.length;
		this.N = modelPts.length;
	}

	/**
	 * Performs Levenberg-Marquardt non-linear optimization to get better
	 * estimates of the parameters.
	 * @param cam the initial camera parameters
	 * @param views the initial view transforms
	 */
	protected void optimize(Camera cam, ViewTransform[] views) {
		this.initCam = cam;
		this.initViews = views;
		this.camParLength = cam.getParameterVector().length;
		this.viewParLength = views[0].getParameters().length;

		MultivariateVectorFunction V = makeValueFun();
		MultivariateMatrixFunction J = makeJacobianFun();

		RealVector start = makeInitialParameters(initCam, initViews);
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

		updateEstimates(result.getPoint());
	}

	abstract MultivariateVectorFunction makeValueFun();
	abstract MultivariateMatrixFunction makeJacobianFun();
	
	
	/**
	 * Common value function for optimizers defined in sub-classes.
	 */
	protected class ValueFun implements MultivariateVectorFunction {
		@Override
		public double[] value(double[] params) {
			final double[] a = Arrays.copyOfRange(params, 0, camParLength);
			final Camera cam = new Camera(a);
			final double[] Y = new double[2 * M * N];
			int l = 0; 
			for (int i = 0; i < M; i++) {
				int m = camParLength + i * viewParLength;
				double[] w = Arrays.copyOfRange(params, m, m + viewParLength);
				ViewTransform view = new ViewTransform(w);
				for (int j = 0; j < N; j++) {
					double[] uv = cam.project(view, modelPts[j]);
					Y[l * 2 + 0] = uv[0];
					Y[l * 2 + 1] = uv[1];
					l = l + 1;
				}
			}
			return Y;
		}
	}	// end of inner class 'ValueFun'

	// ---------------------------------------------------------------------

	private RealVector makeInitialParameters(Camera camera, ViewTransform[] views) {
		double[] s = camera.getParameterVector();
		double[] c = new double[s.length + M * viewParLength];

		// insert camera parameters at beginning of c
		System.arraycopy(s, 0, c, 0, s.length);

		// insert M view parameters
		int start = s.length;
		for (int i = 0; i < M; i++) {
			double[] w = views[i].getParameters();
			System.arraycopy(w, 0, c, start, w.length);
			start = start + w.length;
		}
		return new ArrayRealVector(c);
	}


	/**
	 * Stack the observed image coordinates of the calibration pattern points into
	 * a vector.
	 * @return the observed vector
	 */
	protected RealVector makeObservedVector()	{
		double[] obs = new double[M * N * 2];
		for (int i = 0, k = 0; i < M; i++) {
			for (int j = 0; j < N; j++, k++) {
				obs[k * 2 + 0] = obsPts[i][j].getX();
				obs[k * 2 + 1] = obsPts[i][j].getY();
			}
		}
		// obs = [u_{0,0}, v_{0,0}, u_{0,1}, v_{0,1}, ..., u_{M-1,N-1}, v_{M-1,N-1}]
		return new ArrayRealVector(obs); 
	}

	private void updateEstimates(RealVector parameters) {
		double[] c = parameters.toArray();
		double[] s = Arrays.copyOfRange(c, 0, camParLength);
		finalCamera = new Camera(s);

		finalViews = new ViewTransform[M];
		int start = s.length;
		for (int i = 0; i < M; i++) {
			double[] w = Arrays.copyOfRange(c, start, start + viewParLength);
			finalViews[i] = new ViewTransform(w);
			start = start + w.length;
		}
	}
	
	/**
	 * Returns the optimized camera parameters.
	 * @return the optimized camera parameters
	 */
	protected Camera getFinalCamera() {
		return finalCamera;
	}

	/**
	 * Returns the optimized view parameters.
	 * @return the optimized view parameters
	 */
	protected ViewTransform[] getFinalViews() {
		return finalViews;
	}
	
}
