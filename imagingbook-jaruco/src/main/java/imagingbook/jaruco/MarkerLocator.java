package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Polygon2d;

/**
 * Responsible for calculating a marker's corner coordinates with more or
 * less precision.
 */
public interface MarkerLocator {

    public enum Type {
        RawCorners,
        StraightFit,
        ParabolicFit
    }

    /**
     * Returns a new instance of the specified {@link MarkerLocator} type, which is extracted
     * from the passed parameters.
     * @param params a parameter bundle associated with the calling {@link ArucoMarkerDetector} instance
     * @return a new {@link MarkerLocator}
     */
    public static MarkerLocator createFrom(ArucoMarkerDetector.Parameters params) {
        return switch (params.locatorType) {
            case RawCorners -> new RawCornersMarkerLocator(params);
            case StraightFit -> new StraightMarkerLocator(params);
            case ParabolicFit -> new ParabolicMarkerLocator(params);
        };
    }

    /**
     * Returns the (usually but not necessarily refined) image corner coordinates
     * for the marker candidate as a {@link Polygon2d} instance.
     * @param segmentedPolygon the original, already segmented contour (see {@link SegmentedPolygon})
     * @return the refined corner points in image coordinates
     */
    public Polygon2d getMarkerCorners(SegmentedPolygon segmentedPolygon);

}
