package imagingbook.extras.calibration.zhang;

import java.awt.geom.Point2D;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

/**
 * Nonlinear optimizer based on the Levenberg-Marquart method, where the Jacobian matrix
 * is calculated numerically (i.e., by estimating the first partial derivatives from
 * finite differences).
 * @author WB
 *
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
	

	/**
	 * This is the implementation of the value function for the optimiser. It
	 * computes the predicted location of an image point by projecting a model
	 * point through the camera homography and then applying the distortion. The
	 * implementation is converted from the C code produced by the following
	 * matlab symbolic code:
	 * 
	 * <pre>
	 * <code>
	 * syms u0 v0 fx fy sk real
	 * syms tx ty tz wx wy wz real
	 * syms k1 k2 real
	 * syms X Y real
	 * 
	 * % the intrinsic parameter matrix
	 * K=[fx sk u0; 0 fy v0; 0 0 1];
	 * 
	 * % Expression for the rotation matrix based on the Rodrigues formula
	 * theta=sqrt(wx^2+wy^2+wz^2);
	 * omega=[0 -wz wy; wz 0 -wx; -wy wx 0];
	 * R = eye(3) + (sin(theta)/theta)*omega + ((1-cos(theta))/theta^2)*(omega*omega);
	 * 
	 * % Expression for the translation vector
	 * t=[tx;ty;tz];
	 * 
	 * % perspective projection of the model point (X,Y)
	 * uvs=K*[R(:,1) R(:,2) t]*[X; Y; 1];
	 * u=uvs(1)/uvs(3);
	 * v=uvs(2)/uvs(3);
	 * 
	 * % application of 2-term radial distortion
	 * uu0 = u - u0;
	 * vv0 = v - v0;
	 * x =  uu0/fx;
	 * y =  vv0/fy;
	 * r2 = x*x + y*y;
	 * r4 = r2*r2;
	 * uv = [u + uu0*(k1*r2 + k2*r4); v + vv0*(k1*r2 + k2*r4)];
	 * ccode(uv, 'file', 'zhang-value.c')
	 * </code>
	 * </pre>
	 * 
	 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
	 * 
	 */
	
//	private class ValueFun implements MultivariateVectorFunction {
//		@Override
//		public double[] value(double[] params) {
//			final double[] a = Arrays.copyOfRange(params, 0, camParLength);
//			final Camera cam = new Camera(a);
//			final double[] Y = new double[2 * M * N];
//			int l = 0; 
//			for (int i = 0; i < M; i++) {
//				int m = camParLength + i * viewParLength;
//				double[] w = Arrays.copyOfRange(params, m, m + viewParLength);
//				ViewTransform view = new ViewTransform(w);
//				for (int j = 0; j < N; j++) {
//					double[] uv = cam.project(view, modelPts[j]);
//					Y[l * 2 + 0] = uv[0];
//					Y[l * 2 + 1] = uv[1];
//					l = l + 1;
//				}
//			}
//			return Y;
//		}
//	}	// end of inner class 'ValueFun'

	
	
	/**
	 * This is the implementation of the Jacobian function for the optimiser; it
	 * is the partial derivative of the value function with respect to the
	 * parameters. The implementation is based on the matlab symbolic code:
	 * 
	 * <pre>
	 * <code>
	 * syms u0 v0 fx fy sk real
	 * syms tx ty tz wx wy wz real
	 * syms k1 k2 real
	 * syms X Y real
	 * 
	 * % the intrinsic parameter matrix
	 * K=[fx sk u0; 0 fy v0; 0 0 1];
	 * 
	 * % Expression for the rotation matrix based on the Rodrigues formula
	 * theta=sqrt(wx^2+wy^2+wz^2);
	 * omega=[0 -wz wy; wz 0 -wx; -wy wx 0];
	 * R = eye(3) + (sin(theta)/theta)*omega + ((1-cos(theta))/theta^2)*(omega*omega);
	 * 
	 * % Expression for the translation vector
	 * t=[tx;ty;tz];
	 * 
	 * % perspective projection of the model point (X,Y)
	 * uvs=K*[R(:,1) R(:,2) t]*[X; Y; 1];
	 * u=uvs(1)/uvs(3);
	 * v=uvs(2)/uvs(3);
	 * 
	 * % application of 2-term radial distortion
	 * uu0 = u - u0;
	 * vv0 = v - v0;
	 * x =  uu0/fx;
	 * y =  vv0/fy;
	 * r2 = x*x + y*y;
	 * r4 = r2*r2;
	 * uv = [u + uu0*(k1*r2 + k2*r4); v + vv0*(k1*r2 + k2*r4)];
	 * J=jacobian(uv,[fx,fy,u0,v0,sk,k1,k2, wx wy wz tx ty tz]); 
	 * ccode(J, 'file', 'zhang-jacobian.c')
	 * </code>
	 * </pre>
	 * 
	 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
	 * 
	 */
	private class JacobianFun implements MultivariateMatrixFunction {
		// THIS VERSION only calculates single blocks of the Jacobian!
	    public double[][] value(double[] params) {
//			long starttime = System.nanoTime();
			
	    	//System.out.println("JacobianFun " + Matrix.toString(params));
	    	// M = number of views, N = number of target points
	        double[][] J = new double[2 * M * N][params.length];	// the Jacobian matrix (initialized to zeroes!)
	        double[] refValues = new double[2 * M * N];	// values obtained with undisturbed parameters 
	        
	        double[] s = Arrays.copyOfRange(params, 0, camParLength);
	        Camera camOrig = new Camera(s);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	        
	        for (int row = 0, i = 0; i < M; i++) {	// for all views
	        	int start = camParLength + i * viewParLength;
				double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < N; j++) {	// for all model points: calculate reference values
	        		double[] uv = camOrig.project(view, modelPts[j]);
	        		refValues[row + 0] = uv[0];
	        		refValues[row + 1] = uv[1];
	        		row = row + 2;
	        	}        	 
	        }
	        
	        // Step 1: calculate the leftmost (green) block of J associated with camera intrinsics
	        
	        for (int k = 0; k < s.length; k++) {	// for all parameters in s
	        	int col = k;
	        	double sk = s[k];			// keep original parameter value       	
	        	double delta = estimateDelta(sk);
	        	s[k] = s[k] + delta;		// modify parameter s_k
	        	Camera camMod = new Camera(s);	// modified camera
	        	
		        for (int row = 0, i = 0; i < M; i++) {	// for all views
		        	int start = camParLength + i * viewParLength;
		        	double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int j = 0; j < N; j++) {	// for all model points: calculate disturbed value
		        		Point2D Pj = modelPts[j];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[row + 0][col] = (uvMod[0] - refValues[row + 0]) / delta;   // dX
		        		J[row + 1][col] = (uvMod[1] - refValues[row + 1]) / delta;   // dY
		        		row = row + 2;
		        	}    
		        }
		        s[k] = sk; 	// return parameter s_k to original
	        }
	        
	        // Step 2: calculate the diagonal blocks, one for each view
	        
	        for (int i = 0; i < M; i++) {	// for all views/blocks
	        	int start = camParLength + i * viewParLength;
	        	double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
	        	int col = s.length + i * w.length;		// leftmost matrix column of block i
	        	for (int k = 0; k < w.length; k++) {	// for all parameters in w
	        		double wk = w[k];					// keep original parameter w_k
	        		double delta = estimateDelta(wk);
	        		w[k] =  w[k] + delta;				// modify parameter w_k
	        		ViewTransform view = new ViewTransform(w);
	        		int row = 2 * i * N;
	        		for (int j = 0; j < N; j++) {		// for all model points: calculate disturbed value
	        			Point2D Pj = modelPts[j];
	        			double[] uvMod = camOrig.project(view, Pj);
	        			J[row + 0][col + k] = (uvMod[0] - refValues[row + 0]) / delta;   // dX
	        			J[row + 1][col + k] = (uvMod[1] - refValues[row + 1]) / delta;   // dY
	        			row = row + 2;
	        		} 
	        		w[k] = wk; // w[k] - DELTA;		// return parameter w_k to original
	        		//col++;
	        	}
	        }
	        
//			long endtime = System.nanoTime();
//			System.out.println("time diff = " + (endtime - starttime) + " ns");
	        return J;
	    }
		
		 // THIS VERSION calculates all entries of the Jacobian (not used)!
	    @SuppressWarnings("unused")
	    @Deprecated
		public double[][] value(double[] params, boolean dummy) {
	    	long starttime = System.nanoTime();
	    	//System.out.println("getJacobianMatrix - NUMERICAL");
	    	// M = number of views, N = number of model points
	        double[][] J = new double[2 * M * N][params.length];	// the Jacobian matrix
	        double[] refValues = new double[2 * M * N];	// function values obtained with undisturbed parameters 
	        
	        double[] s = Arrays.copyOfRange(params, 0, camParLength);
	        Camera cam = new Camera(s);
	        
	        // Step 0: calculate all 2MN reference output values (for undisturbed parameters)
	        
	        for (int row = 0, i = 0; i < M; i++) {	// for all views
	        	int start = camParLength + i * viewParLength;
				double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
				ViewTransform view = new ViewTransform(w);
	        	for (int j = 0; j < N; j++) {	// for all model points: calculate reference values
	        		double[] uv = cam.project(view, modelPts[j]);
	        		refValues[row + 0] = uv[0];
	        		refValues[row + 1] = uv[1];
	        		row = row + 2;
	        	}        	 
	        }
	        
	        // Step 1: calculate all entries of the Jacobian (brute!)
	        
	        for (int k = 0; k < params.length; k++) {	// for ALL parameters
	        	int col = k;
	        	// calculate step width and modify the parameter vector c:
	        	double pk = params[k];
	        	double delta = estimateDelta(pk);
	        	params[k] = params[k] + delta;		// modify parameter c_k
	        	
	        	double[] smod = Arrays.copyOfRange(params, 0, camParLength);
	        	Camera camMod = new Camera(smod);	// modified camera
	        	
		        for (int row = 0, i = 0; i < M; i++) {	// for all views
		        	int start = camParLength + i * viewParLength;
		        	double[] w = Arrays.copyOfRange(params, start, start + viewParLength);
		        	ViewTransform view = new ViewTransform(w);
		        	for (int j = 0; j < N; j++) {	// for all model points: calculate disturbed value
		        		Point2D Pj = modelPts[j];
		        		double[] uvMod = camMod.project(view, Pj);
		        		J[row + 0][col] = (uvMod[0] - refValues[row + 0]) / delta;   // du
		        		J[row + 1][col] = (uvMod[1] - refValues[row + 1]) / delta;   // dv	
		        		row = row + 2;
		        	}    
		        }
		        params[k] = pk;
	        }
	       
	        long endtime = System.nanoTime();
			System.out.println("time diff = " + (endtime - starttime) + " ns");
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
    	double dx = eps * Math.max(Math.abs(x), 1); 
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