/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.plumbline;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.process.ByteProcessor;
import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial3TermDistortion;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.calibrate.optimize.support.FiniteDifferenceModel;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.line.OrthogonalLineFitEigen;
import imagingbook.common.geometry.line.AlgebraicLine;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;
import imagingbook.common.math.PrintPrecision;
import org.apache.commons.math4.legacy.exception.TooManyEvaluationsException;
import org.apache.commons.math4.legacy.exception.TooManyIterationsException;
import org.apache.commons.math4.legacy.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;

import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;
import static imagingbook.common.math.Matrix.zeroVector;

/**
 * Estimates non-linear camera distortion from sets of sensor points trusted to be on straight
 * lines.
 * Version 1: One target/Jacobian row for each observed POINT.
 */
public class StraightnessDistortionEstimator1 {

    private static int maxEvaluations = 1000;
    private static int maxIterations  = 1000;

    private final Camera initCam;
    private final AffineMapping2D sensorToNormalizedMapping;
    private final AffineMapping2D normalizedToSensorMapping;
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
     * @param points a list of point sets, each assumed to form a straight line (image coordinates)
     */
    public StraightnessDistortionEstimator1(Camera initCam, List<List<Pnt2d>> points) {
        this.initCam = initCam;
        this.sensorToNormalizedMapping = new AffineMapping2D(initCam.getAffineMatrixInverse().getData());
        this.normalizedToSensorMapping = new AffineMapping2D(initCam.getAffineMatrix().getData());

        this.initDistortion = initCam.getDistortion();
        this.K = initDistortion.getParameterCount();
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

    class StraightnessOptimizationModel1 extends FiniteDifferenceModel {

        final double huberDelta = 1e-5;     // for Pseudo-Huber function (1e-7 works best)

        public StraightnessOptimizationModel1(int rows, int cols) {
            super(rows, cols);
        }

        @Override
        double[] getValues(double[] p) {
            // PrintPrecision.set(6);
            // System.out.println("getValues(): p = " + Matrix.toString(p));
            double[] Y = new double[totalPntCnt];
            DistortionModel distortion = initDistortion.withParameters(p);

            // process each collinear point set j:
            for (int j = 0, row = 0; j < pntArray.length; j++) {
                // apply inverse warping to all points in this set
                Pnt2d[] unwarpedPts = new Pnt2d[pntArray[j].length];
                for (int i = 0; i < unwarpedPts.length; i++) {
                    Pnt2d xy = sensorToNormalizedMapping.applyTo(pntArray[j][i]);
                    unwarpedPts[i] = distortion.unwarp(xy);
                }
                // fit a straight line to unwarped points
                AlgebraicLine line = new OrthogonalLineFitEigen(unwarpedPts).getLine();
                // get individual point distances:
                for (int i = 0; i < unwarpedPts.length; i++, row++) {
                    double d = line.getSignedDistance(unwarpedPts[i]);
                    // Y[row] = sqr(d);    // = version B
                    // Y[row] = d;      // = version C
                    // Y[row] = Math.abs(d);    // = Version D works best???
                    // Y[row] = Math.signum(d) * sqr(d) ;   // Version E
                    Y[row] = Math.sqrt(sqr(d) + huberDelta * huberDelta) - huberDelta;  // Version F: Pseudo-Huber function (effectively |d|)
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
        MultivariateJacobianFunction model = new StraightnessOptimizationModel1(totalPntCnt, K);
        double[] pStart = initDistortion.getParameters();

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .target(zeroVector(totalPntCnt))
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
            throw new RuntimeException(e);
            // return null;
        }

        double[] optParams = result.getPoint().toArray();

        System.out.println("Iterations = " + result.getIterations());
        System.out.println("Evaluations = " + result.getEvaluations());
        System.out.println("RMS = " + result.getRMS());

        DistortionModel optDistortion = initDistortion.withParameters(optParams);
        return new StandardCamera(initCam.getLinearParameters(), optDistortion);
    }

    // -----------------------------------------------------------------------------------------------------

    static final double k0 = 0.2, k1 = -0.05, k2 = 0.02;

    // static List<List<Pnt2d>> sampleStraightLines(DistortionModel dist) {
    //     List<List<Pnt2d>> lines = new ArrayList<>();
    //     for (int j = 0; j <= 10; j++) {
    //         List<Pnt2d> ln = new ArrayList<>();
    //         double y = -0.5 + j * 0.1;
    //         for (int i = 0; i <= 10; i++) {
    //             double x = -0.5 + i * 0.1;
    //             ln.add(dist.warp(Pnt2d.from(x, y)));
    //         }
    //         lines.add(ln);
    //     }
    //     return lines;
    // }

    public static void main(String[] args) {
        double[] A = {520, 520, 0, 320, 240};
        DistortionModel realDist = new Radial3TermDistortion(new double[] {k0, k1, k2});
        DistortionModel initDist = new Radial3TermDistortion(new double[] {0, 0, 0});
        Camera realCam = new StandardCamera(A, realDist);
        Camera initCam = new StandardCamera(A, initDist);

        // create collinear image point sets using the real distortion
        List<List<Pnt2d>> lines = new CollinearPointsGenerator(realCam).makeCollinearPoints(10, 10, false);
        System.out.println("lines = " + lines.size());

        Overlay oly = CollinearPointsGenerator.makeOverlay(lines, 640 * 0.01);
        ImagePlus im = new ImagePlus("img", new ByteProcessor(640, 480));
        im.setOverlay(oly);
        im.show();

        StraightnessDistortionEstimator1 estimator = new StraightnessDistortionEstimator1(initCam, lines);
        Camera newCam = estimator.estimateDistortion();
        PrintPrecision.set(6);
        System.out.println("result = " + newCam.getDistortion());

    }
}
