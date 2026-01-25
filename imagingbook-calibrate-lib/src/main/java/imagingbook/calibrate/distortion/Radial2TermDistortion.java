/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.calibrate.zhang.data.ZhangData;
import org.apache.commons.math4.legacy.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math4.legacy.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math4.legacy.analysis.solvers.UnivariateDifferentiableSolver;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.Random;

/**
 * Basic radial distortion model used in Zhang's EasyCalib implementation with two
 * {@code parameters = (k0, k1)}.
 * Distortion is modeled by function
 * <pre>{@code
 * r' = warp(r) = r * (1 + k0 * r^2 + k1 * r^4)}
 *              = r + k0 * r^3 + k1 * r^5
 * </pre>
 */
public class Radial2TermDistortion extends RadialDistortion {

    private final double k0;
    private final double k1;

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public Radial2TermDistortion() {
        this(new double[] {0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public Radial2TermDistortion(double[]  parameters) {
        super(parameters);
        if (parameters.length != 2) {
            throw new IllegalArgumentException("expected 2 parameters but received " +
                    parameters.length);
        }
        this.k0 = parameters[0];
        this.k1 = parameters[1];
    }

    // ----------------------------------------

    @Override
    public Radial2TermDistortion withParameters(double[]  params) {
        return (params == null) ?
                new Radial2TermDistortion() :
                new Radial2TermDistortion(params);
    }

//    @Override
//    public Radial2TermDistortion getScaled(double s) {
//        double k0_ = k0 * Math.pow(s, 1-3); // k0 = a3
//        double k1_ = k1 * Math.pow(s, 1-5); // k1 = a5
//        return this.fromParameters(new double[] {k0_, k1_});
//    }

    // -----------------------------------------

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        double xx = x * x;
        double yy = y * y;
        double r2 = xx + yy;
        double r4 = r2 * r2;
        return new double[][] {
                {du * r2, du * r4},
                {dv * r2, dv * r4}};
    }

    // -----------------------------------------

    /**
     * Forward radial distortion function.
     * @param r the original radius of a point in the ideal projection plane
     * @return the distorted radius
     */
    @Override
    public double fRad(final double r) {
        double r2 = r * r;
        double D = r2 * (k0 + k1 * r2);		// D(r) = k0 * r^2 + k1 * r^4
        return r * (1 + D);
    }

    /**
     * Inverse radial distortion function. Finds the original (undistorted) radius r from the distorted radius R, both
     * measured from the center = (0,0) of the ideal projection. Finds r as the root of the polynomial
     * <pre>p(r) = -R + r + k0 * r^3 + k1 * r^5,</pre>
     * where R is given and fixed, by using a Newton-Raphson solver.
     *
     * @param R the distorted radius
     * @return the undistorted radius
     */
    @Override
    public double fRadInv(final double R) {
        double k0 = parameters[0];
        double k1 = parameters[1];
        double[] coefficients = {-R, 1, 0, k0, 0, k1};
        UnivariateDifferentiableSolver inverseSolver = new NewtonRaphsonSolver();
        PolynomialFunction p = new PolynomialFunction(coefficients);
        int maxEval = 20;
        double r = inverseSolver.solve(maxEval, p, R); // rInit = R
        // System.out.format("** solver iterations = %d\n", solver.getEvaluations());
        return r;
    }

    // ------------------------------------------------------------------------

    /**
     * Find an approximate inverse function for
     *  fRad(r) = r' =r * (1 + k0 * r^2 + k1 * r^4)
     *  using the same form of polynomial
     *  fRadInv(r') = r = r' * (1 + q0 * r'^2 + q1 * r'^4)
     * @return
     */
    public double[] estimateInverseFunction2() {
        int N = 1000;   // number of samples
        Random rand = new Random();
        RealMatrix A = MatrixUtils.createRealMatrix(N, 2);
        RealVector d = new ArrayRealVector(N);
        for (int i = 0; i < N; i++) {
            double r = rand.nextDouble();
            double rr = fRad(r);
            double rr3 = rr * rr * rr;
            double rr5 = rr3 * rr * rr;
            A.setEntry(i, 0, rr3);
            A.setEntry(i, 1, rr5);
            d.setEntry(i, r - rr);
        }

        DecompositionSolver solver = new QRDecomposition(A).getSolver();
        RealVector q = solver.solve(d);

        RealVector residual = d.subtract(A.operate(q));
        double maxres = residual.getMaxValue();
        double avgres = residual.getNorm() / N;
        System.out.println("max residual = " + maxres);
        System.out.println("avg residual = " + avgres);
        return q.toArray();
    }

    /**
     * Just an experiment. Find coefficients for a polynomial
     * to model the inverse function.
     * @return
     */
    public double[] estimateInverseFunction3() {
        int N = 1000;   // number of samples
        Random rand = new Random();
        RealMatrix A = MatrixUtils.createRealMatrix(N, 3);
        RealVector d = new ArrayRealVector(N);
        for (int i = 0; i < N; i++) {
            double r = rand.nextDouble();
            double rr = fRad(r);
            double rr3 = rr * rr * rr;
            double rr5 = rr3 * rr * rr;
            double rr7 = rr5 * rr * rr;
            A.setEntry(i, 0, rr3);
            A.setEntry(i, 1, rr5);
            A.setEntry(i, 2, rr7);
            d.setEntry(i, r - rr);
        }

        DecompositionSolver solver = new QRDecomposition(A).getSolver();
        RealVector q = solver.solve(d);

        RealVector residual = d.subtract(A.operate(q));
        double maxres = residual.getMaxValue();
        double avgres = residual.getNorm() / N;
        double rmserr = Math.sqrt(avgres);
        System.out.println("max residual = " + maxres);
        System.out.println("avg residual = " + avgres);
        System.out.println("rms residual = " + rmserr);
        return q.toArray();
    }

    public static void main(String[] args) {
        double r = 0.9;
        double s = 0.7;
        System.out.println("r = " + r);
        System.out.println("s = " + s);
        System.out.println("s r = " + s * r);
        StandardCamera cam1 = ZhangData.getCamera();
        Radial2TermDistortion distortion = (Radial2TermDistortion) cam1.getDistortion();
        double[] params1 = cam1.getDistortion().getParameters();
        System.out.println("params1 = " + Arrays.toString(params1));
        double rr = distortion.fRad(r);
        System.out.format("cam1: %.5f -> %.5f\n", r, s * distortion.fRad(r));

        double a1 = 1;
        double a2 = 0;
        double a3 = params1[0];
        double a4 = 0;
        double a5 = params1[1];
        double[] a = {a1, a2, a3, a4, a5};
        //System.out.format("cam2: %.5f -> %.5f\n", r, fRadScaled(a, s, r));
        // System.out.format("CHECK %.5f vs %.5f\n", s * distortion.fRad(r), fRadScaled(a, s, s * r));

        double aa3 = a3 * Math.pow(s, 1-3);
        double aa5 = a5 * Math.pow(s, 1-5);
        Radial2TermDistortion distortion2 =
                distortion.withParameters(new double[] {aa3, aa5});
        System.out.println("params2 = " + Arrays.toString(distortion2.getParameters()));
        System.out.format("cam2: %.5f -> %.5f\n", s * r, distortion2.fRad(s * r));
    }


    static double fRadScaled(double[] a, double s, double r) {
        double[] as = new double[a.length];
        for (int i = 1; i <= a.length; i++) {
            double ai = a[i-1];
            as[i-1] = ai * Math.pow(s, 1-i);
        }
        System.out.println("as = " + Arrays.toString(as));
        double sum = 0;
        for (int i = 1; i <= a.length; i++) {
            sum += as[i-1] * Math.pow(s * r, i);
        }
        return sum;
    }
}
