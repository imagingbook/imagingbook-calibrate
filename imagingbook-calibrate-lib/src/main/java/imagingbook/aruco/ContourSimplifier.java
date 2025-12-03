/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.line.AlgebraicLine;
import imagingbook.common.regions.Contour;

import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;

/*
TODO: Merge into common/geometry classes, make more flexible parameters!
 */

public class ContourSimplifier {

    public static List<Pnt2d> simplify(Contour contour, double epsilon, boolean closed) {
        return simplify(contour.getPointList(), epsilon, closed);
    }

    /**
     * Simplifies a 2D polyline (or closed contour) using a iterative (i.e.,
     * non-recursive) implementation of the Douglas–Peucker algorithm.
     * If the given point sequence is assumed to represents a closed polygon, the
     * resulting simplified point sequence is also implicitly  closed,
     * i.e., its last vertex connects to its first
     * (the first point is not duplicated at the end).
     *
     * @param pts input list of points
     * @param epsilon max allowed perpendicular distance
     * @param closed whether the polyline is assumed to be closed (first point connects to last)
     * @return simplified list of points
     */
    public static List<Pnt2d> simplify(List<Pnt2d> pts, double epsilon, boolean closed) {
        final double epsilonSq = sqr(epsilon);  // using squared distances for efficiency
        int n = pts.size();
        if (n == 0) return Collections.emptyList();
        if (n <= 3) return new ArrayList<>(pts);

        // Special case: closed polyline with < 3 points
        // if (closed && n < 3) {
        //     List<Pnt2d> out = new ArrayList<>(pts);
        //     if (!out.get(0).equals(out.get(out.size() - 1)))
        //         out.add(out.get(0));
        //     return out;
        // }

        // Result: start with the first point
        List<Pnt2d> result = new ArrayList<>();
        result.add(pts.get(0));

        // Working ranges (start index, end index)
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, closed ? n : n - 1});

        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int start = range[0];
            int end = range[1];

            Pnt2d A = pts.get(start);
            Pnt2d B = pts.get(end % n); // wrap for closed contours

            double maxDist = -1;
            int farthestIdx = -1;

            // Walk from (start+1) to end-1, with wrap if closed
            int i = (start + 1) % n;
            int stop = end % n;

            while (i != stop) {
                double d = perpendicularDistanceSq(pts.get(i), A, B);
                if (d > maxDist) {
                    maxDist = d;
                    farthestIdx = i;
                }
                i = (i + 1) % n;
                if (!closed && i >= stop) break;  // no wrap for open polylines
            }
            if (maxDist > epsilonSq) {
                // Subdivide
                stack.push(new int[]{farthestIdx, end});
                stack.push(new int[]{start, farthestIdx});
            } else {
                // Keep the end point if not already appended
                Pnt2d pt = pts.get(end % n);
                if (!pt.equals(result.get(result.size() - 1))) {
                    result.add(pt);
                }
            }
        }

        // Ensure closed polylines don't have starting point duplicated at the end:
        if (closed) {
            int lastIdx = result.size() - 1;
            Pnt2d ps = result.get(0);
            Pnt2d pe  = result.get(lastIdx);
            if (ps.equals(pe)) {
                result.remove(lastIdx);
            }
        }

        return result;
    }


    // public static List<Pnt2d> simplify(List<Pnt2d> points, double epsilon, boolean closed) {
    //     int n = points.size();
    //     if (n < 3) return new ArrayList<>(points);
    //
    //     // Result list
    //     List<Pnt2d> result = new ArrayList<>();
    //     result.add(points.get(0));
    //
    //     // Stack of ranges to process: start and end indices
    //     Deque<int[]> stack = new ArrayDeque<>();
    //     stack.push(new int[]{0, closed ? n : n - 1});
    //
    //     while (!stack.isEmpty()) {
    //         int[] range = stack.pop();
    //         int start = range[0];
    //         int end = range[1];
    //
    //         double maxDist = -1;
    //         int index = -1;
    //
    //         Pnt2d A = points.get(start);
    //         Pnt2d B = points.get(end % n); // wrap for closed contours
    //
    //         // Find farthest point from line segment AB
    //         for (int i = (start + 1) % n; i != end % n; i = (i + 1) % n) {
    //             double dist = perpendicularDistance(points.get(i), A, B);
    //             if (dist > maxDist) {
    //                 maxDist = dist;
    //                 index = i;
    //             }
    //             if (i == (n - 1) && closed) break; // avoid infinite loop in closed wrap
    //         }
    //
    //         if (maxDist > epsilon) {
    //             // Subdivide: push ranges in DFS order (start->index, index->end)
    //             stack.push(new int[]{index, end});
    //             stack.push(new int[]{start, index});
    //         } else {
    //             // Keep end point if not already in result
    //             Pnt2d pt = points.get(end % n);
    //             if (!result.contains(pt)) result.add(pt);
    //         }
    //     }
    //
    //     // For closed contours, ensure the first point equals last point
    //     if (closed && !result.get(0).equals(result.get(result.size() - 1))) {
    //         result.add(result.get(0));
    //     }
    //
    //     return result;
    // }

    // Optimized 2D-only perpendicular distance from point P to segment AB
    private static double perpendicularDistance(Pnt2d P, Pnt2d A, Pnt2d B) {
        return Math.sqrt(perpendicularDistanceSq(P, A, B));
    }

    private static double perpendicularDistanceSq(Pnt2d P, Pnt2d A, Pnt2d B) {
        final double ax = A.getX(), ay = A.getY();
        final double bx = B.getX(), by = B.getY();
        final double px = P.getX(), py = P.getY();
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) {
            // A and B are identical
            // dx = P.getX() - A.getX();
            // dy = P.getY() - A.getY();
            // return Math.hypot(dx, dy);
            return P.distanceSq(A);
        }
        // Project point onto line segment, clamped to [0,1]
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t * dx;
        double projY = ay + t * dy;
        // dx = P.getX() - projX;
        // dy = P.getY() - projY;
        // return Math.hypot(px - projX, py - projY);
        return sqr(px - projX) + sqr(py - projY);
    }


    /**
     * Checks if this convex hull contains the specified point. This method is used instead of
     * {@link Path2D#contains(double, double)} to avoid false results due to roundoff errors.
     *
     * @param vertices some 2D points
     * @param tolerance positive quantity for being outside
     * @return true if the point is inside the hull
     */
    static boolean isConvex(List<Pnt2d> vertices, double tolerance) {
        final int n = vertices.size();
        // for all line segments:
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            Pnt2d pi = vertices.get(i);
            Pnt2d pj = vertices.get(j);
            if (!pi.equals(pj, 1e-6)) {
                AlgebraicLine line = AlgebraicLine.from(vertices.get(i), vertices.get(j));
                // check all vertices p
                for (Pnt2d p : vertices) {
                    double dist = line.getSignedDistance(p);
                    // positive signed distance means that the point is to the left
                    if (dist + tolerance < 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean isConvex(List<Pnt2d> pts) {
        int n = pts.size();
        // if (n < 4) return true; // triangles always convex (but we may want to know winding rule)
        if (n < 3) return true;    // single points and lines are convex

        double sign = 0;

        for (int i = 0; i < n; i++) {
            Pnt2d a = pts.get(i);
            Pnt2d b = pts.get((i + 1) % n);
            Pnt2d c = pts.get((i + 2) % n);
            double cross =
                    (b.getX() - a.getX()) * (c.getY() - b.getY()) -
                    (b.getY() - a.getY()) * (c.getX() - b.getX());

            if (cross == 0) continue; // collinear → ignore

            if (sign == 0) { // sign still undetermined
                sign = Math.signum(cross);
            } else if (Math.signum(cross) != sign) {
                return false; // turn direction changed → concave
            }
        }
        return true;
    }

    // --------------------------------------------------------------------------------

    public static void main(String[] args) {
        // test1();
        // test2();
        test3();
    }

    // tests from https://github.com/locationtech/jts/blob/master/modules/core/src/test/java/org/locationtech/jts/simplify/DouglasPeuckerSimplifierTest.java

    static void test1() {
        System.out.println("test1");
        List<Pnt2d> poly1 = makePoly(20, 220, 40, 220, 60, 220, 80, 220, 100, 220, 120, 220, 140, 220, 140, 180, 100, 180, 60, 180, 20, 180);
        List<Pnt2d> poly2 = makePoly(20, 220, 140, 220, 140, 180, 20, 180);
        double tol = 10.0;
        boolean closed = true;
        doTest(poly1, poly2, tol, closed);
    }

    static void test2() {
        System.out.println("test2");
        List<Pnt2d> poly1 = makePoly(120, 120, 121, 121, 122, 122, 220, 120, 180, 199, 160, 200, 140, 199);
        List<Pnt2d> poly2 = makePoly(120, 120, 220, 120, 180, 199, 160, 200, 140, 199);
        double tol = 10.0;
        boolean closed = true;
        doTest(poly1, poly2, tol, closed);
    }

    static void test3() {
        System.out.println("test3");
        // from https://github.com/mourner/simplify-js/blob/master/test/test.js
        double[][] origpoints = {
                {224.55, 250.15},{226.91, 244.19},{233.31, 241.45},{234.98, 236.06},
                {244.21, 232.76},{262.59, 215.31},{267.76, 213.81},{273.57, 201.84},
                {273.12, 192.16},{277.62, 189.03},{280.36, 181.41},{286.51, 177.74},
                {292.41, 159.37},{296.91, 155.64},{314.95, 151.37},{319.75, 145.16},
                {330.33, 137.57},{341.48, 139.96},{369.98, 137.89},{387.39, 142.51},
                {391.28, 139.39},{409.52, 141.14},{414.82, 139.75},{427.72, 127.30},
                {439.60, 119.74},{474.93, 107.87},{486.51, 106.75},{489.20, 109.45},
                {493.79, 108.63},{504.74, 119.66},{512.96, 122.35},{518.63, 120.89},
                {524.09, 126.88},{529.57, 127.86},{534.21, 140.93},{539.27, 147.24},
                {567.69, 148.91},{575.25, 157.26},{580.62, 158.15},{601.53, 156.85},
                {617.74, 159.86},{622.00, 167.04},{629.55, 194.60},{638.90, 195.61},
                {641.26, 200.81},{651.77, 204.56},{671.55, 222.55},{683.68, 217.45},
                {695.25, 219.15},{700.64, 217.98},{703.12, 214.36},{712.26, 215.87},
                {721.49, 212.81},{727.81, 213.36},{729.98, 208.73},{735.32, 208.20},
                {739.94, 204.77},{769.98, 208.42},{779.60, 216.87},{784.20, 218.16},
                {800.24, 214.62},{810.53, 219.73},{817.19, 226.82},{820.77, 236.17},
                {827.23, 236.16},{829.89, 239.89},{851.00, 248.94},{859.88, 255.49},
                {865.21, 268.53},{857.95, 280.30},{865.48, 291.45},{866.81, 298.66},
                {864.68, 302.71},{867.79, 306.17},{859.87, 311.37},{860.08, 314.35},
                {858.29, 314.94},{858.10, 327.60},{854.54, 335.40},{860.92, 343.00},
                {856.43, 350.15},{851.42, 352.96},{849.84, 359.59},{854.56, 365.53},
                {849.74, 370.38},{844.09, 371.89},{844.75, 380.44},{841.52, 383.67},
                {839.57, 390.40},{845.59, 399.05},{848.40, 407.55},{843.71, 411.30},
                {844.09, 419.88},{839.51, 432.76},{841.33, 441.04},{847.62, 449.22},
                {847.16, 458.44},{851.38, 462.79},{853.97, 471.15},{866.36, 480.77}};

        double[][] smplpoints = {
                {224.55, 250.15},{267.76, 213.81},{296.91, 155.64},{330.33, 137.57},
                {409.52, 141.14},{439.60, 119.74},{486.51, 106.75},{529.57, 127.86},
                {539.27, 147.24},{617.74, 159.86},{629.55, 194.60},{671.55, 222.55},
                // {727.81, 213.36},{739.94, 204.77},{769.98, 208.42},{779.60, 216.87},
                {727.81, 213.36},{739.94, 204.77},{769.98, 208.42},{784.20, 218.16},
                {800.24, 214.62},{820.77, 236.17},{859.88, 255.49},{865.21, 268.53},
                // {857.95, 280.30},{867.79, 306.17},{859.87, 311.37},{854.54, 335.40},
                {857.95, 280.30},{867.79, 306.17},{858.29, 314.94},{854.54, 335.40},
                {860.92, 343.00},{849.84, 359.59},{854.56, 365.53},{844.09, 371.89},
                {839.57, 390.40},{848.40, 407.55},{839.51, 432.76},{853.97, 471.15},
                {866.36, 480.77}};

        List<Pnt2d> poly1 = makePoly(origpoints);
        List<Pnt2d> poly2 = makePoly(smplpoints);
        double tol = 5.0;
        boolean closed = false;
        doTest(poly1, poly2, tol, closed);
    }



    // ------------------------------------------------------------------------

    static void doTest(List<Pnt2d> poly1, List<Pnt2d> poly2, double tol, boolean closed) {
        List<Pnt2d> spoly = ContourSimplifier.simplify(poly1, tol, closed);
        spoly.forEach(p -> System.out.println("   " + p.toString()));
        System.out.println("Expected length = : " + poly2.size());
        System.out.println("Simplified length = : " + spoly.size());
        System.out.println("Check OK: " + checkSame(poly2, spoly));
        System.out.println("Check convex: " + isConvex(spoly));
    }

    private static List<Pnt2d> makePoly(double... coords) {
        List<Pnt2d> pntList = new ArrayList<>();
        for (int i = 0; i < coords.length; i+=2) {
            pntList.add(Pnt2d.from(coords[i], coords[i+1]));
        }
        return pntList;
    }

    private static List<Pnt2d> makePoly(double[][] coords) {
        List<Pnt2d> pntList = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            pntList.add(Pnt2d.from(coords[i][0], coords[i][1]));
        }
        return pntList;
    }

    private static boolean checkSame(List<Pnt2d> poly1, List<Pnt2d> poly2) {
        if (poly1.size() != poly2.size()) {
            return false;
        }
        int falseCnt = 0;
        for (int i = 0; i < poly1.size(); i++) {
            Pnt2d p1 = poly1.get(i);
            Pnt2d p2 = poly2.get(i);
            boolean wrong = (p1.distanceSq(p2) > 1e-6);
            if (wrong) {
                falseCnt++;
            }
            String mark = (wrong) ? "*WRONG*" : "ok";
            System.out.printf("%d: p1=%s p2=%s %s\n", i, p1, p2, mark);
        }
        return falseCnt == 0;
    }


}
