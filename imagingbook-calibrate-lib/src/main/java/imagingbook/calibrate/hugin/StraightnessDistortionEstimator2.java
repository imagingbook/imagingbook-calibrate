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
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.exception.TooManyEvaluationsException;
import org.apache.commons.math4.legacy.exception.TooManyIterationsException;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;
import static imagingbook.common.math.Matrix.zeroVector;

/**
 * Estimates non-linear camera distortion from sets of sensor points trusted to be on straight
 * lines.
 * Version 2: One target/Jacobian row for each straight line. Generally has similar performance as
 * version 1 but struggles when there is only a single line (just 1 target value)!
 */
public class StraightnessDistortionEstimator2 {

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    private final Camera initCam;
    private final DistortionModel initDistortion;
    private final int K;
    private final Pnt2d[][] pntArray;
    private final int lineCnt;
    private final int totalPntCnt;

    private String failureReason;   // TODO

    /**
     * Constructor.
     * The initial camera instance specifies the linear camera parameters (assumed to be known)
     * and the type of non-linear distortion model whose parameters are to be estimated.
     * @param initCam initial {@link Camera} instance
     * @param points a list of point sets, each assumed to form a straight line
     */
    public StraightnessDistortionEstimator2(Camera initCam, List<List<Pnt2d>> points) {
        this.initCam = initCam;
        this.initDistortion = initCam.getDistortion();
        this.K = initDistortion.getParameterCount();
        this.lineCnt = points.size();
        this.pntArray = new Pnt2d[points.size()][];
        int cnt = 0;
        for (int j = 0; j < pntArray.length; j++) {
            this.pntArray[j] = points.get(j).toArray(new Pnt2d[0]);
            cnt += pntArray[j].length;
        }

        this.totalPntCnt = cnt;
        System.out.println("totalPntCnt = " + totalPntCnt);
    }

    // ---------------------------------------------------------------------------------------
    class OptimizationModel extends MultivariateJacobianNumeric {

        public OptimizationModel(int rows, int cols) {
            super(rows, cols);
        }

        @Override
        double[] getValues(double[] p) {
            double[] Y = new double[lineCnt];
            DistortionModel dist = initDistortion.withParameters(p);

            // process each straight point set j:
            for (int j = 0; j < pntArray.length; j++) {
                // apply inverse warping to all points in this set
                Pnt2d[] unwarpedPts = new Pnt2d[pntArray[j].length];
                for (int i = 0; i < unwarpedPts.length; i++) {
                    unwarpedPts[i] = dist.unwarp(pntArray[j][i]);
                }
                // fit a straight line to unwarped points
                OrthogonalLineFitEigen lineFit = new OrthogonalLineFitEigen(unwarpedPts);
                Y[j] = lineFit.getError();
            }
            return Y;
        }
    }

    /**
     * Estimates distortion parameters from sets of sensor points trusted to be on straight lines.
     * @return a new camera with updated distortion model
     */
    public Camera estimateDistortion() {
        MultivariateJacobianFunction model = new OptimizationModel(lineCnt, K);
        double[] pStart = initDistortion.getParameters();

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(zeroVector(lineCnt))
                .model(model)
                .start(pStart)
                .checker(new EvaluationRmsChecker(1e-10))
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

        System.out.println("Iterations = " + result.getIterations());
        System.out.println("Evaluations = " + result.getEvaluations());
        System.out.println("RMS = " + result.getRMS());

        DistortionModel optDistortion = initDistortion.withParameters(optParams);
        return new StandardCamera(initCam.getLinearParameters(), optDistortion);
    }

    // -----------------------------------------------------------------------------------------------------

    // static List<List<Pnt2d>> sampleStraightLines(DistortionModel dist) {
    //     List<List<Pnt2d>> lines = new ArrayList<>();
    //     // horizontal lines:
    //     for (int j = 0; j <= 10; j++) {
    //         List<Pnt2d> ln = new ArrayList<>();
    //         double y = -0.5 + j * 0.1;
    //         for (int i = 0; i <= 10; i++) {
    //             double x = -0.5 + i * 0.1;
    //             ln.add(dist.warp(Pnt2d.from(x, y)));
    //         }
    //         lines.add(ln);
    //     }
    //     // vertical lines:
    //     for (int j = 0; j <= 10; j++) {
    //         List<Pnt2d> ln = new ArrayList<>();
    //         double x = -0.5 + j * 0.1;
    //         for (int i = 0; i <= 10; i++) {
    //             double y = -0.5 + i * 0.1;
    //             ln.add(dist.warp(Pnt2d.from(x, y)));
    //         }
    //         lines.add(ln);
    //     }
    //     return lines;
    // }

    static final double k0 = 0.2, k1 = -0.05, k2 = 0.02;


}
