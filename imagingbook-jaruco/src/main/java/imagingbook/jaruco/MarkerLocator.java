package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

import java.util.List;

/**
 * Responsible for calculating a marker's corner coordinates with more or
 * less precision.
 */
public interface MarkerLocator {

    /**
     * Returns the (usually but not necessarily refined) image corner coordinates
     * for the marker, derived from the supplied {@link SegmentedPolygon} instance.
     * @return the refined corner points in image coordinates
     */
    public List<Pnt2d> getCorners (SegmentedPolygon segmentedPolygon);

    public default Polygon2d getCornerPolygon (SegmentedPolygon segmentedPolygon) {
        return new Polygon2d(getCorners(segmentedPolygon));
    }

}
