package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * Implementation of {@link MarkerLocator} which simply passes back the raw corner
 * points from the segmented contour, without any refinement.
 * Mainly for testing and debugging.
 */
public class SimpleMarkerLocator implements MarkerLocator {

    /**
     * Constructor.
     */
    public SimpleMarkerLocator() {
    }

    @Override
    public List<Pnt2d> getCorners (SegmentedPolygon poly) {
        return poly.getCorners();
    }

}
