package imagingbook.jaruco.marker;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.jaruco.cornerdata.Pnt2dOrderedSet;
import imagingbook.jaruco.util.JsonResource;

/**
 * Image contours and corners of single markers under rotations as point sequences (for testing)
 * Note: Contours are too big to be defined in Java code, therefore external JSON files.
 */
public enum MarkerTestContours  implements JsonResource, Pnt2dOrderedSet {
    single_marker_5_0_contour,
    single_marker_5_0_corners,

    single_marker_5_1_contour,
    single_marker_5_1_corners,

    single_marker_5_2_contour,
    single_marker_5_2_corners,

    single_marker_5_3_contour,
    single_marker_5_3_corners
    ;

    @Override
    public Class<?> getResourceClass() {
        return double[][].class;
    }

    @Override
    public Pnt2d[] getPoints() {
        double[][] pts = this.readObject();
        return PntUtils.fromDoubleArray(pts);
    }
}
