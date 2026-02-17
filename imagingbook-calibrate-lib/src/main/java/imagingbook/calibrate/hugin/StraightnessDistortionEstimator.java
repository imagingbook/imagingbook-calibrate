/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.hugin;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.line.OrthogonalLineFitEigen;
import imagingbook.common.geometry.line.AlgebraicLine;
import imagingbook.common.math.Arithmetic;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;

import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Estimates non-linear camera distortion from sets of sensor points trusted to be on straight
 * lines.
 */
public class StraightnessDistortionEstimator {

    private final Camera initCam;
    private final DistortionModel initDistortion;
    private final int K;
    private final Pnt2d[][] pntArray;
    private final int totalPntCnt;

    /**
     * Constructor.
     * The initial camera instance specifies the linear camera parameters (assumed to be known)
     * and the type of non-linear distortion model whose parameters are to be estimated.
     * @param initCam initial {@link Camera} instance
     * @param points a list of point sets, each assumed to form a straight line
     */
    public StraightnessDistortionEstimator(Camera initCam, List<List<Pnt2d>> points) {
        this.initCam = initCam;
        this.initDistortion = initCam.getDistortion();
        this.K = initDistortion.getParameterCount();
        this.pntArray = new Pnt2d[points.size()][];
        int cnt = 0;
        for (int j = 0; j < pntArray.length; j++) {
            this.pntArray[j] = points.get(j).toArray(new Pnt2d[0]);
            cnt += pntArray[j].length;
        }
        this.totalPntCnt = cnt;
    }

    // ---------------------------------------------------------------------------------------
    class OptimizationModel extends MultivariateJacobianNumeric {

        public OptimizationModel(int rows, int cols) {
            super(rows, cols);
        }

        @Override
        double[] getValues(double[] p) {
            double[] Y = new double[totalPntCnt];
            DistortionModel dist = initDistortion.withParameters(p);

            // process each straight point set j:
            for (int j = 0, row = 0; j < pntArray.length; j++) {
                // apply inverse warping to all points in this set
                Pnt2d[] unwarpedPts = new Pnt2d[pntArray[j].length];
                for (int i = 0; i < unwarpedPts.length; i++) {
                    unwarpedPts[i] = dist.unwarp(pntArray[j][i]);
                }
                // fit a straight line to unwarped points
                AlgebraicLine line = new OrthogonalLineFitEigen(unwarpedPts).getLine();
                // get individual point distances:
                for (int i = 0; i < unwarpedPts.length; i++, row++) {
                    Y[row] = sqr(line.getSignedDistance(unwarpedPts[i]));
                }
            }

            return Y;
        }

    }

    /**
     * Estimates distortion parameters from sets of sensor points trusted to be on straight lines.
     * @return a new camera with updated distortion model
     */
    public Camera estimateDistortion() {
        MultivariateJacobianFunction model = new OptimizationModel(totalPntCnt, K);


        return initCam;
    }
}
