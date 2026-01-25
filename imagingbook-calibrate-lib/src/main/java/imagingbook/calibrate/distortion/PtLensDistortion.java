/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.AbstractCamera;
import imagingbook.calibrate.intrinsics.Camera;
import org.apache.commons.math4.legacy.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math4.legacy.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math4.legacy.analysis.solvers.UnivariateDifferentiableSolver;

/**
 * PtLens radial distortion model, as used by PanoTools, Hugin, lensfun etc. Its radial distortion
 * function is
 * <pre>{@code
 * r' = warp(r) = (1 - a - b - c) r + c r^2 + b r^3 + a r^4
 *              = r + r [c (r - 1) + b (r^2 - 1) + a (r^3 - 1)}</pre>
 * The model's parameters {@code a, b, c} are specified for a scaled projection space, whose scale
 * depends on the image size.
 */
public class PtLensDistortion extends RadialDistortion implements ScaledDistortionSpace {

    private final double a, b, c;
    private final double scale;

    @Deprecated
    public static PtLensDistortion from(Camera cam, int imgWidth, int imgHeight) {
        double scale = findScale(cam, imgWidth, imgHeight);
        return new PtLensDistortion(new double[] {0, 0, 0}, scale);
    }

    /**
     * Calculates and returns the scale factor required for the {@code PtLensDistortion} type distortion
     * model. The scale factor {@code s} is calculated such that a circle with radius {@code 1/s} maps to
     * the largest circle that fits the entire sensor image. It therefore depends on the image dimensions
     * {@code W} and {@code H} (whichever is smaller) and the intrinsic camera parameters
     * {@code alpha} and {@code beta} (which define the system's focal length).
     *
     * @param cam a {@code Camera} instance with initialized linear part (affine transform)
     * @param imgWidth image width {@code W}
     * @param imgHeight image height {@code H}
     * @return the scale factor to apply to normalised projection coordinates
     */
    public static double findScale(AbstractCamera cam, int imgWidth, int imgHeight) {
        return Math.max(
                 cam.getAlpha() / (0.5 * imgWidth),
                 cam.getBeta() / (0.5 * imgHeight));
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public PtLensDistortion(double[] parameters, double scale) {
        super((parameters == null) ? new double[3] : parameters);
        double[] params = this.getParameters();
        this.a = params[0];
        this.b = params[1];
        this.c = params[2];
        this.scale = scale;
    }

    @Override
    public PtLensDistortion withParameters(double[]  params) {
        return new PtLensDistortion(params, this.scale);
    }

    @Override
    public double getScale() {
        return this.scale;
    }

    // -------------------------------------------------------------------------

    @Override
    public double fRad(double r0) {
        double rho = scale * r0;
        double rho2 = rho * rho;
        double rho3 = rho2 * rho;
        double D = c * (rho - 1) + b * (rho2 - 1) + a * (rho3 - 1);
        double Rho = rho * (1 + D);
        return Rho / scale;
    }

    @Deprecated // original formulation, for testing only!
    double fRad2(double r) {
        double rho = scale * r;     // convert to scaled space
        double rho2 = rho * rho;
        double rho3 = rho2 * rho;
        double rho4 = rho2 * rho2;
        double Rho = (1 - a - b - c) * rho + c * rho2 + b * rho3 + a * rho4;
        return Rho/ scale;          // convert back to normalized space
    }

    @Deprecated // for testing only!
    double Dpt(double r) {
        double rho = scale * r;
        double r2 = rho * rho;
        double r3 = r2 * rho;
        return c * (rho - 1) + b * (r2 - 1) + a * (r3 - 1);
    }

    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre> (1-a-b-c) r + c r^2 + b r^3 + a r^4 - R = 0,</pre>
     * where R is known and r is unknown. The solution is found by a Newton-Raphson solver.
     * @param R the distorted radius
     * @return r, the undistorted radius
     */
    @Override
    public double fRadInv(double R) {
        double Rho = scale * R;      // convert to scaled space
        double[] coefficients = {-Rho, (1 - a - b - c), c, b, a};
        PolynomialFunction p = new PolynomialFunction(coefficients);
        UnivariateDifferentiableSolver solver = new NewtonRaphsonSolver();
        int maxEval = 20;
        double rho = solver.solve(maxEval, p, Rho); // initial Rho
        return rho / scale;       // convert back to normalized space
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

}
