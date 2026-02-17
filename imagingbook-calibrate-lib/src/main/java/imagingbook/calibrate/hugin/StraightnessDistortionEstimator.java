/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.hugin;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial3TermDistortion;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.line.OrthogonalLineFitEigen;
import imagingbook.common.geometry.line.AlgebraicLine;
import imagingbook.common.math.Arithmetic;
import org.apache.commons.math4.legacy.exception.TooManyEvaluationsException;
import org.apache.commons.math4.legacy.exception.TooManyIterationsException;
import org.apache.commons.math4.legacy.fitting.leastsquares.*;

import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Estimates non-linear camera distortion from sets of sensor points trusted to be on straight
 * lines.
 */
public class StraightnessDistortionEstimator {

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    private final Camera initCam;
    private final DistortionModel initDistortion;
    private final int K;
    private final Pnt2d[][] pntArray;
    private final int totalPntCnt;

    private String failureReason;   // TODO

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
        double[] pStart = initDistortion.getParameters();

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(new double[totalPntCnt])    // zero vector
                .model(model)
                .start(pStart)
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .build();

        LevenbergMarquardtOptimizer lm = new LevenbergMarquardtOptimizer().withInitialStepBoundFactor(100);
        LeastSquaresOptimizer.Optimum result;
        try {
            result = lm.optimize(problem);
        }
        catch (TooManyIterationsException | TooManyEvaluationsException e) {
            failureReason = "maximum number of iterations or evaluations exceeded";
            return null;
        }

        double[] optParams = result.getPoint().toArray();
        DistortionModel optDistortion = initDistortion.withParameters(optParams);
        return new StandardCamera(initCam.getLinearParameters(), optDistortion);
    }

    // -----------------------------------------------------------------------------------------------------

    static List<Pnt2d> sampleLine(AlgebraicLine line) {
        return null;
    }

    public static void main(String[] args) {
        double[] A = {520, 520, 0, 320, 240};
        DistortionModel dist = new Radial3TermDistortion();
        Camera cam = new StandardCamera(A, dist);
        // create a couple of straight lines

    }
}
