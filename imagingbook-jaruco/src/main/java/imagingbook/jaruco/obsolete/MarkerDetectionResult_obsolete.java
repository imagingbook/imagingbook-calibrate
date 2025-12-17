package imagingbook.jaruco.obsolete;

import imagingbook.common.geometry.basic.Pnt2d;

/**
 * Represents the detection result for a single marker.
 */
@Deprecated
public record MarkerDetectionResult_obsolete(
        int markerId,
        int rotation,
        int hDist,
        MarkerOutline corners,
        Pnt2d[] rejectedPoints) {
}

