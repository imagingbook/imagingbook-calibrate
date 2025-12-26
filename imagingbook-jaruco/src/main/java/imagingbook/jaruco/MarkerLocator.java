package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Polygon2d;

/**
 * Responsible for calculating a marker's corner coordinates with more or
 * less precision.
 */
public interface MarkerLocator {

    public enum Type {
        RawCorners {
            @Override
            public MarkerLocator create() {
                return new RawCornersMarkerLocator();
            }},
        StraightLine {
            @Override
            public MarkerLocator create() {
                return new StraightLineMarkerLocator();
            }},
        Parabolic {
            @Override
            public MarkerLocator create() {
                return new ParabolicMarkerLocator();
            }};

        public abstract MarkerLocator create();
    }

    /**
     * Returns the (usually but not necessarily refined) image corner coordinates
     * for the marker candidate as a {@link Polygon2d} instance.
     * @param segmentedPolygon the original, already segmented contour
     * @return the refined corner points in image coordinates
     */
    public Polygon2d getCandidateCorners(SegmentedPolygon segmentedPolygon);

}
