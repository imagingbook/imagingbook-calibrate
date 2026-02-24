package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines helper methods associated with marker corners.
 */
public final class Corners {

    private Corners() { }

    /**
     * Reverses the order of points but keeps the first point in place.
     * That is, if the initial points were (0, 1, 2, ..., n-1) the points of the reversed
     * array are (0, n-1, ..., 2, 1).
     * @param points the original point array
     * @return the reversed point array
     */
    public static Pnt2d[] reverse(Pnt2d[] points) {
        // Pnt2d[] copy = points.clone();
        // for (int i = 1; i < points.length; i++) {
        //     copy[i] = points[points.length - i];
        // }
        // return copy;
        return new Polygon2d(points).reverse().getPnts();
    }

    /**
     * Cyclically rotates an array of points by the specified distance.
     * Uses {@link Collections#rotate(List, int)}.
     * @param points the original point array
     * @param distance the
     * @return the rotated point array
     */
    public static Pnt2d[] rotate(Pnt2d[] points, int distance) {
        // Pnt2d[] copy = points.clone();
        // Collections.rotate(Arrays.asList(copy), distance);  // this modifies the underlying array!
        // return copy;
        return new Polygon2d(points).rotate(distance).getPnts();
    }
}
