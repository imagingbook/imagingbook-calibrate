package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.Collections;
import java.util.List;

/**
 * Represents a polygon outlining a candidate marker in the input image.
 * Originally this is the raw contour which is subsequently simplified.
 * Instances are immutable.
 */
public class MarkerOutline {
    static int MARKER_UID = -1;
    // TODO: add getter methods!
    public final int uid;          // each marker has a uid for debugging
    public final int threshold;    // gray-level threshold at which this outline was obtained
    public final List<Pnt2d> polygon;  // marker corners in original image coordinates

    // Full constructor.
    MarkerOutline(int uid, int threshold, List<Pnt2d> polygon) {
        this.uid = uid;
        this.threshold = threshold;
        this.polygon = polygon;
    }

    // Constructor, copies an existing outline with a new polygon.
    MarkerOutline(MarkerOutline outline, List<Pnt2d> polygon) {
        this(outline.uid, outline.threshold, polygon);
    }

    static void resetUid() {
        MARKER_UID = -1;
    }

    static int nextUid() {
        MARKER_UID++;
        return MARKER_UID;
    }

    void rotatePolygon(int steps) {
        Collections.rotate(this.polygon, steps);
    }
}
