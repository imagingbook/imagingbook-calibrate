/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.distortion.LensDistortion;
import imagingbook.calibration.distortion.Radial2TermDistortion;
import imagingbook.calibration.util.MathUtil;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.util.ParameterBundle;

import org.apache.commons.math4.legacy.linear.RealMatrix;

import java.util.ArrayList;
import java.util.List;


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
	 * Inner class representing a set of parameters for instantiating new objects of type of {@link Calibrator}.
	 * Parameters can be specified by setting the associated public fields.
	 */
	public static class Parameters implements ParameterBundle<Calibrator> {
        /** Lens distortion model to be used. */
        public LensDistortion distortionModel = Radial2TermDistortion.INSTANCE;
		/** Normalize point coordinates for numerical stability in {@link Homography}. */
		public boolean normalizePoints = true;
        /** Assume that the camera has no skew (currently not used). */
		public boolean assumeZeroSkew = false;
		/** Use numeric (instead of analytic) calculation of the Jacobian in {@link NonlinearOptimizer}. */
		public boolean useNumericJacobian = false;
		/** Turn on debugging output. */
		public boolean debug = false;					
	}
	
	private int M;							// the number of camera views
	private final Pnt2d[] modelPts;			// the sequence of 2D points in the planar model
	private final List<Pnt2d[]> imgPntSet; 	// list of vectors containing observed 2D image points for each view
	private final Parameters params;
	private Camera initCam, finalCam;
	private ViewTransform[] initViews, finalViews;

    // private boolean normalizePointSets = true;
    // private boolean useNumericJacobian = false;
    // private boolean debug = false;
	
	// ------- constructors ------------------------------

	/**
	 * The only constructor.
	 * @param params a parameter object (default parameters are used if {@code null} is passed)
	 * @param model a sequence of 2D points specifying the x/y coordinates of the planar calibration pattern (assuming
	 * zero z-coordinates)
	 */
	public Calibrator(Parameters params, Pnt2d[] model) {
		this.params = (params != null) ? params : new Parameters();
		this.modelPts = model;
		this.imgPntSet = new ArrayList<>();
        assert params != null;
        // this.normalizePointSets = params.normalizePointSets;
        // this.useNumericJacobian = params.useNumericJacobian;
        // this.debug = params.debug;
	}

    // ------------ setup methods ----------------------------------------

    /**
     * Adds a new observation (a sequence of 2D image points) of the planar calibration pattern.
     * @param pts a sequence of 2D image points
     */
    public void addView(Pnt2d[] pts) {
        imgPntSet.add(pts);
    }

    // -------------------------------------------------------------------

	/**
	 * Performs the actual camera calibration based on the provided sequence of views.
	 * @return the estimated camera intrinsics as a {@link Camera} object
	 */
	public Camera calibrate() {
		M = imgPntSet.size();	// number of views to process
		if (M < 2) {
			throw new IllegalStateException("Calibration: at least two views needed");
		}
        // M views with N observed points each
        Pnt2d[][] obsPts = imgPntSet.toArray(new Pnt2d[0][]);
		
		// Step 1: Calculate the homographies for each of the given N views:
        Homography[] homographies = new Homography[M];
        for(int i = 0; i < M; i++) {
            homographies[i] = Homography.from(modelPts, obsPts[i], params.normalizePoints, true);
        }
		
		// Step 2: Estimate the intrinsic parameters by linear optimization:
		RealMatrix A_init = CameraIntrinsics.from(homographies);
		initCam = new Camera(A_init, params.distortionModel);
		
		// Step 3: calculate the extrinsic view parameters (3D view transforms)
        initViews = new ViewTransform[M];
        for (int i = 0; i < M; i++) {
            initViews[i] = ViewTransform.from(A_init, homographies[i]);
        }
		
		// Step 4: Determine the lens distortion from initial estimates:
        LensDistortion distortion =
                LensDistortionEstimate.from(initCam, initViews, modelPts, obsPts).getDistortion();
        // System.out.println("initial distortion = " + Arrays.toString(distortion.getParameters()));
		Camera improvedCam = new Camera(A_init, distortion);

		// Step 5: Refine all parameters by non-linear optimization
		NonlinearOptimizer optimizer = (params.useNumericJacobian) ?
				new NonlinearOptimizerNumeric(improvedCam, modelPts, obsPts) :
				new NonlinearOptimizerAnalytic(improvedCam, modelPts, obsPts);
		optimizer.optimize(initViews);
		finalCam = optimizer.getFinalCamera();
		finalViews = optimizer.getFinalViews();
		return finalCam;
	}


	//---------------------------------------------------------------------------
	
	// @SuppressWarnings("unused")
	// private void printHomographies(RealMatrix[] homographies) {
	// 	int i = 0;
	// 	for (RealMatrix H : homographies) {
	// 		i++;
	// 		System.out.println("Homography " + i + ":");
	// 		System.out.println(Matrix.toString(H.getData()));
	// 	}
	// }

	/**
	 * Calculates the squared projection error for a single view, associated with a set of observed image points.
	 *
	 * @param cam a camera model (camera intrinsics)
	 * @param view a view transformation (camera extrinsics)
	 * @param observed a set of observed image points
	 * @return the squared projection error (measured in pixel units)
	 */
    public double getProjectionError(Camera cam, ViewTransform view, Pnt2d[] observed) {
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
	 * Calculates the squared projection error for a sequence of views, associated with a sequence of observed image
	 * point sets.
	 *
	 * @param cam a camera model (camera intrinsics)
	 * @param views a sequence of view transformations (camera extrinsics)
	 * @param observed a sequence of sets of observed image points
	 * @return the squared projection error (measured in pixel units)
	 */
    public double getProjectionError(Camera cam, ViewTransform[] views, Pnt2d[][] observed) {
    	double totalError = 0;
    	for (int i = 0; i < views.length; i++) {
    		totalError = totalError + getProjectionError(cam, views[i], observed[i]);
    	}
    	return totalError;
    }
    
    // ----------------------------------------------------------------------

	/**
	 * Returns the initial camera model (no lens distortion).
	 *
	 * @return the initial camera model
	 */
    public Camera getInitialCamera() {
    	return initCam;
    }

	/**
	 * Returns the final camera model (including lens distortion).
	 *
	 * @return the final camera model
	 */
    public Camera getFinalCamera() {
    	return finalCam;
    }

	/**
	 * Returns the sequence of initial camera views (extrinsics, no lens distortion).
	 *
	 * @return the sequence of initial camera views
	 */
    public ViewTransform[] getInitialViews() {
    	return initViews;
    }

	/**
	 * Returns the sequence of final camera views (extrinsics, including lens distortion).
	 *
	 * @return the sequence of final camera views
	 */
    public ViewTransform[] getFinalViews() {
    	return finalViews;
    }
    
}
