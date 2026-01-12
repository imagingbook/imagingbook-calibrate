/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

import imagingbook.calibration.Camera;
import imagingbook.calibration.ViewTransform;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;

import java.util.List;

/**
 *  Class for estimating radial distortion parameters.
 */
class LensDistortionEstimator {

    private final ViewTransform[] views;
    private final Pnt2d[][] modPts;
    private final Pnt2d[][] obsPts;

    protected LensDistortionEstimator(ViewTransform[] views, List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {
        this.views = views;
        this.modPts = modPntSet.toArray(new Pnt2d[0][]);
        this.obsPts = obsPntSet.toArray(new Pnt2d[0][]);
    }

    /**
     *  Estimates lens distortion from multiple views, starting from an initial (linear) camera model.
     *  Given an initial estimate of the camera intrinsics (without lens distortion),
     *  the task is to find the optimal distortion parameter vector k = (k0, k1)
     *  by minimum least-squares optimization of
     *  <pre>
     *    D * k = d ,
     *  </pre>
     *  where matrix D is of size 2MN x 2, vector k of size 2, and vector d of size 2MN
     *  (M views with N observed points).
     *  @param cam the initial (linear) camera model
     */
    protected LensDistortionModel getEstimate(Camera cam) {
        final int M = views.length;		// the number of views
        final int N = modPts[0].length;	// the number of model points TODO: this varies!!!
        final LensDistortionModel distortion = cam.getDistortion();
        final int P = distortion.getParameterCount();    // number of distortion parameters

        // the estimated projection center on the sensor plane
        final double uc = cam.getUc();
        final double vc = cam.getVc();
        final RealMatrix D = MatrixUtils.createRealMatrix(2 * M * N, P);    // TODO: this varies!!!
        final RealVector d = new ArrayRealVector(2 * M * N);

        // matrix double-line counter l
        for (int k = 0, l = 0; k < M; k++) {    // iterate over M views:
            Pnt2d[] mod = modPts[k];
            Pnt2d[] obs = obsPts[k];
            ViewTransform vt = views[k];

            for (int j = 0; j < N; j++, l+=2) {   // iterate over N observed points
                final Pnt2d mpt = mod[j];    // model point
                // get point positions in the ideal image plane (normalized projection, f=1)
                double[] xy = cam.projectNormalized(vt, mpt);
                double x = xy[0], y = xy[1];

                // project 3D model point j to the sensor image, using view transform i
                double[] uv = cam.project(vt, mpt);
                double u = uv[0];
                double v = uv[1];
                double du = u-  uc;	// distance to estim. sensor projection center
                double dv = v - vc;
                // insert one pair of rows into matrix D:
                final int l0 = l;
                final int l1 = l + 1;
                // rowUV is a 2 x P matrix (submatrix of D):
                double[][] rowsUV = distortion.getDMatrixRowsUV(x, y, du, dv);
                for (int p = 0; p < P; p++) {
                    D.setEntry(l0, p, rowsUV[0][p]);
                    D.setEntry(l1, p, rowsUV[1][p]);
                }
                // mount vector d with difference between observed and predicted sensor points
                Pnt2d UV = obs[j];  // observed point
                d.setEntry(l0, UV.getX() - u);
                d.setEntry(l1, UV.getY() - v);
            }
        }

        DecompositionSolver solver = new SingularValueDecomposition(D).getSolver();
        RealVector kopt = solver.solve(d);  // optimal distortion parameter

        // keep errors for later use (optional)
        double err1 = D.operate(new ArrayRealVector(new double[P])).subtract(d).getNorm();
        double err2 = D.operate(kopt).subtract(d).getNorm();
        // System.out.format("err1=%.2f, err2=%.2f \n", err1, err2);

        return distortion.copyOf(kopt.toArray(), err1 / (M * N));   // TODO: check error quantity is avg
    }

}
