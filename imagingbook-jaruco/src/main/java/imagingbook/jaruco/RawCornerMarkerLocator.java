package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

import java.util.List;

/**
 * Implementation of {@link MarkerLocator} which simply passes back the raw corner
 * points from the segmented contour, without any refinement.
 * Mainly for testing and debugging.
 */
public class RawCornerMarkerLocator implements MarkerLocator {

    /**
     * Constructor.
     */
    public RawCornerMarkerLocator() {
    }

    @Override
    public Polygon2d getCandidateCorners(SegmentedPolygon poly) {
        return poly.getCornerPolygon();
    }

}
