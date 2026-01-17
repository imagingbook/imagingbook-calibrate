/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.math3legacy.Rotation;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PtLens distortion model, as used by PanoTools, Hugin, lensfun etc.
 */
public class PtLensDistortionModel extends RadialDistortionModel implements ScaledDistortionSpace {

    private final double a, b, c;
    private final double scale;

    @Deprecated
    public static PtLensDistortionModel from(Camera cam, int imgWidth, int imgHeight) {
        double scale = findScale(cam, imgWidth, imgHeight);
        // System.out.println("scale = " + scale);
        return new PtLensDistortionModel(new double[] {0, 0, 0}, scale);
    }

    /**
     * Calculates and returns the scale factor required for the {@code PtLensDistortionModel} type distortion
     * model. The scale factor {@code s} is calculated such that a circle with radius {@code 1/s} maps to
     * the largest circle that fits the entire sensor image. It therefore depends on the image dimensions
     * {@code W} and {@code H} (whichever is smaller) and the intrinsic camera parameters
     * {@code alpha} and {@code beta} (which define the system's focal length).
     *
     * @param cam a {@code Camera} instance with initialized linear part (affine transform)
     * @param imgWidth image width {@code W}
     * @param imgHeight image geight {@code H}
     * @return the scale factor to apply to normalied projection coordinates
     */
    public static double findScale(Camera cam, int imgWidth, int imgHeight) {
//        return 1 / Math.min(
//                imgWidth  / (2 * cam.getAlpha()),
//                imgHeight / (2 * cam.getBeta()));

        return Math.max(
                 cam.getAlpha() / (0.5 * imgWidth),
                 cam.getBeta() / (0.5 * imgHeight));
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public PtLensDistortionModel(double[] parameters, double scale) {
        super((parameters == null) ? new double[3] : parameters);
        double[] params = this.getParameters();
        this.a = params[0];
        this.b = params[1];
        this.c = params[2];
        this.scale = scale;
    }

    @Override
    public PtLensDistortionModel fromParameters(double[]  params) {
        return new PtLensDistortionModel(params, this.scale);
    }

    @Override
    public double getScale() {
        return this.scale;
    }

    // -------------------------------------------------------------------------

    @Override
    public double fRad(double r0) {
        double r = scale * r0;
        double r2 = r * r;
        double r3 = r2 * r;
        double D = c * (r - 1) + b * (r2 - 1) + a * (r3 - 1);
        return r * (1 + D) / scale;
    }

    @Deprecated // for testing only!
    public double fRad2(double r0) {
        double r = scale * r0;
        double r2 = r * r;
        double r3 = r2 * r;
        double r4 = r2 * r2;
        double rr = (1 - a - b - c) * r + c * r2 + b * r3 + a * r4;
        return rr/ scale;
    }

    @Deprecated // for testing only!
    public double Dpt(double r) {
        r = scale * r;
        double r2 = r * r;
        double r3 = r2 * r;
        double D = c * (r - 1) + b * (r2 - 1) + a * (r3 - 1);		// D(r) = k1 * r^2 + k1 * r^4 + k2 * r^6
        return D;
    }

    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre> r + k0 * r^3 + k1 * r^5 + k2 * r^7 - R = 0,</pre>
     * where R is known and r is unknown The solution is found by a Newton-Raphson solver.
     * @param R the distorted radius
     * @return r, the undistorted radius
     */
    @Override
    public double fRadInv(double R) {
        // double[] coefficients = {-R, 1, 0, a, 0, b, 0, c};
        // PolynomialFunction p = new PolynomialFunction(coefficients);
        // UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        // int maxEval = 20;
        // double r = solver.solve(maxEval, p, R); // rInit = R
        // return r;
        throw new UnsupportedOperationException("fRadInv() not supported yet.");
    }

    // -------------------------------------------------------------------------

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        x = scale * x;
        y = scale * y;
        double xx = x * x;
        double yy = y * y;
        double r2 = xx + yy;
        double r = Math.sqrt(r2);
        double r3 = r2 * r;
        return new double[][] {
                {du * (r3 - 1), du * (r2 - 1), du * (r - 1)},
                {dv * (r3 - 1), dv * (r2 - 1), dv * (r - 1)}};
    }

    // -------------------------------------------------------------------------

    // <distortion model="ptlens" focal="40" a="0.0114400833647736" b="-0.0388117252490693" c="0.0340496771870945"/> Viltrox AF 40mm f/2.5
    // <distortion model="ptlens" focal="55" a="0.000016" b="-0.0102041" c="0.0105145"/> // Yashica DSB 55mm f/2
   static void listfRad() {
       double[] abc = {0.01144, -0.0102, 0.01051};     // distortion parameters
       // double[] abc = {0,0,0};
       int W = 640;
       int H = 480;
       // double s = 0.7;
       double alpha = 700; //Math.hypot(0.5 * W, 0.5 * H);
       double beta = alpha;
       System.out.println("alpha = " + alpha);
       ViewTransform view = new ViewTransform(Rotation.IDENTITY, new double[]{0, 0, 1});

       Camera cam0 = new Camera(new double[] { alpha, beta, 0, 0.5 * W, 0.5 * H }, null);
       double scale = findScale(cam0, W, H);
       // scale = 1 / scale;
       System.out.println("scale = " + scale);
       PtLensDistortionModel dist = new PtLensDistortionModel(abc, scale);
       // System.out.println("scale = " + dist.getScale());

       Camera cam1 = new Camera(cam0.getLinearParameters(), dist);

       // points xy in normalized projection space
       for (Pnt2d xy : Arrays.asList(Pnt2d.from(0, 1/scale), Pnt2d.from(0, -1/scale))) {
           // r = 1/s is a point on the model's fixed circle
           //Pnt2d XY = Pnt2d.from(0, 1/scale);
           System.out.println("\nxy (normalized) = " + xy);

           double[] xyd = cam1.getDistortion().warp(xy.toDoubleArray());
           System.out.println("  xy (warped) = " + Matrix.toString(xyd));

           // should project to (W/2, H)
           double[] uv = cam1.mapToSensorPlane(xyd);
           System.out.println("  uv (sensor) = " + Matrix.toString(uv));
       }

       // for (Pnt2d XY : Arrays.asList(Pnt2d.from(0, 1/scale), Pnt2d.from(0, -1/scale))) {
       //     // r = 1/s is a point on the model's fixed circle
       //     //Pnt2d XY = Pnt2d.from(0, 1/scale);
       //     System.out.println("\nXY (3D) = " + XY);
       //
       //     double[] xy = cam1.projectNormalized(view, XY);
       //     System.out.println("  xy (normalized) = " + Matrix.toString(xy));
       //
       //     // should project to (W/2, H)
       //     double[] uv = cam1.project(view, XY);
       //     System.out.println("  uv (sensor) = " + Matrix.toString(uv));
       // }

   }

    static Pnt2d[] makeModelPoints() {
        int n = 20;
        List<Pnt2d> points = new ArrayList<Pnt2d>();
        for (int i = 0; i <= n; i++) {
            double r = i * 1.0 / n;
            double xy = Math.sqrt(0.5 * r);
            points.add(Pnt2d.from(r, 0));
            points.add(Pnt2d.from(-r, 0));
            points.add(Pnt2d.from(0, r));
            points.add(Pnt2d.from(0, -r));
            points.add(Pnt2d.from(xy, xy));
            points.add(Pnt2d.from(-xy, xy));
            points.add(Pnt2d.from(xy, -xy));
            points.add(Pnt2d.from(-xy, -xy));
        }
        return points.toArray(new Pnt2d[0]);
    }


     static void doDistortionCalibration() {
         int W = 640;
         int H = 480;
         double s = 0.7;
         double alpha = Math.hypot(0.5 * W, 0.5 * H);
         double beta = alpha;
         System.out.println("alpha = " + alpha);

         // real distortion model
         PtLensDistortionModel realDist = new PtLensDistortionModel(new double[] {0.01144, -0.0102, 0.01051}, s);

         List<Pnt2d[]> modPntList = new ArrayList<>();
         List<Pnt2d[]> imgPntList = new ArrayList<>();
         Pnt2d[] modelPoints = makeModelPoints();
         modPntList.add(modelPoints);

         ViewTransform view = new ViewTransform(Rotation.IDENTITY, new double[]{0, 0, 1});
         Camera realCam = new Camera(new double[] { alpha, beta, 0, 0, 0 }, realDist);
         System.out.println("realCam = " + realCam);

         System.out.println("view = " + view);
         System.out.println("realCam = " + realCam);
         System.out.println("N = " + modelPoints.length);


         Pnt2d[] imgPnts = new Pnt2d[modelPoints.length];
         for (int i = 0; i < modelPoints.length; i++) {
             Pnt2d XY = modelPoints[i];
             Pnt2d xy = Pnt2d.from(realCam.projectNormalized(view, XY));
             Pnt2d uv = Pnt2d.from(realCam.project(view, XY));
             // System.out.printf("%s -> %s -> %s\n", XY, xy, uv);
             imgPnts[i] = uv;
         }
         imgPntList.add(imgPnts);

         Camera initCam = new Camera(new double[] { alpha, beta, 0, 0, 0 }, null);
         //DistortionModel dist = PtLensDistortionModel.from(initCam, 640, 480);
         DistortionModel dist = DistortionModelType.PtLens.create(initCam, 640, 480);
         initCam.setDistortion(dist);
         DistortionEstimator estimtr = new DistortionEstimator(initCam);
         Camera camImproved = estimtr.getEstimate(List.of(view), modPntList, imgPntList);
         PrintPrecision.set(8);
         System.out.println("camImproved = " + camImproved.getDistortion());
     }

    public static void main(String[] args) {
        listfRad();
        // doDistortionCalibration();

    }

}
