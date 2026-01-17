/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

/**
 * Dummy distortion model that performs no distortion. Mainly for testing.
 */
public class NullDistortion extends DistortionModel {

    public NullDistortion() {
        super(new double[0]);
    }

    @Override
    public DistortionModel fromParameters(double[] params) {
        return new NullDistortion();
    }

    @Override
    double[][] getDMatrixRowsUV(double x, double y, double du, double dv) {
        return new double[2][0];
    }

    @Override
    public double[] warp(double[] xy) {
        return xy;
    }

    @Override
    public double[] unwarp(double[] xyd) {
        return xyd;
    }
}
