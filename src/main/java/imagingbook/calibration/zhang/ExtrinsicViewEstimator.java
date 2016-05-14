package imagingbook.calibration.zhang;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import imagingbook.calibration.zhang.util.MathUtil;


public class ExtrinsicViewEstimator {
	
	static boolean beVerbose = false;
	
	private final RealMatrix A_inv;
	
	protected ExtrinsicViewEstimator(RealMatrix A) {
		this.A_inv = MatrixUtils.inverse(A);
	}
	
	
	protected ViewTransform[] getExtrinsics(RealMatrix[] homographies) {
		final int M = homographies.length;
		ViewTransform[] views = new ViewTransform[M];
		//ExtrinsicViewEstimator eve = new ExtrinsicViewEstimator(A);
		for (int i = 0; i < M; i++) {
			views[i] = estimateViewTransform(homographies[i]);
		}
		return views;
	}
	
	private ViewTransform estimateViewTransform(RealMatrix H) {
		RealVector h0 = H.getColumnVector(0);
		RealVector h1 = H.getColumnVector(1);
		RealVector h2 = H.getColumnVector(2);

		double lambda = 1 / A_inv.operate(h0).getNorm();
		if (beVerbose) {
			System.out.format("lambda = %f\n", lambda);
		}

		// compute the columns in the rotation matrix
		RealVector r0 = A_inv.operate(h0).mapMultiplyToSelf(lambda);
		RealVector r1 = A_inv.operate(h1).mapMultiplyToSelf(lambda);
		RealVector r2 = MathUtil.crossProduct3x3(r0, r1);
		RealVector t  = A_inv.operate(h2).mapMultiplyToSelf(lambda);
		
		if (beVerbose) {
			MathUtil.print("r1 = ", r0);
			MathUtil.print("r2 = ", r1);
			MathUtil.print("t  = ", t);
		}
		
		RealMatrix R = MatrixUtils.createRealMatrix(3, 3);
		R.setColumnVector(0, r0);
		R.setColumnVector(1, r1);
		R.setColumnVector(2, r2);
		if (beVerbose) {
			MathUtil.print("Rinit = ", R);
		}
				
//		// the R matrix is probably not a real rotation matrix.  So find
//		// the closest real rotation matrix
//		RealMatrix R = MathUtil.approximateRotationMatrix(R);	// not needed, View takes care of this
			
		// assemble the complete view transformation [R|T]:
//		RealMatrix RT = MatrixUtils.createRealMatrix(3, 4);
//		RT.setSubMatrix(R.getData(), 0, 0);
//		RT.setColumnVector(3, t);
		
		ViewTransform view = new ViewTransform(R, t);
		return view;
	}
	
}
