package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a closed polygon extracted from a sequence of image
 * points. The original (full set) of contour points is split into K segments,
 * each segment starting with a corner point. This structure is subsequently
 * used for homography fitting.
 */
public class SegmentedPolygon extends Polygon2d {

    // private final int N;    // length of the full contour
    private final Pnt2d[][] segments;

    public SegmentedPolygon(Polygon2d poly, List<Integer> cornerIdxs) {
        super(poly);
        //this(poly.getPnts(), cornerIdxs);
        List<Pnt2d> allPoints = this.getPnts();
        // this.N = allPoints.size();
        int N = this.length();
        if (cornerIdxs.get(0) != 0) {
            throw new IllegalArgumentException("first corner index must be 0 but is " +
                    cornerIdxs.get(0));
        }
        // check if each corner index is inside fullContour:
        for (int cIdx : cornerIdxs) {
            if (cIdx < 0 || cIdx >= N) {
                throw new IllegalArgumentException("out-of-bounds corner index: " + cIdx);
            }
        }
        // initialize quad segments (sets of segment points, corner is first)
        int[] cIdx = new int[cornerIdxs.size() + 1];
        for (int i = 0; i < cornerIdxs.size(); i++) {
            cIdx[i] = cornerIdxs.get(i);
        }
        cIdx[cornerIdxs.size()] = N;    // additional corner index for last segment
        segments = new Pnt2d[cornerIdxs.size()][];
        for (int i = 0; i < cornerIdxs.size(); i++) {
            segments[i] = allPoints.subList(cIdx[i], cIdx[i+1]).toArray(new Pnt2d[0]);
        }
    }

    // public SegmentedPolygon(Polygon2d poly, List<Integer> cornerIdxs) {
    //     //this(poly.getPnts(), cornerIdxs);
    //     List<Pnt2d> allPoints = poly.getPnts();
    //     this.N = allPoints.size();
    //     if (cornerIdxs.get(0) != 0) {
    //         throw new IllegalArgumentException("first corner index must be 0 but is " +
    //                 cornerIdxs.get(0));
    //     }
    //     // check if each corner index is inside fullContour:
    //     for (int cIdx : cornerIdxs) {
    //         if (cIdx < 0 || cIdx >= N) {
    //             throw new IllegalArgumentException("out-of-bounds corner index: " + cIdx);
    //         }
    //     }
    //     // initialize quad segments (sets of segment points, corner is first)
    //     int[] cIdx = new int[cornerIdxs.size() + 1];
    //     for (int i = 0; i < cornerIdxs.size(); i++) {
    //         cIdx[i] = cornerIdxs.get(i);
    //     }
    //     cIdx[cornerIdxs.size()] = N;    // additional corner index for last segment
    //     segments = new Pnt2d[cornerIdxs.size()][];
    //     for (int i = 0; i < cornerIdxs.size(); i++) {
    //         segments[i] = allPoints.subList(cIdx[i], cIdx[i+1]).toArray(new Pnt2d[0]);
    //     }
    // }

    // /**
    //  * Constructor.
    //  *
    //  * @param allPoints the original sequence of contour points
    //  * @param cornerIdxs the indexes of the corners in {@code contourPoints}
    //  */
    // public SegmentedPolygon(List<Pnt2d> allPoints, List<Integer> cornerIdxs) {
    //     this.N = allPoints.size();
    //     if (cornerIdxs.get(0) != 0) {
    //         throw new IllegalArgumentException("first corner index must be 0 but is " +
    //                 cornerIdxs.get(0));
    //     }
    //     // check if each corner index is inside fullContour:
    //     for (int cIdx : cornerIdxs) {
    //         if (cIdx < 0 || cIdx >= N) {
    //             throw new IllegalArgumentException("out-of-bounds corner index: " + cIdx);
    //         }
    //     }
    //     // initialize quad segments (sets of segment points, corner is first)
    //     int[] cIdx = new int[cornerIdxs.size() + 1];
    //     for (int i = 0; i < cornerIdxs.size(); i++) {
    //         cIdx[i] = cornerIdxs.get(i);
    //     }
    //     cIdx[cornerIdxs.size()] = N;    // additional corner index for last segment
    //     segments = new Pnt2d[cornerIdxs.size()][];
    //     for (int i = 0; i < cornerIdxs.size(); i++) {
    //         segments[i] = allPoints.subList(cIdx[i], cIdx[i+1]).toArray(new Pnt2d[0]);
    //     }
    // }

    // -----------------------------------------------------------------------------------

    // public int length() {
    //     return N;
    // }

    /**
     * Returns the {@code k}th corner point (of 4 corner points).
     * @param k the corner point index (0,...,3).
     * @return the referenced corner point
     */
    public Pnt2d getCorner(int k) {
        return segments[k][0];
    }

    @Deprecated
    public List<Pnt2d> getCorners() {
        List<Pnt2d> cornerList = new ArrayList<>(segments.length);
        for (int k = 0; k < segments.length; k++) {
            cornerList.add(segments[k][0]);
        }
        return cornerList;
    }

    public Polygon2d getCornerPolygon() {
        List<Pnt2d> corners = new ArrayList<>(segments.length);
        for (int k = 0; k < segments.length; k++) {
            corners.add(segments[k][0]);
        }
        return new Polygon2d(corners);
    }

    /**
     * Returns the points inside the specified quad segment, corner
     * points not included. Segment 0 contains the points between
     * corners 0 and 1, etc.
     * @param k the segment index (0,...,3)
     * @return an array of segment points
     */
    public Pnt2d[] getSegment(int k) {
        return segments[k];
    }

    public int getSegmentCount() {
        return segments.length;
    }

}
