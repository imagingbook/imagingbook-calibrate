package imagingbook.calibration.zhang;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * Abstract super-class for non-linear optimizers used for final, overall optimization of calibration parameters. The
 * actual optimization is performed by the sub-classes.
 *
 * @author WB
 */
public abstract class NonlinearOptimizer {

	private static int maxEvaluations = 1000;
	private static int maxIterations = 1000;

	protected final Point2D[] modelPts;
	protected final Point2D[][] obsPts;
	protected final int M;        // number of views
	protected final int N;        // number of model points
	protected int camParLength;        // number of camera parameters (7)
	protected int viewParLength;    // number of view parameters (6)

	private Camera initCam = null;
	private Camera finalCamera = null;
	private ViewTransform[] initViews = null;
	private ViewTransform[] finalViews = null;

	protected NonlinearOptimizer(Point2D[] modelPts, Point2D[][] obsPts) {
		this.modelPts = modelPts;
		this.obsPts = obsPts;
		this.M = obsPts.length;
		this.N = modelPts.length;
	}

	/**
	 * Performs Levenberg-Marquardt non-linear optimization to get better estimates of the parameters.
	 *
	 * @param initCam the initial camera parameters
	 * @param initViews the initial view transforms
	 */
	protected void optimize(Camera initCam, ViewTransform[] initViews) {
		this.initCam = initCam;
		this.initViews = initViews;
		this.camParLength = initCam.getParameterVector().length;
		this.viewParLength = initViews[0].getParameters().length;

		MultivariateVectorFunction V = makeValueFun();
		MultivariateMatrixFunction J = makeJacobianFun();

		RealVector start = makeInitialParameters();
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
	 *
	 * @return a vector value function
	 */
	abstract MultivariateVectorFunction makeValueFun();

	/**
	 * To be implemented by subclasses.
	 *
	 * @return a Jacobian function
	 */
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
			int c = 0;
			for (int m = 0; m < M; m++) {
				int q = camParLength + m * viewParLength;
				double[] w = Arrays.copyOfRange(params, q, q + viewParLength);
				ViewTransform view = new ViewTransform(w);
				for (int n = 0; n < N; n++) {
					double[] uv = cam.project(view, modelPts[n]);
					Y[c * 2 + 0] = uv[0];
					Y[c * 2 + 1] = uv[1];
					c = c + 1;
				}
			}
			return Y;
		}
	}

	// ---------------------------------------------------------------------

	private RealVector makeInitialParameters() {
		double[] s = initCam.getParameterVector();
		double[] c = new double[s.length + M * viewParLength];

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
	protected RealVector makeObservedVector() {
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
	 *
	 * @return the optimized camera parameters
	 */
	protected Camera getFinalCamera() {
		return finalCamera;
	}

	/**
	 * Returns the optimized view parameters.
	 *
	 * @return the optimized view parameters
	 */
	protected ViewTransform[] getFinalViews() {
		return finalViews;
	}

}
