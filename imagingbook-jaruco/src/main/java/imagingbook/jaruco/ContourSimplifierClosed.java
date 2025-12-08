/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco;

import ij.IJ;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.regions.Contour;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static imagingbook.jaruco.ContourSimplifier.makePoly;
import static imagingbook.common.math.Arithmetic.isZero;
import static imagingbook.common.math.Arithmetic.sqr;

/*
TODO: Merge into common/geometry classes, make more flexible parameters!
 */

public class ContourSimplifierClosed {

    public static List<Pnt2d> simplify(Contour contour, double epsilon) {
        return simplify(contour.getPointList(), epsilon);
    }

    public static List<Pnt2d> simplify(List<Pnt2d> pts, double tol) {
        int n = pts.size();
        if (n <= 3) return new ArrayList<>(pts);

        // Pick optimal starting index
        int start = chooseStart(pts);
        // IJ.log("start = " + start + " = " + pts.get(start));

        // Rotate the polygon
        List<Pnt2d> rotated = new ArrayList<>(n + 1);
        for (int i = 0; i < n; i++)
            rotated.add(pts.get((start + i) % n));

        // Standard DP stack
        boolean[] keep = new boolean[n];
        keep[0] = true;
        // keep[0] = keep[n - 1] = true;

        double tol2 = tol * tol;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});

        while (!stack.isEmpty()) {
            int[] seg = stack.pop();
            int i0 = seg[0], i1 = seg[1];

            Pnt2d A = rotated.get(i0);
            Pnt2d B = rotated.get(i1);

            double maxDist2 = -1;
            int indexMax = -1;

            for (int i = i0 + 1; i < i1; i++) {
                double d2 = perpendicularDistanceSq(rotated.get(i), A, B);
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

        // Build simplified rotated polygon
        List<Pnt2d> simp = new ArrayList<>();
        for (int i = 0; i < n; i++)
            if (keep[i]) simp.add(rotated.get(i));

        // Rotate back
        List<Pnt2d> out = new ArrayList<>();
        int m = simp.size();
        int offset = simp.indexOf(rotated.get(0)); // original rotated start

        for (int i = 0; i < m; i++)
            out.add(simp.get((offset + i) % m));

        return out;
    }

    // --------------------

    static int chooseStart(List<Pnt2d> pts) {
        double cx = 0, cy = 0;
        for (var p : pts) {
            cx += p.getX(); cy += p.getY();
        }
        cx /= pts.size();
        cy /= pts.size();

        int best = 0;
        double maxD2 = -1;

        for (int i = 0; i < pts.size(); i++) {
            double dx = pts.get(i).getX() - cx;
            double dy = pts.get(i).getY() - cy;
            double d2 = dx*dx + dy*dy;
            if (d2 > maxD2) {
                maxD2 = d2;
                best = i;
            }
        }
        return best;
    }


    // Optimized 2D-only perpendicular distance from point P to segment AB
    private static double perpendicularDistance(Pnt2d P, Pnt2d A, Pnt2d B) {
        return Math.sqrt(perpendicularDistanceSq(P, A, B));
    }


    // Squared perpendicular distance from P to line AB
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

    // --------------------------------------------------------------------------------

     public static boolean isConvex(List<Pnt2d> pts) {
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

    public static double getCircularity(List<Pnt2d> poly) {
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

    public static void main(String[] args) {
        // List<Pnt2d> poly = makePoly(20, 220, 140, 220, 140, 180, 20, 180);
        // List<Pnt2d> poly = makePoly(0, 0, 0, 1, 1, 1, 1, 0);     // unit square
        // List<Pnt2d> poly = makePoly(0, 0, 0, 1, 1, 2, 1, 1);       // diamond
        List<Pnt2d> poly = makePoly(0, 0, 0, 1, 10, 1, 10, 0);       // flat rectangle
        // List<Pnt2d> poly = makeCircle(1, 200);
        double area = getArea(poly);
        System.out.println("area = " + area);
        double len = getLength(poly);
        System.out.println("length = " + len);
        double ecc = getCircularity(poly);
        System.out.println("ecc = " + ecc);
    }
}
