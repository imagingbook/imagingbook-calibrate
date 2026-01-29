/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.Camera;

import java.util.Objects;
import java.util.function.Function;

/**
 * Factory enum for {@link DistortionModel}. Usage:
 * <pre>{@code
 *     DistortionModelType type = DistortionModelType.Radial2Term;
 *     DistortionModel model = type.getInstance();
 * }</pre>
 */
public enum DistortionModelType {
    Radial2Term((camera, w, h)    -> new Radial2TermDistortion()),
    Radial3Term((camera, w, h)    -> new Radial3TermDistortion()),
    RadialLateral((camera, w, h)  -> new RadialLateralDistortion()),
    PtLens((camera, w, h)         -> new PtLensDistortion(camera)),
    ;

    // holds each enum's factory instance
    // private final BiFunction<Integer, Integer, ? extends DistortionModel> factory;
    private final TriFunction<Camera, Integer, Integer, ? extends DistortionModel> factory;

    // enum constructor
//    DistortionModelType(BiFunction<Integer, Integer, ? extends DistortionModel> factory) {
//        this.factory = factory;
//    }

    DistortionModelType(TriFunction<Camera, Integer, Integer, ? extends DistortionModel> factory) {
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
    public DistortionModel create(Camera cam, int imgWidth, int ImgHeight) {
        return factory.apply(cam, imgWidth, ImgHeight);
    }


    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {

        R apply(T t, U u, V v);

        default <K> TriFunction<T, U, V, K> andThen(Function<? super R, ? extends K> after) {
            Objects.requireNonNull(after);
            return (T t, U u, V v) -> after.apply(apply(t, u, v));
        }
    }
}
