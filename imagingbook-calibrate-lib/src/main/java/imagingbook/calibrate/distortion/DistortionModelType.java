/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import java.util.function.BiFunction;

/**
 * Factory enum for {@link DistortionModel}. Usage:
 * <pre>{@code
 *     DistortionModelType type = DistortionModelType.Radial2Term;
 *     DistortionModel model = type.getInstance();
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

    // holds each enum's factory instance
    private final BiFunction<Integer, Integer, ? extends DistortionModel> factory;

    // enum constructor
    DistortionModelType(BiFunction<Integer, Integer, ? extends DistortionModel> factory) {
        this.factory = factory;
    }

    /**
     * Creates and returns a new {@link DistortionModel} instance for this
     * {@link DistortionModelType} enum type.
     * Parameters {@code imgWidth}, {@code imgHeight} are only used for some distortion models and
     * ignored for all others.
     * @param imgWidth the image width
     * @param ImgHeight the image height
     * @return a new {@link DistortionModel} instance
     */
    public DistortionModel create(int imgWidth, int ImgHeight) {
        return factory.apply(imgWidth, ImgHeight);
    }
}
