/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import java.util.function.Supplier;

/**
 * Factory enum for {@link DistortionModel}. Usage:
 * <pre>{@code
 * DistortionModelType type = DistortionModelType.Radial2Term;
 * DistortionModel model = type.getInstance();
 * }</pre>
 */
public enum DistortionModelType {
    Radial2Term(Radial2TermDistortionModel::new),
    Radial3Term(Radial3TermDistortionModel::new),
    RadialLateral(RadialLateralDistortionModel::new),
    // RadialLateral(() -> new RadialLateralDistortionModel(10, 15))    // if more parameters required
    ;

    private final Supplier<? extends DistortionModel> factory;

    DistortionModelType(Supplier<? extends DistortionModel> factory) {
        this.factory = factory;
    }

    public DistortionModel getInstance() {
        return factory.get();
    }
}
