/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate;

import imagingbook.calibrate.distortion.DistortionEstimator;
import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.DistortionModelType;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.homography.HomographyEstimator;
import imagingbook.calibrate.homography.HomographyEstimatorSimple;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.IntrinsicsEstimator;
import imagingbook.calibrate.intrinsics.IntrinsicsEstimatorConstrained;
import imagingbook.calibrate.optimize.obsolete.NonlinearOptimizer;
import imagingbook.calibrate.optimize.OverallOptimizer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.PrintPrecision;
import imagingbook.common.util.ParameterBundle;
import org.apache.commons.math4.legacy.linear.RealMatrix;

import java.util.ArrayList;
import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;

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
        public DistortionModelType distortionModelType = DistortionModelType.Radial2Term;
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

	// --------------------------------------------------------------------------------------------

	private int M;								// number of views
	private final List<Pnt2d[]> modelPntSet;
	private final List<Pnt2d[]> imagePntSet; 	// list of vectors containing observed 2D image points for each view
	private RealMatrix[] homographies = null;
	private final Parameters params;
	private final int imgWidth, imgHeight;
	private Camera initCam, finalCam;
	private List<ViewTransform> initViews;
	private List<ViewTransform> finalViews;

	// --------------------------------------------------------------

	/**
	 * The only constructor.
	 * @param params a parameter object (default parameters are used if {@code null} is passed)
	 * @param imgWidth image width (used to estimate the principal point)
	 * @param imgHeight image height (used to estimate the principal point)
	 */
	public Calibration(Parameters params, int imgWidth, int imgHeight) {
		this.params = (params != null) ? params : new Parameters();
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.modelPntSet = new ArrayList<>();
		this.imagePntSet = new ArrayList<>();
		this.M = 0;
	}

    // ------------ setup methods ----------------------------------------

	/**
	 * Adds a new "view" as a pair of 2D point sets: model points and image points.
	 * Model and image points must be of the same length and in correspondence.
	 * @param modelPts a sequence of 2D model points
	 * @param imagePts a sequence of 2D image points
	 */
	public void addView(Pnt2d[] modelPts, Pnt2d[] imagePts) {
		if (homographies != null) {
			throw new IllegalStateException("no views can be added after calibration");
		}
		if (modelPts.length != imagePts.length) {
			throw new IllegalArgumentException("model and image pt arrays must have same length");
		}
		if (modelPts.length < 4) {
			throw new IllegalArgumentException("view must contain at least 4 point pairs: " +
					modelPts.length);
		}
		modelPntSet.add(modelPts);
		imagePntSet.add(imagePts);
		M++;
	}

    // -------------------------------------------------------------------

	/**
	 * Performs the actual camera calibration based on the provided sequence of views. At least one
	 * view is required to run calibration. Use {@link #addView(Pnt2d[], Pnt2d[])} to add views.
	 */
	public void calibrate() {
		if (M < 1) {
			throw new IllegalStateException("min. one view needed to run calibration, use addView()");
		}

		// Create an initial dummy camera (standard or simple):
		initCam = new Camera(null, null);

		// Step 1: Calculate the homographies for each of the given M views:
		debug("Step 1: Calculate the homographies for each of the given " + M + " views");
        homographies = new  RealMatrix[M];
		HomographyEstimator hestmtr =
				new HomographyEstimatorSimple(
						params.normalizePoints,
						params.refineHomographies, 1000, 100);
        for (int k = 0; k < M; k++) {
			homographies[k] = hestmtr.getHomography(modelPntSet.get(k), imagePntSet.get(k));
			debug("homography" + k + ": \n" + homographies[k]);
        }

		// Step 2: Estimate intrinsic camera parameters by linear optimization:
		debug("Step 2: Estimate intrinsic camera parameters by linear optimization");
		// IntrinsicsEstimator intrEstimtr = new IntrinsicsEstimatorZhang();
		IntrinsicsEstimator intrEstm = new IntrinsicsEstimatorConstrained(imgWidth, imgHeight);
		RealMatrix Ainit = intrEstm.estimate(homographies);
		initCam = initCam.withParameters(Ainit);
        debug("initial camera = " + initCam);
		
		// Step 3: Calculate the extrinsic view parameters (3D view transforms)
		debug("Step 3: calculate the extrinsic view parameters (3D view transforms)");
        initViews = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            initViews.add(ViewTransform.from(Ainit, homographies[i]));
        }

		// Step 4: Determine the lens distortion from initial estimates:
		debug("Step 4: Estimate lens distortion from initial camera and view data:");
		DistortionModel distModel = params.distortionModelType.create(initCam, imgWidth, imgHeight);
		System.out.println("params.distortionModelType = " + params.distortionModelType);
		System.out.println("distModel = " + distModel);
		initCam.setDistortion(distModel);
		DistortionEstimator distEstim = new DistortionEstimator(initCam);
		Camera improvedCam = distEstim.getEstimate(initViews, modelPntSet, imagePntSet);
        debug("improved camera = " + improvedCam);

		// Step 5: Refine all parameters by overall non-linear optimization
		debug("Step 5: Refine all parameters by non-linear optimization");
        debug("non-linear optimization:  useNumericJacobian = " + params.useNumericJacobian);
		// NonlinearOptimizer optim = (params.useNumericJacobian) ?
		// 		new NonlinearOptimizerNumeric(improvedCam, initViews, modelPntSet, imagePntSet) :
		// 		new NonlinearOptimizerAnalytic(improvedCam, initViews, modelPntSet, imagePntSet);

		// no parameter scaling but penalizing gamma with factor 1000000, results are good!
		// (J condition No = 9.051008281593193E7), 1 iteration, 12 evaluations
		// NonlinearOptimizer optim =
		// 		new OverallNonlinearOptimizer_Unscaled(improvedCam, initViews, modelPntSet, imagePntSet);

		OverallOptimizer optim1 = new OverallOptimizer(improvedCam, initViews, modelPntSet, imagePntSet);
		optim1.fixGamma();
		optim1.fixViewParameters();

		System.out.println("improved camera = " + improvedCam);

		if (optim1.optimize()) {
			finalCam = optim1.getFinalCamera();
			finalViews = optim1.getFinalViews();
			System.out.println("optimize1: iterations = " + optim1.getIterations());
			System.out.println("optimize1: evaluations = " + optim1.getEvaluations());
			System.out.println("optimize1: |residuals| = " + optim1.getResiduals().getNorm());
			System.out.println("optimize1: RMS error = " + optim1.getRmsError());
			System.out.println("optimize1 camera = " + finalCam);
		}
		else {
			System.out.println("overall optimization 1 failed, reason: " + optim1.getFailureReason());
			finalCam = improvedCam;
			finalViews = initViews;
		}

		OverallOptimizer optim2 =
				new OverallOptimizer(finalCam, initViews, modelPntSet, imagePntSet);
		// optim2.fixLinearCameraParameters();
		// optim2.fixDistortionParameters();
		optim2.fixGamma();
		optim2.fixPrincipalPoint();

		if (optim2.optimize()) {
			finalCam = optim2.getFinalCamera();
			finalViews = optim2.getFinalViews();
			System.out.println("optimize2: iterations = " + optim2.getIterations());
			System.out.println("optimize2: evaluations = " + optim2.getEvaluations());
			System.out.println("optimize2: |residuals| = " + optim2.getResiduals().getNorm());
			System.out.println("optimize2: RMS error = " + optim2.getRmsError());
			System.out.println("optimize2 camera = " + finalCam);
		}
		else {
			System.out.println("overall optimization 2 failed, reason: " + optim2.getFailureReason());
			// finalCam = improvedCam;
			// finalViews = initViews;
		}

	}

	//---------------------------------------------------------------------------

	/**
	 * Returns the RMS reprojection error for the final result of this {@link Calibration} instance.
	 * @return the RMS reprojection error (average error per projected point)
	 */
	public double getRmsReprojectionError() {
		return getRmsReprojectionError(finalCam, finalViews, modelPntSet, imagePntSet);
	}

	// public double getRmsReprojectionErrorCheck() {
	// 	int m = finalViews.size();
	// 	double squaredErrSum = 0.0;
	// 	int n = 0;
	// 	for (int k = 0; k < m; k++) {
	// 		ViewTransform view = finalViews.get(k);
	// 		Pnt2d[] modelPts = modelPntSet.get(k);
	// 		Pnt2d[] imagePts = imagePntSet.get(k);
	// 		int nk = modelPntSet.get(k).length;
	// 		for (int j = 0; j < nk; j++) {
	// 			double[] uv = finalCam.project(view, modelPts[j]);
	// 			double[] UV = imagePts[j].toDoubleArray();
	// 			double dj2 = sqr(uv[0] - UV[0]) + sqr(uv[1] - UV[1]); // squared distance
	// 			squaredErrSum += dj2;
	// 		}
	// 		n += nk;
	// 	}
	// 	return Math.sqrt(squaredErrSum / n);
	// }

	/**
	 * Calculates and returns the total RMS reprojection error for multiple views with the same camera,
	 * specified by the camera and view parameters.
	 * <pre>{@code
	 *     $RMS_{total} = \sqrt{\frac{\sum_{k=1}^{M} (N_k \cdot RMS_k^2)}{\sum_{k=1}^{M} N_k}}$
	 * }</pre>
	 * @param cam camera parameters
	 * @param viewList list of {@link ViewTransform} instances, one for each view
	 * @param modelPtsList list of model point arrays, one for each view
	 * @param imagePtsList list of image point arrays, one for each view
	 * @return the RMS reprojection error for multiple views
	 */
	public double getRmsReprojectionError(Camera cam, List<ViewTransform> viewList, List<Pnt2d[]> modelPtsList, List<Pnt2d[]> imagePtsList) {
		if (viewList.size() != modelPtsList.size() || viewList.size() != imagePtsList.size()) {
			throw new IllegalArgumentException("view, model and image point lists must have same size");
		}
		int m = viewList.size();
		double squaredErrSum = 0.0;
		int n = 0;
		for (int k = 0; k < m; k++) {
			int nk = modelPtsList.get(k).length;
			squaredErrSum += nk * sqr(getRmsReprojectionError(cam, viewList.get(k), modelPtsList.get(k), imagePtsList.get(k)));
			n += nk;
		}
		return Math.sqrt(squaredErrSum / n);
	}


	/**
	 * Calculates and returns the RMS reprojection error for a single view of the final calibration
	 * result.
	 * @param k	view number
	 * @return the RMS reprojection error for a single view of the final calibration result
	 */
	public double getRmsReprojectionError(int k) {
		return getRmsReprojectionError(finalCam, finalViews.get(k), modelPntSet.get(k), imagePntSet.get(k));
	}

	/**
	 * Calculates and returns the RMS reprojection error for a single view,
	 * specified by the camera and view parameters.
	 * <pre>{@code
	 *     $RMS_k = \sqrt{ \frac{1}{N_k}  \sum_{j=1}^{N_k} d_j^2 }$
	 * }</pre>
	 * @param cam camera parameters
	 * @param view view parameters
	 * @param modelPts model points
	 * @param imagePts image points
	 * @return the RMS reprojection error for a single view
	 */
    public double getRmsReprojectionError(Camera cam, ViewTransform view, Pnt2d[] modelPts, Pnt2d[] imagePts) {
        if (modelPts.length != imagePts.length) {
            throw new IllegalStateException("model and image pt arrays must have same length");
        }
    	 double sqError = 0;
		 for (int j = 0; j < modelPts.length; j++) {
		 	double[] uv = cam.project(view, modelPts[j]);
		 	double[] UV = imagePts[j].toDoubleArray();
			 double dj = sqr(uv[0] - UV[0]) + sqr(uv[1] - UV[1]); // squared distance
		 	sqError += dj;
		 }
    	 return Math.sqrt(sqError / modelPts.length);
    }
    
    // ----------------------------------------------------------------------

	/**
	 * Returns the number of views used for this calibration.
	 * @return the number of views
	 */
	public int getNumberOfViews() {
		return M;
	}

	public Pnt2d[] getModelPoints(int k) {
		return modelPntSet.get(k);
	}

	public Pnt2d[] getImagePoints(int k) {
		return imagePntSet.get(k);
	}

	/**
	 * Returns the initial homography estimates for the specified view.
	 * @param k the view number
	 * @return the estimated homography
	 */
	public RealMatrix getHomography(int k) {
        checkState();
		return homographies[k];
	}

	/**
	 * Returns the initial camera model (no lens distortion).
	 * @return the initial camera model
	 */
    public Camera getInitialCamera() {
        checkState();
    	return initCam;
    }

	/**
	 * Returns the final camera model (including lens distortion).
	 * @return the final camera model
	 */
    public Camera getFinalCamera() {
        checkState();
    	return finalCam;
    }

	/**
	 * Returns the specified initial view transform (extrinsics, no lens distortion).
	 * @param k the view number
	 * @return the initial view transform
	 */
    public ViewTransform getInitialViewTransform(int k) {
        checkState();
    	return initViews.get(k);
    }

	/**
	 * Returns the specified final view transform (extrinsics, no lens distortion).
	 * @param k the view number
	 * @return the initial view transform
	 */
	public ViewTransform getFinalViewTransform(int k) {
        checkState();
		return finalViews.get(k);
	}

	// ----------------------------------------------------------------------

    private void checkState() {
        if (homographies == null) {
            throw new IllegalStateException("calibration not initialized, run calibrate() first");
        }
    }

	void debug(String msg) {
		if (params.debug) {
			try(var prec = PrintPrecision.set(6)) {
				System.out.println("[Debug] " + msg);
			}
		}
	}
    
}
