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

import java.util.List;

/**
 *  Class for estimating radial distortion parameters.
 *  Image width and height are required for normalizing the radius.
 */
public class DistortionEstimator {

    private final DistortionModel distModel;
    private final int imgWidth;
    private final int imgHeight;

    public DistortionEstimator(DistortionModel distModel, int imgWidth, int imgHeight) {
        this.distModel = distModel;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
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
     * @param initCam the initial (linear) camera model
     * @param viewList a sequence of M extrinsic view transformations
     * @param modPntSet a sequence of M 2D model points
     * @param obsPntSet a sequence of M 2D image points
     */
    public Camera getEstimate(Camera initCam, List<ViewTransform> viewList,
                              List<Pnt2d[]> modPntSet, List<Pnt2d[]> obsPntSet) {

        ViewTransform[] views = viewList.toArray(new ViewTransform[0]);
        Pnt2d[][] modPts = modPntSet.toArray(new Pnt2d[0][]);
        Pnt2d[][] obsPts = obsPntSet.toArray(new Pnt2d[0][]);

        int pointCount = getTotalPointCount(modPts);
        int M = views.length;		// the number of views
        // DistortionModel model = initCam.getDistortion();
        int P = distModel.getParameterCount();    // number of distortion parameters

        // the estimated projection center on the sensor plane
        double uc = initCam.getUc();
        double vc = initCam.getVc();
        RealMatrix D = MatrixUtils.createRealMatrix(2 * pointCount, P);
        RealVector d = new ArrayRealVector(2 * pointCount);

        // matrix double-line counter l
        for (int k = 0, row = 0; k < M; k++) {    // iterate over M views:
            Pnt2d[] mod = modPts[k];
            Pnt2d[] obs = obsPts[k];
            ViewTransform vt = views[k];

            for (int j = 0; j < modPts[k].length; j++, row+=2) {   // iterate over N observed points
                final Pnt2d mpt = mod[j];    // model point
                // get point positions in the ideal image plane (normalized projection, f=1)
                double[] xy = initCam.projectNormalized(vt, mpt);
                double x = xy[0], y = xy[1];

                // project 3D model point j to the sensor image, using view transform i
                double[] uv = initCam.project(vt, mpt);
                double u = uv[0];
                double v = uv[1];
                double du = u-  uc;	// distance to estim. sensor projection center
                double dv = v - vc;
                // insert one pair of rows into matrix D:
                // rowUV is a 2 x P matrix (submatrix of D):
                double[][] rowsUV = distModel.getDMatrixRowsUV(x, y, du, dv);
                for (int p = 0; p < P; p++) {
                    D.setEntry(row + 0, p, rowsUV[0][p]);
                    D.setEntry(row + 1, p, rowsUV[1][p]);
                }
                // mount vector d with difference between observed and predicted sensor points
                Pnt2d UV = obs[j];  // observed point
                d.setEntry(row + 0, UV.getX() - u);
                d.setEntry(row + 1, UV.getY() - v);
            }
        }

        // DecompositionSolver solver = new SingularValueDecomposition(D).getSolver();
        DecompositionSolver solver = new QRDecomposition(D).getSolver();
        // ----------------------------------------------------------------------------------
        RealVector kopt = solver.solve(d);  // optimal distortion parameter
        // ----------------------------------------------------------------------------------

        // keep errors for later use (optional)
        double err1 = D.operate(new ArrayRealVector(new double[P])).subtract(d).getNorm();
        double err2 = D.operate(kopt).subtract(d).getNorm();
        // System.out.format("err1=%.2f, err2=%.2f \n", err1, err2);

        DistortionModel dist = distModel.from(kopt.toArray()); //, err1 / (pointCount));
        return new Camera(initCam.getMatrixA(), dist);   // TODO: check error quantity is avg)
    }

    private static int getTotalPointCount(Pnt2d[][] modPts) {
        int total = 0;
        for (Pnt2d[] p : modPts) {
            total += p.length;
        }
        return total;
    }


}
