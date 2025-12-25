package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;


/**
 * Processes raw contours (lists of contour points) and tries to extract
 * a proper quad from each.
 */
public class ContourSegmenter {

    public static final double DefaultAccuracyRate = 0.03;   //= detectorParams.polygonalApproxAccuracyRate;

    private final double accuracyRate;

    // Processing parameters to be added
    public ContourSegmenter() {
        this(DefaultAccuracyRate);
    }

    public ContourSegmenter(double accuracyRate) {
        this.accuracyRate = accuracyRate;
    }

    @Deprecated
    public SegmentedPolygon segment(List<Pnt2d> contour) {
        return segment(new Polygon2d(contour));
    }

    public SegmentedPolygon segment(Polygon2d contour) {
        final int n = contour.length();
        if (n < 4) {
            throw new IllegalArgumentException("cannot segment contour with less than 4 points");
        }
        // Adapt tolerance to contour size
        final double tol = n * accuracyRate; // parameters!!
        // Pick optimal starting index
        int startPt = getMostEccentricVertexIndex(contour);
        // Rotate polygon such that most eccentric point comes first:
        Polygon2d rotatedPoly = new Polygon2d(contour).rotate(-startPt);
        List<Integer> cornerIndexes = rotatedPoly.getSimplifiedCorners(tol);
        return new SegmentedPolygon(cornerIndexes, rotatedPoly);
    }

    int getMostEccentricVertexIndex(Polygon2d poly) {
        Pnt2d ctr = poly.getCentroid();
        double cx = ctr.getX();
        double cy = ctr.getY();
        int maxIdx = 0;
        double maxD2 = -1;
        int n = poly.length();
        for (int i = 0; i < n; i++) {
            Pnt2d pi = poly.getPnt(i);
            double d2 = sqr(pi.getX() - cx) + sqr(pi.getY() - cy);
            if (d2 > maxD2) {
                maxD2 = d2;
                maxIdx = i;
            }
        }
        return maxIdx;
    }

}
