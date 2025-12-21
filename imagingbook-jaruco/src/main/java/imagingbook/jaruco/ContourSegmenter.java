package imagingbook.jaruco;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.obsolete.MarkerOutline;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.getMostEccentricVertexIndex;
import static imagingbook.jaruco.util.Polygons.perpDistSq;

/**
 * Processes raw contours (lists of contour points) and tries to extract
 * a proper quad from each.
 */
public class ContourSegmenter {

    private int minContourLength = 50;
    private double polygonalApproxAccuracyRate = 0.03;   //detectorParams.polygonalApproxAccuracyRate;
    private double minCircularity = 0.5;

    // Processing parameters to be added
    public ContourSegmenter() {

    }

    public SegmentedContour segment(List<Pnt2d> contour) {
        final int n = contour.size();
        if (n <= 3) {
            // return new ArrayList<>(pts); // TODO: to be fixed!
            throw new IllegalArgumentException("SegmentedContour() not ready for n<4 points yet");
        }
        final double tol = n * polygonalApproxAccuracyRate; // parameters!!
        final double tol2 = tol * tol;

        // Pick optimal starting index
        int startPt = getMostEccentricVertexIndex(contour);

        // Rotate the polygon such that most eccentric point comes first:
        List<Pnt2d> rotatedPoly = new ArrayList<>(contour);
        Collections.rotate(rotatedPoly, -startPt);

        // Standard DP stack
        boolean[] keep = new boolean[n];
        keep[0] = true;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});

        while (!stack.isEmpty()) {
            int[] seg = stack.pop();
            int i0 = seg[0], i1 = seg[1];

            Pnt2d A = rotatedPoly.get(i0);
            Pnt2d B = rotatedPoly.get(i1);

            double maxDist2 = -1;
            int indexMax = -1;

            for (int i = i0 + 1; i < i1; i++) {
                double d2 = perpDistSq(rotatedPoly.get(i), A, B);
                if (d2 > maxDist2) {
                    maxDist2 = d2;
                    indexMax = i;
                }
            }

            if (maxDist2 > tol2) {
                keep[indexMax] = true;
                stack.push(new int[]{i0, indexMax});
                stack.push(new int[]{indexMax, i1});
            }
        }

        // Assemble the simplified rotated polygon
        List<Integer> cornerIndexes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                cornerIndexes.add(i);
            }
        }

        // At this place the first point on the contour should be a corner, but we better check:
        if (cornerIndexes.get(0) != 0) {
            throw new IllegalStateException("first point on simplified contour is not a corner");
        }
        return new SegmentedContour(cornerIndexes, rotatedPoly);
    }

    // to be removed ----------------------------------------------------------------------

    @Deprecated
    public MarkerOutline extractQuad(MarkerOutline mol) {
        List<Pnt2d> poly = mol.getPolygon();

        if (poly.size() < minContourLength) {
            return null;
        }

        // Step 1: Simplify the contour outline
        double tol = poly.size() * polygonalApproxAccuracyRate;
        SegmentedContour result = segment(poly);

        // collect the  corners from the adjusted point list:
        List<Pnt2d> smplCtr = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            smplCtr.add(result.getCorner(i));
        }

        // Step 2: Check result for corner count and area
        if (smplCtr.size() != 4) {
            return null;
        }

        // TODO: needs fixing!
        if (Polygons.circularity(smplCtr) < minCircularity) {
            return null;
        }

        int convexity =  Polygons.convexity(smplCtr);
        switch (convexity) {
            case  0 -> { return null; }                     // non-convex contour
            case  1 -> { Collections.reverse(smplCtr); }    // CW contour - reverse!
            case -1 -> { }                                  // CCW contour is OK
            default -> throw new IllegalArgumentException("illegal convexity result: " + convexity);
        }

        // Step 3: Build the composite output object
        // SegmentedContour qc = new SegmentedContour(null, mol.getPolygon()) ;

        return new MarkerOutline(mol, smplCtr);
    }

}
