/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.distortion;

public class RadialLateralDistortion implements LensDistortion {

    public static final int PARAM_COUNT = 5;
    public static final RadialLateralDistortion INSTANCE = new RadialLateralDistortion();
    private final double k0, k1, k2, p1, p2;
    private final double error; // estimation error

    /**
     * Blank constructor. Creates a lens distortion instance with zero parameters.
     */
    public RadialLateralDistortion() {
        this(new double[] {0, 0});
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     */
    public RadialLateralDistortion(double[] parameters) {
        this(parameters, 0.0);
    }

    /**
     * Constructor. Creates a lens distortion instance with the specified parameters.
     * @param parameters vector of distortion parameters
     * @param error average estimation error
     */
    public RadialLateralDistortion(double[] parameters, double error) {
        if (parameters.length != PARAM_COUNT)
            throw new IllegalArgumentException("wrong parameter count: " + parameters.length);
        this.k0 = parameters[0];
        this.k1 = parameters[1];
        this.k2 = parameters[2];
        this.p1 = parameters[3];
        this.p2 = parameters[4];
        this.error = error;
    }

    // ------------------------------------------------------------------------

    @Override
    public LensDistortion copyOf(double[] params, double error) {
        return new RadialLateralDistortion(params, error);
    }

    @Override
    public double[] getParameters() {
        return new double[] {k0, k1, k2, p1, p2};
    }

    @Override
    public double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        final double xx = x * x;
        final double yy = y * y;
        final double xy = x * y;
        final double r2 = xx + yy;
        final double r4 = r2 * r2;
        final double r6 = r2 * r4;
        return new double[][] {
                {du * r2, du * r4, du * r6, 2 * xy, r2 + 2 * xx},
                {dv * r2, dv * r4, dv * r6, r2 + 2 * yy, 2 * xy}};
    }
    // -------------------------------------------------------------------------

    @Override
    public double[] warp(double[] xy) {
        return new double[0];
    }

    @Override
    public double[] unwarp(double[] xyd) {
        return new double[0];
    }
}
