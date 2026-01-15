/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Factory enum for {@link DistortionModel}. Usage:
 * <pre>{@code
 * DistortionModelType type = DistortionModelType.Radial2Term;
 * DistortionModel model = type.getInstance();
 * }</pre>
 */
public enum DistortionModelType {
    NullDistortion((w, h)    -> new NullDistortionModel()),
    Radial2Term((w, h)       -> new Radial2TermDistortionModel()),
    Radial3Term((w, h)       -> new Radial3TermDistortionModel()),
    RadialLateral((w, h)     -> new RadialLateralDistortionModel()),
    PtLens((w, h)            -> new PtLensDistortionModel(w, h)),
    Radial2TermScaled((w, h) -> new Radial2TermDistortionModelScaled(w, h)),
    ;

    //private final Supplier<? extends DistortionModel> factory;
    private final BiFunction<Integer, Integer, ? extends DistortionModel> factory;

    // DistortionModelType(Supplier<? extends DistortionModel> factory) {
    //     this.factory = factory;
    // }

    DistortionModelType(BiFunction<Integer, Integer, ? extends DistortionModel> factory) {
        this.factory = factory;
    }

    public DistortionModel getInstance() {
        return factory.apply(-1, -1);
    }

    public DistortionModel getInstance(int imgWidth, int ImgHeight) {
        return factory.apply(imgWidth, ImgHeight);
    }
}
