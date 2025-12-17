package imagingbook.jaruco.obsolete;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.Collections;
import java.util.List;

/**
 * Represents a raw polygon outlining a potential candidate marker in the input
 * image. Instances have a unique ID for debugging and record the image
 * threshold value used to extract the contour points.
 */
@Deprecated
public class MarkerOutline {
    static int MARKER_UID = -1;
    // TODO: add getter methods!
    public final int uid;          // each marker has a uid for debugging
    public final int threshold;    // gray-level threshold at which this outline was obtained
    public final List<Pnt2d> polygon;  // marker corners in original image coordinates

    // Full constructor.
    public MarkerOutline(int uid, int threshold, List<Pnt2d> polygon) {
        this.uid = uid;
        this.threshold = threshold;
        this.polygon = polygon;
    }

    // Constructor, copies an existing outline with a new polygon.
    @Deprecated
    public MarkerOutline(MarkerOutline outline, List<Pnt2d> polygon) {
        this(outline.uid, outline.threshold, polygon);
    }

    // ----------------------------------------------------------------------

    public int length() {
        return polygon.size();
    }

    public List<Pnt2d> getPolygon() {
        return polygon;
    }

    public static void resetUid() {
        MARKER_UID = -1;
    }

    public static int nextUid() {
        MARKER_UID++;
        return MARKER_UID;
    }

    // ----------------------------------------------------------------------

    public void rotatePolygon(int steps) {
        Collections.rotate(this.polygon, steps);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("% --------------------------------------------------\n");
        sb.append("double[][] contourPoints = {\n");
        for (Pnt2d pt : polygon) {
            double x = pt.getX();
            double y = pt.getY();
            sb.append(String.format("{%.1f, %.1f},\n", x, y));
        }
        sb.append("};\n");
        sb.append("% --------------------------------------------------\n");
        return sb.toString();
    }
}
