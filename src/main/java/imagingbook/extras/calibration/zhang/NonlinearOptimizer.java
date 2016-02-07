package imagingbook.extras.calibration.zhang;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;


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
	 * Perform Levenberg-Marquardt non-linear optimization to get better
	 * estimates of the parameters
	 */
	protected Camera optimize(Camera cam, ViewTransform[] views) {
		this.initCam = cam;
		this.initViews = views;
		this.camParLength = cam.getParameterVector().length;
		this.viewParLength = views[0].getParameters().length;

		LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer();
		MultivariateVectorFunction value = makeValueFun();
		MultivariateMatrixFunction jacobian = makeJacobianFun();

		RealVector start = makeInitialParameters(initCam, initViews);
		RealVector observed = makeObservedVector();
		MultivariateJacobianFunction model = 
				LeastSquaresFactory.model(value, jacobian);
		//showJacobian(jacobian, start);

		Optimum result = lm.optimize(LeastSquaresFactory.create(
				model,
				observed, 
				start, 
				null, 
				maxEvaluations, 
				maxIterations));

		updateEstimates(result.getPoint());
		return finalCamera;
	}

	abstract MultivariateVectorFunction makeValueFun();
	abstract MultivariateMatrixFunction makeJacobianFun();

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
	 * Stack the observed image locations of the calibration pattern points into
	 * a vector
	 * 
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
	
	protected Camera getFinalCamera() {
		return finalCamera;
	}

	protected ViewTransform[] getFinalViews() {
		return finalViews;
	}
	
	@SuppressWarnings("unused")
	private void showJacobian(MultivariateMatrixFunction jacobianFun, RealVector point) {
		double[][] J = jacobianFun.value(point.toArray());
		FloatProcessor fp = new FloatProcessor(J[0].length, J.length);
		for (int i = 0; i < J.length; i++) {
			for (int j = 0; j < J[i].length; j++) {
				fp.setf(j, i, (float) J[i][j]); 
			}
		}
		(new ImagePlus("Jacobian", fp)).show();
	}

}
