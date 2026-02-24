package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;

/**
 * Implementation of {@link MarkerLocator} which simply passes back the raw corner
 * points from the segmented contour, without any refinement.
 * Mainly for testing and debugging.
 */
public class RawCornersMarkerLocator implements MarkerLocator {

    /**
     * Constructor.
     * @param params parameters (ignored)
     */
    public RawCornersMarkerLocator(ArucoMarkerDetector.Parameters params) {
    }

    @Override
    public Pnt2d[] getMarkerCorners(SegmentedPolygon poly) {
        return poly.getCorners();
    }

}
