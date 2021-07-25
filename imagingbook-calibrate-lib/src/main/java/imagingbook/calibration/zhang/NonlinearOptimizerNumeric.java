package imagingbook.calibration.zhang;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

/**
 * Nonlinear optimizer based on the Levenberg-Marquart method, where the Jacobian matrix
 * is calculated numerically (i.e., by estimating the first partial derivatives from
 * finite differences). The advantage is that the calculation of the Jacobian is 
 * independent of the calibration model, while performance and runtime are similar to
 * the analytic version (see {@link NonlinearOptimizerAnalytic}).
 * 
 * @author WB
 */
public class NonlinearOptimizerNumeric extends NonlinearOptimizer {
	
	protected NonlinearOptimizerNumeric(Point2D[] modelPts, Point2D[][] obsPts) {
		super(modelPts, obsPts);
	}
	
	@Override
	MultivariateVectorFunction makeValueFun() {
		return new ValueFun();
	}

	@Override
	MultivariateMatrixFunction makeJacobianFun() {
		return new JacobianFun();
	}

	private class JacobianFun implements MultivariateMatrixFunction {
		
		/**
		 * Calculates a "stacked" Jacobian matrix with 2MN rows and K = 7 + 6M
		 * columns (for M views with N points each, K parameters). For example, 
		 * with M = 5 views and N = 256 points each, J is of size 2560 × 37.
		 * Each pair of rows in the Jacobian corresponds to one point.
		 * THIS VERSION only calculates single blocks of the Jacobian!
		 */
		@Override
	    public double[][] value(double[] params) {
			final int K = params.length;
	        double[][] J = new double[2 * M * N][K];	// the Jacobian matrix (initialized to zeroes!)
	        double[] refValues = new double[2 * M * N];	// values obtained with undisturbed parameters 
	        
	        double[] a = Arrays.copyOfRange(params, 0, camParLength);	// camera parameters
	        Camera camOrig = new Camera(a);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	       
	        for (int r = 0, i = 0; i < M; i++) {	// for all views, r = row
	        	int m = camParLength + viewParLength * i;
				double[] w = Arrays.copyOfRange(params, m, m + viewParLength);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < N; j++) {	// for all model points: calculate reference values
	        		double[] uv = camOrig.project(view, modelPts[j]);
	        		refValues[r + 0] = uv[0];
	        		refValues[r + 1] = uv[1];
	        		r = r + 2;
	        	}        	 
	        }
	        
	        // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
	        
	        for (int k = 0; k < a.length; k++) {	// for all camera parameters
	        	double ak = a[k];					// keep original parameter value       	
	        	double delta = estimateDelta(ak);
	        	a[k] = a[k] + delta;		// modify parameter s_k
	        	Camera camMod = new Camera(a);	// modified camera
	        	
		        for (int r = 0, i = 0; i < M; i++) {	// for all views, r = row
		        	int m = camParLength + i * viewParLength;
		        	double[] w = Arrays.copyOfRange(params, m, m + viewParLength);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int j = 0; j < N; j++) {	// for all model points: calculate disturbed value
		        		Point2D Pj = modelPts[j];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[r + 0][k] = (uvMod[0] - refValues[r + 0]) / delta;   // dX
		        		J[r + 1][k] = (uvMod[1] - refValues[r + 1]) / delta;   // dY
		        		r = r + 2;
		        	}    
		        }
		        a[k] = ak; 	// return parameter s_k to original
	        }
	        
	        // Step 2: calculate the diagonal blocks, one for each view
	        
	        for (int i = 0; i < M; i++) {	// for all views/blocks
	        	final int start = camParLength + i * viewParLength;
	        	double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
	        	final int c = a.length + i * w.length;		// leftmost matrix column of block i
	        	for (int k = 0; k < w.length; k++) {	// for all parameters in w
	        		double wk = w[k];					// keep original parameter w_k
	        		double delta = estimateDelta(wk);
	        		w[k] =  w[k] + delta;				// modify parameter w_k
	        		ViewTransform view = new ViewTransform(w);
	        		int r = 2 * i * N;	// row
	        		for (int j = 0; j < N; j++) {		// for all model points: calculate disturbed value
	        			Point2D Pj = modelPts[j];
	        			double[] uvMod = camOrig.project(view, Pj);
	        			J[r + 0][c + k] = (uvMod[0] - refValues[r + 0]) / delta;   // dX
	        			J[r + 1][c + k] = (uvMod[1] - refValues[r + 1]) / delta;   // dY
	        			r = r + 2;
	        		} 
	        		w[k] = wk; // w[k] - DELTA;		// return parameter w_k to original
	        	}
	        }
	        
//			long endtime = System.nanoTime();
//			System.out.println("time diff = " + (endtime - starttime) + " ns");
//	        System.out.println(NonlinearOptimizerNumeric.class.getSimpleName() + 
//	        		": Jacobian inverse condition number = " + MathUtil.inverseConditionNumber(J));
	        return J;
	    }
		
		 // THIS VERSION calculates all entries of the Jacobian (NOT USED)!
	    
	    @Deprecated
	    @SuppressWarnings("unused")
		public double[][] value(double[] params, boolean dummy) {
	    	//long starttime = System.nanoTime();
	    	//System.out.println("getJacobianMatrix - NUMERICAL");
	    	// M = number of views, N = number of model points
	        double[][] J = new double[2 * M * N][params.length];	// the Jacobian matrix
	        double[] refValues = new double[2 * M * N];	// function values obtained with undisturbed parameters 
	        
	        double[] s = Arrays.copyOfRange(params, 0, camParLength);
	        Camera cam = new Camera(s);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	        
	        for (int row = 0, m = 0; m < M; m++) {	// for all views
	        	int start = camParLength + m * viewParLength;
				double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < N; j++) {	// for all model points: calculate reference values
	        		double[] uv = cam.project(view, modelPts[j]);
	        		refValues[row + 0] = uv[0];
	        		refValues[row + 1] = uv[1];
	        		row = row + 2;
	        	}        	 
	        }
	        
	        // Step 1: calculate all entries of the Jacobian (brute force!)
	        
	        for (int k = 0; k < params.length; k++) {	// for ALL parameters
	        	int col = k;
	        	// calculate step width and modify the parameter vector c:
	        	double pk = params[k];
	        	double delta = estimateDelta(pk);
	        	params[k] = params[k] + delta;		// modify parameter c_k
	        	
	        	double[] smod = Arrays.copyOfRange(params, 0, camParLength);
	        	Camera camMod = new Camera(smod);	// modified camera
	        	
		        for (int row = 0, m = 0; m < M; m++) {	// for all views
		        	int start = camParLength + m * viewParLength;
		        	double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int n = 0; n < N; n++) {	// for all model points: calculate disturbed value
		        		Point2D Pj = modelPts[n];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[row + 0][col] = (uvMod[0] - refValues[row + 0]) / delta;   // du
		        		J[row + 1][col] = (uvMod[1] - refValues[row + 1]) / delta;   // dv	
		        		row = row + 2;
		        	}    
		        }
		        params[k] = pk;
	        }
	       
	        long endtime = System.nanoTime();
//			System.out.println("time diff = " + (endtime - starttime) + " ns");
//			System.out.println(this.getClass().getSimpleName() + 
//	        		": Jacobian inverse condition number = " + MathUtil.inverseConditionNumber(J));
	        return J;
	    }

	}
	
	/**
	 * Returns a positive delta value adapted to the magnitude of the parameter x
	 * @param x
	 * @return
	 */
    private double estimateDelta(double x) {
    	final double eps = 1.5e-8;	// = sqrt(2.2 * 10^{-16})
    	double dx = eps * Math.max(Math.abs(x), 1); // dx >= eps
    	// avoid numerical truncation problems (add and subtract again) - 
    	// not sure if this survives the compiler !?
    	double tmp = x + dx;
    	return tmp - x;
    }
	
}

/*

Benchmarks for calculating the Jacobian:

Numeric/Block
time diff = 3767805 ns
time diff = 2845680 ns
time diff = 3000871 ns
time diff = 2911613 ns
time diff = 3284195 ns
time diff = 1581763 ns
time diff = 1582074 ns

Numeric/Full
time diff = 9383902 ns
time diff = 5680786 ns
time diff = 3523978 ns
time diff = 5784972 ns
time diff = 3406730 ns
time diff = 3463022 ns
time diff = 3566275 ns

Analytic
time diff = 7060397 ns
time diff = 1106239 ns
time diff = 1095976 ns
time diff = 1035330 ns
time diff = 1041550 ns
time diff = 1043416 ns
time diff = 1073895 ns




*/