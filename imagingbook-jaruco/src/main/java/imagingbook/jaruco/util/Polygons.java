/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco.util;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;

import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static imagingbook.common.math.Arithmetic.isZero;
import static imagingbook.common.math.Arithmetic.sqr;

/*
TODO: Merge into common/geometry classes, make more flexible parameters!
 */

/**
 * Static utility methods for dealing with closed polygons.
 */
@Deprecated
public class Polygons {

    @Deprecated
    public static List<Pnt2d> simplify(List<Pnt2d> pts, double tol) {
        final double tol2 = tol * tol;
        final int n = pts.size();
        if (n <= 3) return new ArrayList<>(pts);

        // Pick optimal starting index
        int startPt = getMostEccentricVertexIndex(pts);

        // Rotate the polygon such that most eccentric point comes first:
        List<Pnt2d> rotatedPoly = new ArrayList<>(n + 1);
        for (int i = 0; i < n; i++)
            rotatedPoly.add(pts.get((startPt + i) % n));

        // Standard DP stack
        boolean[] keep = new boolean[n];
        keep[0] = true;
        // keep[0] = keep[n - 1] = true;

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
        List<Pnt2d> simp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                simp.add(rotatedPoly.get(i));
            }
        }

        // At this moment the first point on the contour is likely a corner,
        // but this is not guaranteed.

        // Rotate back
        List<Pnt2d> out = new ArrayList<>();

        // find index of first corner in original point sequence
        int offset = simp.indexOf(rotatedPoly.get(0));

        int m = simp.size();
        for (int i = 0; i < m; i++) {
            out.add(simp.get((offset + i) % m));
        }

        return out;
    }

    // --------------------

    @Deprecated
    public static Pnt2d getCentroid(List<Pnt2d> pts) {
        int n = pts.size();
        double cx = 0, cy = 0;
        for (var p : pts) {
            cx += p.getX();
            cy += p.getY();
        }

        return Pnt2d.from(cx / n,  cy / n);
    }

    @Deprecated
    public static int getMostEccentricVertexIndex(List<Pnt2d> pts) {
        Pnt2d ctr = getCentroid(pts);
        double cx = ctr.getX();
        double cy = ctr.getX();

        int best = 0;
        double maxD2 = -1;

        for (int i = 0; i < pts.size(); i++) {
            double dx = pts.get(i).getX() - cx;
            double dy = pts.get(i).getY() - cy;
            // double d2 = dx*dx + dy*dy;
            double d2 = sqr(pts.get(i).getX() - cx) + sqr(pts.get(i).getY() - cy);
            if (d2 > maxD2) {
                maxD2 = d2;
                best = i;
            }
        }
        return best;
    }

    // Optimized 2D-only perpendicular distance from point P to segment AB
    public static double perpDist(Pnt2d P, Pnt2d A, Pnt2d B) {
        return Math.sqrt(perpDistSq(P, A, B));
    }

    // Squared perpendicular distance from P to line AB
    public static double perpDistSq(Pnt2d P, Pnt2d A, Pnt2d B) {
        final double ax = A.getX(), ay = A.getY();
        final double bx = B.getX(), by = B.getY();
        final double px = P.getX(), py = P.getY();
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) {
            return P.distanceSq(A);
        }
        // Project point onto line segment, clamped to [0,1]
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t * dx;
        double projY = ay + t * dy;
        // return Math.hypot(px - projX, py - projY);
        return sqr(px - projX) + sqr(py - projY);
    }

    // --------------------------------------------------------------------------------

    /**
     * Checks if the supplied closed polygon is convex.
     * If not convex, 0 is returned. Otherwise, the associated winding order is
     * returned, that is, -1 for CCW and 1 for CW order.
     * @param polygon the polygon
     * @return 0 if non-convex, 1 or -1 otherwise
     */
    @Deprecated
     public static int convexity(List<Pnt2d> polygon) {
        int n = polygon.size();
        // if (n < 4) return true; // triangles always convex (but we may want to know winding rule)
        if (n < 2) return 0;    // single points and lines are not convex

        double sign = 0;

        for (int i = 0; i < n; i++) {
            Pnt2d a = polygon.get(i);
            Pnt2d b = polygon.get((i + 1) % n);
            Pnt2d c = polygon.get((i + 2) % n);
            double cross =
                    (b.getX() - a.getX()) * (c.getY() - b.getY()) -
                    (b.getY() - a.getY()) * (c.getX() - b.getX());

            if (cross == 0) continue; // collinear → ignore

            if (sign == 0) { // sign still undetermined
                sign = Math.signum(cross);
            } else if (Math.signum(cross) != sign) {
                return 0; // turn direction changed → concave
            }
        }
        return (int) sign;
    }

    @Deprecated
    public static double getLength(List<Pnt2d> poly) {
        final int n = poly.size();
        double len = 0;
        for (int i = 0; i < n; i++) {
            Pnt2d pi = poly.get(i);
            Pnt2d pj = poly.get((i + 1) % n);
            len += pi.distance(pj);
        }

        return len;
    }

    /**
     * Polygon area calculation from vertices (Gaussian formula).
     *
     * @param poly
     * @return
     */
    public static double getArea(List<Pnt2d> poly) {
        final int n = poly.size();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            Pnt2d pi = poly.get(i);
            Pnt2d pj = poly.get((i + 1) % n);
            sum += (pi.getX() * pj.getY()) - (pj.getX() * pi.getY());
        }

        return Math.abs(sum) / 2;
    }

    public static double circularity(List<Pnt2d> poly) {
        double area = getArea(poly);
        double len =  getLength(poly);
        if (isZero(len)) {
            throw new ArithmeticException("zero polygon length encountered");
        }
        return 4 * Math.PI * area / sqr(len);
    }

    static List<Pnt2d> makeCircle(double radius, int steps) {
        List<Pnt2d> circle = new ArrayList<>(steps);
        for (int i = 0; i < steps; i++) {
            double angle = i * 2 * Math.PI / steps;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            circle.add(Pnt2d.from(x, y));
        }
        return circle;
    }

    // --------------------------------------------------------------------------------

    /**
     * For testing.
     * @param coords a sequence of x/y coordinate pairs
     * @return
     */
    public static List<Pnt2d> makePolygon(double... coords) {
        List<Pnt2d> pntList = new ArrayList<>();
        for (int i = 0; i < coords.length; i+=2) {
            pntList.add(Pnt2d.from(coords[i], coords[i + 1]));
        }
        return pntList;
    }
    /**
     * For testing.
     * @param coords a Nx2 array of x/y coordinate pairs
     * @return
     */
    public static List<Pnt2d> makePolygon(double[][] coords) {
        List<Pnt2d> pntList = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            pntList.add(Pnt2d.from(coords[i][0], coords[i][1]));
        }
        return pntList;
    }

    public static Pnt2d[] toPointArray(List<Pnt2d> poly) {
        return poly.toArray(new Pnt2d[0]);
    }

    public static boolean checkSame(List<Pnt2d> A, List<Pnt2d> B) {
        if (A.size() != B.size()) {
            return false;
        }
        int missCnt = 0;
        for (int i = 0; i < A.size(); i++) {
            Pnt2d p1 = A.get(i);
            Pnt2d p2 = B.get(i);
            if (p1.distanceSq(p2) > 1e-6) {
                missCnt++;
                // break;   // iff efficiency is an issue
            }
            // String mark = (tooFarOff) ? "*WRONG*" : "ok";
            // System.out.printf("%d: p1=%s p2=%s %s\n", i, p1, p2, mark);
        }
        return missCnt == 0;
    }

    /**
     * Creates and returns a {@code double[2][N]} array, i.e., x and y
     * coordinates placed in a pair of separate arrays.
     * @param pts a list of 2D points of length {@code N}
     * @return a {@code double[2][N]} coordinate array
     */
    public static double[][] toXYArray(List<Pnt2d> pts) {
        int m = pts.size();
        double[][] XY = new double[2][m]; // XY[0] are x-values, XY[1] are y-values
        int i = 0;
        for (Pnt2d p : pts) {
            XY[0][i] = p.getX();
            XY[1][i] = p.getY();
            i++;
        }
        return XY;
    }

    /**
     * Creates and returns a list of {@code N} {@link Pnt2d} points derived
     * from the supplied {@code double[2][N]} array.
     * @param XY a {@code double[2][N]} coordinate array
     * @return a list of {@code N} {@link Pnt2d} instances
     */
    public static List<Pnt2d> fromXYArray(double[][] XY) {
        int m = XY[0].length;
        if (m != XY[1].length) {
            throw new IllegalArgumentException("mismatch XY subarray length");
        }
        List<Pnt2d> pntList = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            pntList.add(Pnt2d.from(XY[0][i], XY[1][i]));
        }
        return pntList;
    }

    // -------------------------------------------------------------------------

    public static Path2D getPolygonPath(List<Pnt2d> contour) {
        return getPolygonPath(contour, 0, 0);
    }

    // closed
    public static Path2D getPolygonPath(List<Pnt2d> contour, double xOffset, double yOffset) {
        Path2D path = new Path2D.Float();
        Pnt2d[] pnts = contour.toArray(new Pnt2d[0]);
        if (pnts.length > 1) {
            path.moveTo(pnts[0].getX() + xOffset, pnts[0].getY() + yOffset);
            for (int i = 1; i < pnts.length; i++) {
                path.lineTo(pnts[i].getX() + xOffset,  pnts[i].getY() + yOffset);
            }
            path.closePath();
        }
        else {	// special case: mark a single pixel region "X"
            double x = pnts[0].getX();
            double y = pnts[0].getY();
            path.moveTo(x + xOffset - 0.5, y + yOffset - 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset + 0.5);
            path.moveTo(x + xOffset - 0.5, y + yOffset + 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset - 0.5);
        }
        return path;
    }

    // non-closed
    public static Path2D getPolylinePath(List<Pnt2d> contour) {
        return getPolylinePath(contour, 0, 0);
    }

    // non-closed
    public static Path2D getPolylinePath(List<Pnt2d> contour, double xOffset, double yOffset) {
        Path2D path = new Path2D.Float();
        Pnt2d[] pnts = contour.toArray(new Pnt2d[0]);
        if (pnts.length > 1) {
            path.moveTo(pnts[0].getX() + xOffset, pnts[0].getY() + yOffset);
            for (int i = 1; i < pnts.length; i++) {
                path.lineTo(pnts[i].getX() + xOffset,  pnts[i].getY() + yOffset);
            }
            // path.closePath();
        }
        else {	// special case: mark a single pixel region "X"
            double x = pnts[0].getX();
            double y = pnts[0].getY();
            path.moveTo(x + xOffset - 0.5, y + yOffset - 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset + 0.5);
            path.moveTo(x + xOffset - 0.5, y + yOffset + 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset - 0.5);
        }
        return path;
    }

    // Migrated to imagingbook.common.geometry.mappings.Mapping2D
    // public static List<Pnt2d> applyMapping(List<Pnt2d> poly, LinearMapping2D mapping) {
    //     List<Pnt2d> poly2 = new ArrayList<>(poly.size());
    //     for (Pnt2d p : poly) {
    //         poly2.add(mapping.applyTo(p));
    //     }
    //     return poly2;
    // }

    public static String toString(List<Pnt2d>  poly) {
        if (poly == null) {
            return Objects.toString(null);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Polygon [");
        for (Pnt2d p : poly) {
            sb.append(String.format(Locale.US, "[%.2f, %.2f], ", p.getX(), p.getY()));
        }
        sb.append("]");
        return sb.toString();
    }

}
