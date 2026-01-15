/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.List;

/**
 *  Class for estimating radial distortion parameters.
 *  Image width and height are required for normalizing the radius.
 */
public class DistortionEstimator {

    private final Camera initCam;
    private final DistortionModel distModel;

    public DistortionEstimator(Camera initCam, DistortionModel distModel) {
        this.distModel = distModel;
        this.initCam = initCam;
    }

    /**
     * Estimates lens distortion from multiple views, starting from an initial (linear) camera
     * model. Given an initial estimate of the camera intrinsics (without lens distortion), the task
     * is to find the optimal distortion parameter vector k = (k0, k1) by minimum least-squares
     * optimization of
     * <pre>
     *    D * k = d ,
     *  </pre>
     * where matrix D is of size 2MN x 2, vector k of size 2, and vector d of size 2MN (M views with
     * N observed points).
     *
     * @param viewList a sequence of M extrinsic view transformations
     * @param modPntSet a sequence of M 2D model points
     * @param obsPntSet a sequence of M 2D image points
     */
    public Camera getEstimate(List<ViewTransform> viewList,
                              List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {

        int P = distModel.getParameterCount();          // number of distortion parameters
        if (P == 0) {
            return initCam; // nothing to optimize
        }

        ViewTransform[] views = viewList.toArray(new ViewTransform[0]);
        Pnt2d[][] modPts = modPntSet.toArray(new Pnt2d[0][]);
        Pnt2d[][] obsPts = obsPntSet.toArray(new Pnt2d[0][]);

        int M = views.length;		                    // the number of views
        int N = Arrays.stream(modPts).mapToInt(row -> row.length).sum(); // number of points in all views

        // the estimated projection center on the sensor plane
        double uc = initCam.getUc();
        double vc = initCam.getVc();
        RealMatrix D = MatrixUtils.createRealMatrix(2 * N, P);
        RealVector d = new ArrayRealVector(2 * N);

        // matrix double-line counter l
        for (int k = 0, row = 0; k < M; k++) {    // iterate over M views:
            Pnt2d[] mod = modPts[k];
            Pnt2d[] obs = obsPts[k];
            ViewTransform vt = views[k];

            for (int j = 0; j < modPts[k].length; j++, row+=2) {   // iterate over N observed points
                final Pnt2d XY = mod[j];    // model point
                // get point positions in the ideal image plane (normalized projection, f=1)
                double[] xy = initCam.projectNormalized(vt, XY);
                double x = xy[0];
                double y = xy[1];

                // project 3D model point j to the sensor image, using view transform i
                // double[] uv = initCam.project(vt, XY);
                // map from normalized projection to sensor coordinates (no distortion!)
                double[] uv = initCam.mapToSensorPlane(xy);
                double u = uv[0];
                double v = uv[1];
                double du = u - uc;	// distance to estim. sensor projection center
                double dv = v - vc;
                // for each point, insert one pair of rows into matrix D (rowUV is a 2xP matrix):
                double[][] rowsUV = distModel.getDMatrixRowsUV(x, y, du, dv);
                D.setRow(row + 0, rowsUV[0]);
                D.setRow(row + 1, rowsUV[1]);

                // mount vector d with difference between observed and predicted sensor points
                Pnt2d UV = obs[j];  // observed point
                d.setEntry(row + 0, UV.getX() - u);
                d.setEntry(row + 1, UV.getY() - v);
            }
        }
        // System.out.println("D = \n" + Matrix.toString(D));
        // System.out.println("d = \n" + Matrix.toString(d));

        // DecompositionSolver solver = new SingularValueDecomposition(D).getSolver();
        DecompositionSolver solver = new QRDecomposition(D).getSolver();
        // ----------------------------------------------------------------------------------
        RealVector kopt = solver.solve(d);  // optimal distortion parameter

        // ----------------------------------------------------------------------------------

        // keep errors for later use (optional)
        double err1 = D.operate(new ArrayRealVector(new double[P])).subtract(d).getNorm();
        double err2 = D.operate(kopt).subtract(d).getNorm();
        // System.out.format("err1=%.2f, err2=%.2f \n", err1, err2);

        DistortionModel dist = distModel.fromParameters(kopt.toArray());
        return new Camera(initCam.getAffineMatrix(), dist);
    }

    // private static int getTotalPointCount(Pnt2d[][] modPts) {
    //     int total = 0;
    //     for (Pnt2d[] p : modPts) {
    //         total += p.length;
    //     }
    //     return total;
    // }


}
