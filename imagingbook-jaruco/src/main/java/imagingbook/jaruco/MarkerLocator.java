package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for calculating a marker's corner coordinates with more or
 * less precision.
 */
public interface MarkerLocator {

    /**
     * Returns the (usually but not necessarily refined) image corner coordinates
     * for the marker, derived from the supplied {@link SegmentedContour} instance.
     * @return the refined corner points in image coordinates
     */
    public List<Pnt2d> getCorners (SegmentedContour segmentedContour);

    public default Polygon2d getCornerPolygon (SegmentedContour segmentedContour) {
        return new Polygon2d(getCorners(segmentedContour));
    }

}
