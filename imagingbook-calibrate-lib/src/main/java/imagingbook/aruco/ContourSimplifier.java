/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.regions.Contour;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class ContourSimplifier {

    public static List<Pnt2d> simplify(Contour contour, double epsilon, boolean closed) {
        return simplify(contour.getPointList(), epsilon, closed);
    }

    /**
     * Simplifies a 2D polyline (or closed contour) using a iterative (i.e.,
     * non-recursive) implementation of the Douglas–Peucker algorithm.
     * Note that if a contour is specified as closed the final point is identical
     * to the starting point. This means, for example, that the simplified contour
     * of a rectangle with 4 corners has actually 5 vertices.
     * four corners
     * @param pts input list of points
     * @param epsilon max allowed perpendicular distance
     * @param closed whether the polyline is closed (first point connects to last)
     * @return simplified list of points
     */
    public static List<Pnt2d> simplify(List<Pnt2d> pts, double epsilon, boolean closed) {
        int n = pts.size();
        if (n == 0) return Collections.emptyList();
        if (n <= 2 && !closed) return new ArrayList<>(pts);

        // Special case: closed polyline with < 3 points
        if (closed && n < 3) {
            List<Pnt2d> out = new ArrayList<>(pts);
            if (!out.get(0).equals(out.get(out.size() - 1)))
                out.add(out.get(0));
            return out;
        }

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
                double d = perpendicularDistance(pts.get(i), A, B);
                if (d > maxDist) {
                    maxDist = d;
                    farthestIdx = i;
                }

                i = (i + 1) % n;
                if (!closed && i >= stop) break;  // no wrap for open polylines
            }

            if (maxDist > epsilon) {
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

        // Ensure closed polylines actually close
        if (closed) {
            Pnt2d first = result.get(0);
            Pnt2d last  = result.get(result.size() - 1);
            if (!first.equals(last)) {
                result.add(first);
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
            return P.distance(A);
        }
        // Project point onto line segment, clamped to [0,1]
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t * dx;
        double projY = ay + t * dy;
        // dx = P.getX() - projX;
        // dy = P.getY() - projY;
        return Math.hypot(px - projX, py - projY);
    }

    // --------------------------------------------------------------------------------

    public static void main(String[] args) {
        test1();
        test2();
    }

    // tests from https://github.com/locationtech/jts/blob/master/modules/core/src/test/java/org/locationtech/jts/simplify/DouglasPeuckerSimplifierTest.java

    static void test1() {
        List<Pnt2d> poly1 = makePoly(20, 220, 40, 220, 60, 220, 80, 220, 100, 220, 120, 220, 140, 220, 140, 180, 100, 180, 60, 180, 20, 180, 20, 220);
        List<Pnt2d> poly2 = makePoly(20, 220, 140, 220, 140, 180, 20, 180, 20, 220);
        double tol = 10.0;
        boolean closed = true;
        doTest(poly1, poly2, tol, closed);
    }

    static void test2() {
        List<Pnt2d> poly1 = makePoly(120, 120, 121, 121, 122, 122, 220, 120, 180, 199, 160, 200, 140, 199, 120, 120);
        List<Pnt2d> poly2 = makePoly(120, 120, 220, 120, 180, 199, 160, 200, 140, 199, 120, 120);
        double tol = 10.0;
        boolean closed = true;
        doTest(poly1, poly2, tol, closed);
    }

    // ------------------------------------------------------------------------

    static void doTest(List<Pnt2d> poly1, List<Pnt2d> poly2, double tol, boolean closed) {
        List<Pnt2d> spoly = ContourSimplifier.simplify(poly1, tol, true);
        //spoly.forEach(p -> System.out.println(p.toString()));
        System.out.println("Check OK: " + checkSame(poly2, spoly));
    }

        private static List<Pnt2d> makePoly(double... coords) {
        List<Pnt2d> pntList = new ArrayList<>();
        for (int i = 0; i < coords.length; i+=2) {
            pntList.add(Pnt2d.from(coords[i], coords[i+1]));
        }
        return pntList;
    }

    private static boolean checkSame(List<Pnt2d> poly1, List<Pnt2d> poly2) {
        if (poly1.size() != poly2.size()) {
            return false;
        }
        for (int i = 0; i < poly1.size(); i++) {
            Pnt2d p1 = poly1.get(i);
            Pnt2d p2 = poly2.get(i);
            if (p1.distanceSq(p2) > 1e-6) {
                return false;
            }
        }
        return true;
    }


}
