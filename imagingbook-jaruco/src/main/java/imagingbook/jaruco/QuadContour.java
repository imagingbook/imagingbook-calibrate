package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * Represents a closed, convex 4-corner polygon extracted from a sequence of image
 * points. The original (full set) of contour points is preserved for quad
 * fitting.
 */
public class QuadContour {
    private final int N;
    private final Pnt2d[] fullContour;
    private final int[] cornerIndexes;
    private final Pnt2d[][] segments;

    /**
     * Constructor.
     *
     * @param cornerIdxs the indexes of the corners in {@code contourPoints}
     * @param fullContour       the original sequence of contour points
     */
    public QuadContour(List<Integer> cornerIdxs, List<Pnt2d> fullContour) {
        if (cornerIdxs.size() != 4) {
            throw new IllegalArgumentException("quad does not have four corners: "
                    + cornerIdxs.size());
        }
        this.fullContour = fullContour.toArray(new Pnt2d[0]);
        this.N = this.fullContour.length;
        this.cornerIndexes = new int[cornerIdxs.size()];
        for (int i = 0; i < cornerIndexes.length; i++) {
            cornerIndexes[i] = cornerIdxs.get(i);
            // check if corner index is within the associated boundary:
            if (cornerIndexes[i] < 0 || cornerIndexes[i] >= N) {
                throw new IllegalArgumentException("out-of-bounds corner index: "
                        + cornerIndexes[i]);
            }
        }
        // initialize quad segments (sets of intermediate points, not including the corners)
        segments = new Pnt2d[4][];
        int cIdx0 = cornerIndexes[0];
        int cIdx1 = cornerIndexes[1];
        int cIdx2 = cornerIndexes[2];
        int cIdx3 = cornerIndexes[3];
        segments[0] = fullContour.subList(cIdx0 + 1, cIdx1).toArray(new Pnt2d[0]);
        segments[1] = fullContour.subList(cIdx1 + 1, cIdx2).toArray(new Pnt2d[0]);
        segments[2] = fullContour.subList(cIdx2 + 1, cIdx3).toArray(new Pnt2d[0]);
        segments[3] = fullContour.subList(cIdx3 + 1, N).toArray(new Pnt2d[0]);
    }

    // -----------------------------------------------------------------------------------

    /**
     * Returns a sequence of all points on the original contour,
     * which starts with the first corner.
     * @return the full contour
     */
    public Pnt2d[] getContour() {
        return fullContour;
    }

    /**
     * Returns the point with index {@code i} on the full contour,
     * which starts with the first corner.
     * @param i the point index.
     * @return the referenced point
     */
    public Pnt2d getContourPoint(int i) {
        return fullContour[i];
    }

    /**
     * Returns the {@code k}th corner point (of 4 corner points).
     * @param k the corner point index (0,...,3).
     * @return the referenced corner point
     */
    public Pnt2d getCorner(int k) {
        return fullContour[cornerIndexes[k]];
    }

    /**
     * Returns the points inside the specified quad segment, corner
     * points not included. Segment 0 contains the points between
     * corners 0 and 1, etc.
     * @param k the segment index (0,...,3)
     * @return an array of segment points
     */
    public Pnt2d[] getSegmentPoints(int k) {
        return segments[k];
    }

    /**
     * Returns the normalized position of a point's projection
     * onto the specified line segment with starting point A
     * and endpoint B. The resulting value t is in [0,1],
     * increasing as the projection moves from point A to
     * point B.
     * @param segIdx the segment index (0,...,3)
     * @param pntIdx the point index within the segment
     * @return the relative projection position
     */
    public double getProjectionPosition(int segIdx, int pntIdx) { // return value t_i
        return 0;   // TODO
    }

}
