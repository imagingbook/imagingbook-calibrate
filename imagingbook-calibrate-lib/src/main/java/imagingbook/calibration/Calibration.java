/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.distortion.LensDistortion;
import imagingbook.calibration.distortion.Radial2TermDistortion;
import imagingbook.calibration.homography.HomographyEstimator;
import imagingbook.calibration.homography.HomographyEstimatorSimple;
import imagingbook.calibration.intrinsics.IntrinsicsEstimator;
import imagingbook.calibration.intrinsics.IntrinsicsEstimatorConstrained;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.PrintPrecision;
import imagingbook.common.util.ParameterBundle;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * <p>This is the main camera calibration class.
 * Instances of {@link Calibration} are supposed to be used in the following way:</p>
 * <ol>
 *   <li>Instantiate {@link Calibration} with the required parameters.</li>
 *   <li>Successively add "views" to the calibrator using 
 *   {@link Calibration#addView(Pnt2d[], Pnt2d[])},
 *   each view being a pair of corresponding 2D model points and observed image
 *   points. At least one view is required but more views give more accurate results.</li>
 *   <li>Call {@link Calibration#calibrate()} to perform actual camera calibration.</li>
 *   <li>Query the {@link Calibration} instance for intermediate and final results.</li>
 * </ol>
 * <p> Note that {@link Calibration#calibrate()} may be only called once on a given {@link Calibration}
 * instance and results can only be queried after calling {@link Calibration#calibrate()}.
 * At least one view must have been added before executing {@link Calibration#calibrate()}.
 * All view data are assumed to be taken with the same camera and lens but not necessarily with
 * the same reference model. The resulting camera parameters are in pixel units, which can
 * be easily converted to metric units from the dimensions of the camera sensor, of which the
 * calibrator itself is unaware.</p>
 *
 * @author W. Burger
 * @version 2026/01/10
 */
public class Calibration {

	/**
	 * Inner class representing a set of parameters for instantiating new objects of type of {@link Calibration}.
	 * Parameters can be specified by setting the associated public fields.
	 */
	public static class Parameters implements ParameterBundle<Calibration> {
        /** Lens distortion model to be used. */
        public LensDistortion distortionModel = Radial2TermDistortion.INSTANCE;
		/** Normalize point coordinates for numerical stability in homography estimation. */
		public boolean normalizePoints = true;
		/** Perform non-linear refinement of homographies (usually not needed). */
		public boolean refineHomographies = true;
        /** Assume that the camera has no skew (currently not used). */
		public boolean assumeZeroSkew = false;
		/** Use numeric (instead of analytic) calculation of the Jacobian in {@link NonlinearOptimizer}. */
		public boolean useNumericJacobian = true;
		/** Turn on debugging output. */
		public boolean debug = false;					
	}
	
	private int M;							// the number of camera views
	private final Pnt2d[] modelPts;			// the sequence of 2D points in the planar model
	private final List<Pnt2d[]> imgPntSet; 	// list of vectors containing observed 2D image points for each view
	private final Parameters params;
	private final int imgWidth, imgHeight;

	private Camera initCam, finalCam;
	private ViewTransform[] initViews, finalViews;
	
	// ------- constructors ------------------------------

	/**
	 * The only constructor.
	 *
	 * @param params    a parameter object (default parameters are used if {@code null} is passed)
	 * @param model     a sequence of 2D points specifying the x/y coordinates of the planar calibration pattern (assuming
	 *                  zero z-coordinates)
	 * @param imgWidth image width (used to estimate the principal point)
	 * @param imgHeight image height (used to estimate the principal point)
	 */
	public Calibration(Parameters params, Pnt2d[] model, int imgWidth, int imgHeight) {
		this.params = (params != null) ? params : new Parameters();
		this.modelPts = model;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.imgPntSet = new ArrayList<>();
        assert params != null;
	}

    // ------------ setup methods ----------------------------------------

    // /**
    //  * Adds a new observation (a sequence of 2D image points) of the planar calibration pattern.
    //  * @param pts a sequence of 2D image points
    //  */
	// @Deprecated
    // public void addView(Pnt2d[] pts) {
    //     imgPntSet.add(pts);
    // }

	// @Deprecated
	// public void addViews(Pnt2d[][] views) {
	// 	for (Pnt2d[] pts : views) {
	// 		addView(pts);
	// 	}
	// }


	/**
	 * Adds a new observation (a sequence of 2D image points) of the planar calibration pattern.
	 * Model and image points must be of the same lenbgth and in correspondence.
	 * @param modelPts a sequence of 2D model points
	 * @param imagePts a sequence of 2D image points
	 */
	public void addView(Pnt2d[] modelPts, Pnt2d[] imagePts) {
		if (modelPts.length != imagePts.length) {
			throw new IllegalArgumentException("model and image pt arrays must have same length");
		}
		imgPntSet.add(imagePts);
	}
	
    // -------------------------------------------------------------------

	/**
	 * Performs the actual camera calibration based on the provided sequence of views.
	 * @return the estimated camera intrinsics as a {@link Camera} object
	 */
	public Camera calibrate() {
		M = imgPntSet.size();	// number of views to process
		// if (M < 2) {
		// 	throw new IllegalStateException("Calibration: at least two views needed");
		// }
        // M views with N observed points each
        Pnt2d[][] obsPts = imgPntSet.toArray(new Pnt2d[0][]);
		
		// Step 1: Calculate the homographies for each of the given N views:
		debug("Step 1: Calculate the homographies for each of the given " + M + " views");
        RealMatrix[] homographies = new  RealMatrix[M];
		HomographyEstimator hestmtr = new HomographyEstimatorSimple(params.normalizePoints, params.refineHomographies, 1000, 100);
        for (int i = 0; i < M; i++) {
            // homographies[i] = Homography.from(modelPts, obsPts[i], params.normalizePoints, params.refineHomographies);
			homographies[i] = hestmtr.getHomography(modelPts, obsPts[i]);
			debug("homography" + i + ": \n" + homographies[i]);
        }
		
		// Step 2: Estimate intrinsic camera parameters by linear optimization:
		debug("Step 2: Estimate intrinsic camera parameters by linear optimization");
		// IntrinsicsEstimator intrEstimtr = new IntrinsicsEstimatorZhang();
		IntrinsicsEstimator intrEstimtr = new IntrinsicsEstimatorConstrained(imgWidth, imgHeight);
		RealMatrix Ainit = intrEstimtr.estimate(homographies);
		initCam = new Camera(Ainit, params.distortionModel);
        debug("initial camera = " + initCam);
		
		// Step 3: calculate the extrinsic view parameters (3D view transforms)
		debug("Step 3: calculate the extrinsic view parameters (3D view transforms)");
        initViews = new ViewTransform[M];
        for (int i = 0; i < M; i++) {
            initViews[i] = ViewTransform.from(Ainit, homographies[i]);
        }
		
		// Step 4: Determine the lens distortion from initial estimates:
		debug("Step 4: Determine the lens distortion from initial estimates:");
        LensDistortion distortion = LensDistortion.from(initCam, initViews, modelPts, obsPts);
        debug("initial distortion = " + Arrays.toString(distortion.getParameters()));
		Camera improvedCam = new Camera(Ainit, distortion);
        debug("improved camera = " + improvedCam);

		// Step 5: Refine all parameters by non-linear optimization
		debug("Step 5: Refine all parameters by non-linear optimization");
        debug("non-linear optimization:  useNumericJacobian = " + params.useNumericJacobian);
		NonlinearOptimizer optimizer = (params.useNumericJacobian) ?
				new NonlinearOptimizerNumeric(improvedCam, modelPts, obsPts) :
				new NonlinearOptimizerAnalytic(improvedCam, modelPts, obsPts);
		optimizer.optimize(initViews);
		finalCam = optimizer.getFinalCamera();
        debug("final camera = " + finalCam);
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
			double[] UV = observed[j].toDoubleArray();
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

	void debug(String msg) {
		if (params.debug) {
			PrintPrecision.set(6);
			System.out.println("[Debug] " + msg);
		}
	}
    
}
