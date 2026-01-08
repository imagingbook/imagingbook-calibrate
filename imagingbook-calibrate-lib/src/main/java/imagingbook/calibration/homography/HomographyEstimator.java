/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.homography;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Arithmetic;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * Abstract super-class for homography estimators. Handles point set normalization and
 * optional refinement.
 */
public abstract class HomographyEstimator {

    final boolean normalizePoints;
    final boolean doRefinement;

    /**
     * Constructor for abstract {@link HomographyEstimator}.
     * @param normalizePoints normalize point sets for homography estimation (recommended)
     * @param doRefinement perform numerical refinement (if implemented)
     */
    HomographyEstimator(boolean normalizePoints, boolean doRefinement) {
        this.normalizePoints = normalizePoints;
        this.doRefinement = doRefinement;
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D
     * point sequences assumed to be in correspondence (and of same length).
     * Use of point set normalization (to be specified in the constructor)
     * is recommended for numerical stability
     * (see {@link HomographyEstimator#HomographyEstimator(boolean, boolean)}).
     * The resulting 3x3 homography matrix is scaled to H(2,2) = 1 if possible.
     *
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 1st sequence of 2D points
     * @return the estimated homography matrix
     */
    public final RealMatrix getHomography(Pnt2d[] ptsA, Pnt2d[] ptsB) {
        if (ptsA.length != ptsB.length) {
            throw new IllegalArgumentException("point sequences A, B have different lengths");
        }
        if (ptsA.length < 4) {
            throw new IllegalArgumentException("cannot estimate homography from less than 4 point pairs");
        }

        RealMatrix Na = null, Nb = null;
        Pnt2d[] ptsAn, ptsBn;

        if (normalizePoints) {
            Na = HomographyUtils.getNormalisationMatrix(ptsA);
            Nb = HomographyUtils.getNormalisationMatrix(ptsB);
            ptsAn = HomographyUtils.projectPoints(ptsA, Na);
            ptsBn = HomographyUtils.projectPoints(ptsB, Nb);
        }
        else {
            ptsAn = ptsA;
            ptsBn = ptsB;
        }

        // get an initial homography estimate (with normalized coordinates):
        RealMatrix Hinit = estimateHomography(ptsAn, ptsBn);    // implemented by subclasses

        // optionally refine this estimate (still with normalized coordinates):
        RealMatrix Href = (doRefinement) ? refineHomography(Hinit, ptsAn, ptsBn) : Hinit;

        // de-normalize the homography matrix:
        RealMatrix H = (normalizePoints) ? MatrixUtils.inverse(Nb).multiply(Href).multiply(Na) : Href;

        // if possible, rescale H to H(2,2) = 1:
        return (Arithmetic.isZero(H.getEntry(2, 2), 1e-15)) ?
                H : H.scalarMultiply(1.0 / H.getEntry(2, 2));
    }

    /**
     * Estimates the homography (projective) transformation from two given 2D point sets,
     * which are normalized if the {@code normalizePoints} flag is set in the constructor.
     * The correspondence between the points is assumed to be known.
     * The estimate is obtained by solving a homogeneous linear system over all 9 elements
     * of the homography matrix.
     * @param ptsA the 1st sequence of 2D points
     * @param ptsB the 2nd sequence of 2D points
     * @return the estimated homography (3 x 3 matrix)
     */
    abstract RealMatrix estimateHomography(Pnt2d[] ptsA, Pnt2d[] ptsB);

    /**
     * Refines the initial homography by non-linear optimization.
     * If {@code normalizePoints} is set, the initial homography and the two point sets
     * are supposed to be normalized, as is the resulting homography
     * @param Hinit the initial (estimated) homography
     * @param pntsA the 1st sequence of 2D points (the model points)
     * @param pntsB the 2nd sequence of 2D points (the observed image points)
     * @return the refined homography
     */
    abstract RealMatrix refineHomography(RealMatrix Hinit, Pnt2d[] pntsA, Pnt2d[] pntsB);

    // -------------------------------------------------------------------------------------------

    /**
     * Estimates the homographies between a fixed set of 2D model points and multiple observations
     * (image point sets).
     * The correspondence between the points is assumed to be known.
     * @param modelPts a sequence of 2D points on the model (calibration target)
     * @param obsPoints a sequence 2D image point sets (one set per view).
     * @return the sequence of estimated homographies (3 x 3 matrices), one for each view
     */
    public RealMatrix[] getHomographies(Pnt2d[] modelPts, Pnt2d[][] obsPoints) {
    	int M = obsPoints.length;
        RealMatrix[] homographies = new RealMatrix[M];
    	for (int i = 0; i < M; i++) {
    		homographies[i] = this.getHomography(modelPts, obsPoints[i]);
    	}
    	return homographies;
    }

}
