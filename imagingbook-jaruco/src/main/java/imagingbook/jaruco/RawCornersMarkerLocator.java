package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Polygon2d;

/**
 * Implementation of {@link MarkerLocator} which simply passes back the raw corner
 * points from the segmented contour, without any refinement.
 * Mainly for testing and debugging.
 */
public class RawCornersMarkerLocator implements MarkerLocator {

    /**
     * Constructor.
     */
    public RawCornersMarkerLocator() {
    }

    @Override
    public Polygon2d getCandidateCorners(SegmentedPolygon poly) {
        return poly.getCornerPolygon();
    }

}
