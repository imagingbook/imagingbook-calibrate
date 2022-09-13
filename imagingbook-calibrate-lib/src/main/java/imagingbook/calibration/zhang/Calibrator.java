package imagingbook.calibration.zhang;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;

import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.common.math.Matrix;
import imagingbook.common.util.parameters.DialogParameters;


/**
 * This is an implementation of the camera calibration method 
 * described in
 *   Z. Zhang, "A flexible new technique for camera calibration",
 *   IEEE Transactions on Pattern Analysis and Machine Intelligence, 
 *   22(11), pp. 1330-1334, 2000.
 * See also 
 * <a href="http://research.microsoft.com/en-us/um/people/zhang/Calib/">
 * http://research.microsoft.com/en-us/um/people/zhang/Calib/</a>
 * and
 * <a href="http://research.microsoft.com/en-us/um/people/zhang/Papers/TR98-71.pdf">
 * http://research.microsoft.com/en-us/um/people/zhang/Papers/TR98-71.pdf</a>
 * 
 * @author W. Burger
 * @version 2018/12/29
 */
public class Calibrator {
	
	/**
	 * Inner class representing a set of parameters for instantiating
	 * new objects of type of {@link Calibrator}. 
	 * Parameters can be specified by setting the associated public fields.
	 */
	public static class Parameters implements DialogParameters {
		/** Normalize point coordinates for numerical stability in {@link HomographyEstimator}. */
		public boolean normalizePointCoordinates = true;
		/** Assume that the camera has no skew (currently not used). */
		public boolean assumeZeroSkew = false;
		/** Use numeric (instead of analytic) calculation of the Jacobian in {@link NonlinearOptimizer}. */
		public boolean useNumericJacobian = false;
		/** Number of lens distortion coefficients (2 = simple polynomial model). */
		public int lensDistortionKoeffients = 2;
		/** Turn on debugging output. */
		public boolean debug = false;					
	}
	
	private int M;								// the number of camera views
	private final Point2D[] modelPts;			// the sequence of 2D points in the planar model
	private final List<Point2D[]> imgPntSet; 	// list of vectors containing observed 2D image points for each view
	
	private Point2D[][] obsPts = null;
	private final Parameters params;
	private Camera initCam, finalCam;
	private ViewTransform[] initViews, finalViews;
	
	// ------- constructors ------------------------------
	
	/**
	 * The only constructor. 
	 * @param params a parameter object (default parameters are used if {@code null} is passed)
	 * @param model a sequence of 2D points specifying the x/y coordinates of 
	 * 			the planar calibration pattern (assuming zero z-coordinates)
	 */
	public Calibrator(Parameters params, Point2D[] model) {
		this.params = (params != null) ? params : new Parameters();
		this.modelPts = model;
		this.imgPntSet = new ArrayList<Point2D[]>();
	}
	
	/**
	 * Adds a new observation (a sequence of 2D image points) of the planar calibration pattern.
	 * @param pts a sequence of 2D image points
	 */
	public void addView(Point2D[] pts) {
		imgPntSet.add(pts);
	}
	
	/**
	 * Performs the actual camera calibration based on the provided sequence of views.
	 * @return the estimated camera intrinsics as a {@link Camera} object
	 */
	public Camera calibrate() {
		M = imgPntSet.size();	// number of views to process
		if (M < 2) {
			throw new IllegalStateException("Calibration: at least two views needed");
		}
		
		obsPts = imgPntSet.toArray(new Point2D[0][]);
		
		// Step 1: Calculate the homographies for each of the given N views:
		HomographyEstimator hest = new HomographyEstimator(params.normalizePointCoordinates, true);
		RealMatrix[] H_init = hest.estimateHomographies(modelPts, obsPts);
		
		// Step 2: Estimate the intrinsic parameters by linear optimization:
		CameraIntrinsicsEstimator cis = new CameraIntrinsicsEstimator();
		
		RealMatrix A_init = cis.getCameraIntrinsics(H_init);
		initCam = new Camera(A_init, new double[params.lensDistortionKoeffients]);
		
		// Step 3: calculate the extrinsic view parameters:
		ExtrinsicViewEstimator eve = new ExtrinsicViewEstimator(A_init);
		initViews = eve.getExtrinsics(H_init);
		
		// Step 4: Determine the lens distortion from initial estimates:
		RadialDistortionEstimator rde = new RadialDistortionEstimator();
		double[] distParams = rde.estimateLensDistortion(initCam, initViews, modelPts, obsPts);
		Camera improvedCam = new Camera(A_init, distParams);
		
		// Step 5: Refine all parameters by non-linear optimization
		NonlinearOptimizer optimizer = (params.useNumericJacobian) ?
				new NonlinearOptimizerNumeric(modelPts, obsPts) :
				new NonlinearOptimizerAnalytic(modelPts, obsPts);
		optimizer.optimize(improvedCam, initViews);
		finalCam = optimizer.getFinalCamera();
		finalViews = optimizer.getFinalViews();

		return finalCam;
	}
	
	
	//---------------------------------------------------------------------------
	
	@SuppressWarnings("unused")
	private void printHomographies(RealMatrix[] homographies) {
		int i = 0;
		for (RealMatrix H : homographies) {
			i++;
			System.out.println("Homography " + i + ":");
			System.out.println(Matrix.toString(H.getData()));
		}
	}
	
	/**
	 * Calculates the squared projection error for a single view, associated
	 * with a set of observed image points.
	 * @param cam a camera model (camera intrinsics)
	 * @param view a view transformation (camera extrinsics)
	 * @param observed a set of observed image points
	 * @return the squared projection error (measured in pixel units)
	 */
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
    
	/**
	 * Calculates the squared projection error for a sequence of views, associated
	 * with a sequence of observed image point sets.
	 * @param cam a camera model (camera intrinsics)
	 * @param views a sequence of view transformations (camera extrinsics)
	 * @param observed a sequence of sets of observed image points
	 * @return the squared projection error (measured in pixel units)
	 */
    public double getProjectionError(Camera cam, ViewTransform[] views, Point2D[][] observed) {
    	double totalError = 0;
    	for (int i = 0; i < views.length; i++) {
    		totalError = totalError + getProjectionError(cam, views[i], observed[i]);
    	}
    	return totalError;
    }
    
    // ----------------------------------------------------------------------
    
    /**
     * Returns the initial camera model (no lens distortion).
     * @return the initial camera model
     */
    public Camera getInitialCamera() {
    	return initCam;
    }
    
    /**
     * Returns the final camera model (including lens distortion).
     * @return the final camera model
     */
    public Camera getFinalCamera() {
    	return finalCam;
    }
    
    /**
     * Returns the sequence of initial camera views (extrinsics, no 
     * lens distortion).
     * @return the sequence of initial camera views
     */
    public ViewTransform[] getInitialViews() {
    	return initViews;
    }
    
    /**
     * Returns the sequence of final camera views (extrinsics, including 
     * lens distortion).
     * @return the sequence of final camera views
     */
    public ViewTransform[] getFinalViews() {
    	return finalViews;
    }
    
}
