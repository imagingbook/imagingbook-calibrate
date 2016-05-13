package imagingbook.extras.calibration.zhang;

import imagingbook.extras.calibration.zhang.util.MathUtil;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;



/**
 * This is an implementation of the camera calibration method 
 * described in
 *   Z. Zhang, "A flexible new technique for camera calibration",
 *   IEEE Transactions on Pattern Analysis and Machine Intelligence, 
 *   22(11), pp. 1330-1334, 2000.
 * See also http://research.microsoft.com/en-us/um/people/zhang/Calib/ and
 * http://research.microsoft.com/en-us/um/people/zhang/Papers/TR98-71.pdf
 * @author W. Burger
 * @version 2015/05/26
 *
 */
public class Calibrator {
	
	public static class Parameters {
		public boolean normalize = true;
		public boolean assumeZeroSkew = false;
		public int maxEvaluations = 1000;	// for LM-optimizer
		public int maxIterations = 1000;	// for LM-optimizer
		public int lensDistortionKoeffients = 2;
		public boolean beVerbose = false;
	}
	
//	private double alpha, beta, gamma, cx, cy; 	// camera intrinsics
//	private RealMatrix A = null;				// camera intrinsics
	
	private int M;								// the number of camera views
	private final Point2D[] modelPts;			// the sequence of 2D points in the planar model
	private final List<Point2D[]> imgPntSet; 	// list of vectors containing observed 2D image points for each view
	
	private Point2D[][] obsPoints = null;
	private final Parameters params;
	private Camera initialCam, finalCam;
	private ViewTransform[] initialViews, finalViews;
	
	// ------- constructors ------------------------------
	
	public Calibrator(Parameters params, Point2D[] model) {
		this.params = params;
		this.modelPts = model;
		this.imgPntSet = new ArrayList<Point2D[]>();
	}
	
	// attach a new observation (set of image points)
	public void addView(Point2D[] pts) {
		imgPntSet.add(pts);
	}
	
	/**
	 * The actual calibration is being done here.
	 * @return
	 */
	public Camera calibrate() {
		M = imgPntSet.size();	// number of views to process
		if (M < 2) {
			throw new IllegalStateException("Calibration: at least two views needed");
		}
		
		obsPoints = imgPntSet.toArray(new Point2D[0][]);
		
		// Step 1: Calculate the homographies for each of the given N views:
		HomographyEstimator hest = new HomographyEstimator();
		RealMatrix[] homographies = hest.estimateHomographies(modelPts, obsPoints);
		
		// Step 2: Estimate the intrinsic parameters by linear optimization:
		CameraIntrinsicsEstimator cis = new CameraIntrinsicsEstimator();
		
		RealMatrix A = cis.getIntrinsics(homographies);
		MathUtil.print("A Cholesky:", A);
		
		RealMatrix AZ1 = cis.getIntrinsicsZhang1(homographies);
		MathUtil.print("A Zhang1:", AZ1);
		
		RealMatrix AZ2 = cis.getIntrinsicsZhang2(homographies);
		MathUtil.print("A Zhang2:", AZ2);
		
		RealMatrix AZ3 = cis.getIntrinsicsZhang3(homographies);
		MathUtil.print("A Zhang3:", AZ3);
		
		initialCam = new Camera(A, new double[params.lensDistortionKoeffients]);
		
		// Step 3: calculate the extrinsic view parameters:
		ExtrinsicViewEstimator eve = new ExtrinsicViewEstimator(A);
		initialViews = eve.getExtrinsics(homographies);
		
		// Step 4: Determine the lens distortion from initial estimates:
		RadialDistortionEstimator rde = new RadialDistortionEstimator();
		double[] distParams = rde.estimateLensDistortion(initialCam, initialViews, modelPts, obsPoints);
		Camera improvedCam = new Camera(A, distParams);
		
		// Step 5: Refine all parameters by non-linear optimization
//		NonlinearOptimizer optimizer = new NonlinearOptimizerAnalytic(modelPts, observations);
		NonlinearOptimizer optimizer = new NonlinearOptimizerNumeric(modelPts, obsPoints);
		finalCam = optimizer.optimize(improvedCam, initialViews);
		finalViews = optimizer.getFinalViews();

		return finalCam;
	}
	
	
	//---------------------------------------------------------------------------
	
	@SuppressWarnings("unused")
	private void printHomographies(RealMatrix[] homographies) {
		int i = 0;
		for (RealMatrix H : homographies) {
			i++;
			MathUtil.print("Homography " + i, H); 
			
		}
	}
	
    public double getProjectionError(Camera cam, ViewTransform view, Point2D[] observed) {
    	double sqError = 0;
		for (int j = 0; j < modelPts.length; j++) {
			double[] uv = cam.project(view, modelPts[j]);
			double[] UV = MathUtil.toArray(observed[j]);
			double du = uv[0] - UV[0];
			double dv = uv[1] - UV[1];
			sqError = sqError + du * du + dv * dv;
		}
    	return sqError;
    }
    
    public double getProjectionError(Camera cam, ViewTransform[] views, Point2D[][] observed) {
    	double totalError = 0;
    	for (int i = 0; i < views.length; i++) {
    		totalError = totalError + getProjectionError(cam, views[i], observed[i]);
    	}
    	return totalError;
    }
    
    // ----------------------------------------------------------------------
    
    public Camera getInitialCamera() {
    	return initialCam;
    }
    public Camera getFinalCamera() {
    	return finalCam;
    }
    
    public ViewTransform[] getInitialViews() {
    	return initialViews;
    }
    
    public ViewTransform[] getFinalViews() {
    	return finalViews;
    }
    
}
