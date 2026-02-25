/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.Mapping2D;
import imagingbook.common.geometry.mappings.linear.AffineMapping2D;

/**
 * <p>
 * This class represents a special geometric mapping for  rectifying (i.e., removing the lens
 * distortion from) an image, given the associated camera parameters. The transformation maps
 * a position {@code xy} in the rectified (target) image to the corresponding position {@code uv}
 * in the distorted (source) image. The mapping is implicitly inverted, i.e., it maps
 * target (rectified) to source (distorted) image coordinates.
 * </p>
 * <p>
 * Typically usage (by target-to-source-mapping):
 * </p>
 * <pre>{@code
 * ImageProcessor distorted = ... ;    // the distorted image (source)
 * ImageProcessor rectified = ... ;    // the (new) rectified image (target)
 * // ImageMapper requires a target-to-source mapping:
 * ImageMapper mapper = new ImageMapper(new RectificationMapping(camera));
 * mapper.map(source, target);}
 * </pre>
 */
public class RectificationMapping implements Mapping2D {
	private final Camera cam;
	private final DistortionModel distortion;
	private final AffineMapping2D sensorToNormalizedMapping;
	private final AffineMapping2D normalizedToSensorMapping;

	public RectificationMapping (Camera cam) {
		this.cam = cam;
		this.distortion = cam.getDistortion();
		this.sensorToNormalizedMapping = new AffineMapping2D(cam.getAffineMatrixInverse().getData());
		this.normalizedToSensorMapping = sensorToNormalizedMapping.getInverse(); 		// new AffineMapping2D(cam.getAffineMatrix().getData());
	}

	@Override
	public Pnt2d applyTo(Pnt2d XY) {     // (X,Y) is a point in the rectified image
		// (1) apply the inverse linear camera mapping to get the normalized point (x,y):
		Pnt2d xy = sensorToNormalizedMapping.applyTo(XY);
		// (2) apply the forward radial lens distortion in the normalized plane:
		Pnt2d xyd = distortion.warp(xy);
		// (3) apply the (forward) linear camera mapping to get the distorted sensor point (u, v):
		return normalizedToSensorMapping.applyTo(xyd);
	}

}
