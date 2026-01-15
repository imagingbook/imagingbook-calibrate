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

import java.util.Arrays;
import java.util.List;

/**
 * Nonlinear optimizer based on the Levenberg-Marquart method, where the Jacobian matrix is calculated numerically
 * (i.e., by estimating the first partial derivatives from finite differences). The advantage is that the calculation of
 * the Jacobian is independent of the calibration model, while performance and runtime are similar to the analytic
 * version (see {@link NonlinearOptimizerAnalytic}).
 *
 * @author WB
 */
public class NonlinearOptimizerNumeric extends NonlinearOptimizer {
	
	public NonlinearOptimizerNumeric(Camera initCam, List<ViewTransform> viewList, List<Pnt2d[]> modelPntSet, List<Pnt2d[]> obsPntSet) {
		super(initCam, viewList, modelPntSet, obsPntSet);
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
		 * Calculates a "stacked" Jacobian matrix with 2MN rows and K = 7 + 6M columns (for M views with N points each,
		 * K parameters). For example, with M = 5 views and N = 256 points each, J is of size 2560 × 37. Each pair of
		 * rows in the Jacobian corresponds to one point. THIS VERSION only calculates single blocks of the Jacobian!
		 */
		@Override
	    public double[][] value(double[] params) {
			final int K = params.length;
	        double[][] J = new double[2 * N][K];	// the Jacobian matrix (initialized to zeroes!)
	        double[] refValues = new double[2 * N];	// values obtained with undisturbed parameters
	        
	        double[] a = Arrays.copyOfRange(params, 0, camParCount);	// camera parameters
	        Camera camOrig = initCam.fromParameters(a);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	       
	        for (int k = 0, r = 0; k < M; k++) {	// for all views, r = row
	        	int m = camParCount + viewParCount * k;
				double[] w = Arrays.copyOfRange(params, m, m + viewParCount);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < modPts[k].length; j++, r+=2) {	// for all model points: calculate reference values
	        		double[] uv = camOrig.project(view, modPts[k][j]);
	        		refValues[r + 0] = uv[0];
	        		refValues[r + 1] = uv[1];
	        	}        	 
	        }
	        
	        // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
	        
	        for (int p = 0; p < a.length; p++) {	// for all camera parameters
	        	double ak = a[p];					// keep original parameter value
	        	double delta = estimateDelta(ak);
	        	a[p] = a[p] + delta;		// modify parameter s_k
	        	Camera camMod = camOrig.fromParameters(a);	// modified camera
	        	
		        for (int k = 0, r = 0; k < M; k++) {	// for all views k, r = row
		        	int m = camParCount + k * viewParCount;
		        	double[] w = Arrays.copyOfRange(params, m, m + viewParCount);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int j = 0; j < modPts[k].length; j++, r+=2) {	// for all model points: calculate disturbed value
		        		Pnt2d Pj = modPts[k][j];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[r + 0][p] = (uvMod[0] - refValues[r + 0]) / delta;   // dX
		        		J[r + 1][p] = (uvMod[1] - refValues[r + 1]) / delta;   // dY
		        	}    
		        }
		        a[p] = ak; 	// return parameter s_k to original
	        }
	        
	        // Step 2: calculate the diagonal blocks, one for each view
	        
	        for (int k = 0; k < M; k++) {	// for all views/blocks
	        	final int start = camParCount + k * viewParCount;
	        	double[] w = Arrays.copyOfRange(params, start, start + viewParCount);
	        	final int c = a.length + k * w.length;		// leftmost matrix column of block i
	        	for (int p = 0; p < w.length; p++) {	// for all parameters in w
	        		double wp = w[p];					// keep original parameter w_k
	        		double delta = estimateDelta(wp);
	        		w[p] =  w[p] + delta;				// modify parameter w_k
	        		ViewTransform view = new ViewTransform(w);
	        		int r = 2 * k * modPts[k].length;	// row
	        		for (int j = 0; j < modPts[k].length; j++) {		// for all model points: calculate disturbed value
	        			Pnt2d Pj = modPts[k][j];
	        			double[] uvMod = camOrig.project(view, Pj);
	        			J[r + 0][c + p] = (uvMod[0] - refValues[r + 0]) / delta;   // dX
	        			J[r + 1][c + p] = (uvMod[1] - refValues[r + 1]) / delta;   // dY
	        			r = r + 2;
	        		} 
	        		w[p] = wp; // w[k] - DELTA;		// return parameter w_k to original
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
	        double[][] J = new double[2 * N][params.length];	// the Jacobian matrix
	        double[] refValues = new double[2 * N];	// function values obtained with undisturbed parameters
	        
	        double[] s = Arrays.copyOfRange(params, 0, camParCount);
	        Camera cam = initCam.fromParameters(s);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	        
	        for (int k = 0, r = 0; k < M; k++) {	// for all views k
	        	int start = camParCount + k * viewParCount;
				double[] w = Arrays.copyOfRange(params, start, start + viewParCount);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < modPts[k].length; j++, r+=2) {	// for all model points: calculate reference values
	        		double[] uv = cam.project(view, modPts[k][j]);
	        		refValues[r + 0] = uv[0];
	        		refValues[r + 1] = uv[1];
	        	}        	 
	        }
	        
	        // Step 1: calculate all entries of the Jacobian (brute force!)
	        
	        for (int p = 0; p < params.length; p++) {	// for ALL parameters
	        	int col = p;
	        	// calculate step width and modify the parameter vector c:
	        	double wp = params[p];
	        	double delta = estimateDelta(wp);
	        	params[p] = params[p] + delta;		// modify parameter c_k
	        	
	        	double[] smod = Arrays.copyOfRange(params, 0, camParCount);
	        	Camera camMod = cam.fromParameters(smod);	// modified camera
	        	
		        for (int k = 0, r = 0; k < M; k++) {	// for all views k
		        	int start = camParCount + k * viewParCount;
		        	double[] w = Arrays.copyOfRange(params, start, start + viewParCount);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int j = 0; j < modPts[k].length; j++) {	// for all model points: calculate disturbed value
		        		Pnt2d Pj = modPts[k][j];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[r + 0][col] = (uvMod[0] - refValues[r + 0]) / delta;   // du
		        		J[r + 1][col] = (uvMod[1] - refValues[r + 1]) / delta;   // dv
		        		r = r + 2;
		        	}
		        }
		        params[p] = wp;
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
	 *
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

/* Benchmarks for calculating the Jacobian:

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