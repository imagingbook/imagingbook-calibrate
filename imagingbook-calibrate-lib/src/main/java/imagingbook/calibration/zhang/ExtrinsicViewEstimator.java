package imagingbook.calibration.zhang;

import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.common.math.Matrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


/**
 * This class defines methods for estimating the extrinsic camera parameters from multiple homographies.
 *
 * @author WB
 */
public class ExtrinsicViewEstimator {

	static boolean beVerbose = false;

	private final RealMatrix A_inv;

	/**
	 * The only constructor.
	 *
	 * @param A the 3 x 3 matrix with intrinsic camera parameters
	 */
	protected ExtrinsicViewEstimator(RealMatrix A) {
		this.A_inv = MatrixUtils.inverse(A);
	}

	/**
	 * Estimates the extrinsic camera parameters for a sequence of homographies.
	 *
	 * @param homographies a set of homographies given as 3 x 3 matrices
	 * @return the sequence of extrinsic camera parameters (views), one view for each homography
	 */
	protected ViewTransform[] getExtrinsics(RealMatrix[] homographies) {
		final int M = homographies.length;
		ViewTransform[] views = new ViewTransform[M];
		// ExtrinsicViewEstimator eve = new ExtrinsicViewEstimator(A);
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
		RealVector t = A_inv.operate(h2).mapMultiplyToSelf(lambda);

		if (beVerbose) {
			System.out.println("r1 = " + Matrix.toString(r0.toArray()));
			System.out.println("r2 = " + Matrix.toString(r1.toArray()));
			System.out.println("t = " + Matrix.toString(t.toArray()));
		}

		RealMatrix R = MatrixUtils.createRealMatrix(3, 3);
		R.setColumnVector(0, r0);
		R.setColumnVector(1, r1);
		R.setColumnVector(2, r2);
		if (beVerbose) {
			System.out.println("Rinit = \n" + Matrix.toString(R.getData()));
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
