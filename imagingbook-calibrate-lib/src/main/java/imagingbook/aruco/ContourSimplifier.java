/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.math.Arithmetic;
import imagingbook.common.regions.Contour;

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

        // Ensure closed polylines don't have the starting point duplicated at the end:
        if (closed) {
            int lastIdx = result.size() - 1;
            Pnt2d ps = result.get(0);
            Pnt2d pe  = result.get(lastIdx);
            // if (ps.equals(pe)) {
            //     result.remove(lastIdx);
            // }
            result.remove(lastIdx);
        }

        return result;
    }

    // Optimized 2D-only perpendicular distance from point P to segment AB
    private static double perpendicularDistance(Pnt2d P, Pnt2d A, Pnt2d B) {
        return Math.sqrt(perpendicularDistanceSq(P, A, B));
    }



    /**
     * Remove points that lie within 'tol' perpendicular distance of the segment
     * connecting their neighbors. Uses squared distances, no sqrt.
     *
     * @param pts  input polygon, open or closed
     * @param closed  whether polygon is closed
     * @param tol  distance tolerance (actual distance, NOT squared)
     * @return simplified polygon list
     */
    public static List<Pnt2d> cleanupCollinear(List<Pnt2d> pts, double tol, boolean closed) {
        int n = pts.size();
        if (n <= 2) return new ArrayList<>(pts);

        double tol2 = tol * tol;
        List<Pnt2d> clean = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            // If open polygon and endpoints: always keep
            if (!closed && (i == 0 || i == n - 1)) {
                clean.add(pts.get(i));
                continue;
            }

            int iPrev, iNext;
            if (closed) {
                iPrev = Arithmetic.mod(i - 1, n);
                iNext = Arithmetic.mod(i + 1, n);
            }
            else {
                iPrev = Math.max(i - 1, 0);
                iNext = Math.min(i + 1, n - 1);
            }

            // int iPrev = closed ? Arithmetic.mod(i - 1, n) : Math.max(i - 1, 0);
            // int iNext = closed ? Arithmetic.mod(i + 1, n) : Math.min(i + 1, n - 1);

            // int iPrev = (i == 0 ? (closed ? n - 1 : 0) : i - 1);
            // int iNext = (i == n - 1 ? (closed ? 0 : n - 1) : i + 1);
            Pnt2d A = pts.get(iPrev);
            Pnt2d B = pts.get(i);
            Pnt2d C = pts.get(iNext);

            // Squared perpendicular distance from B to line AC
            double dist2 = perpendicularDistanceSq(B, A, C);

            // Keep only if distance >= tolerance OR if AC is degenerate
            if (dist2 >= tol2 || A.equals(C)) {
                clean.add(B);
            }
        }
        return clean;
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

    // --------------------------------------------------------------------------------

    public static void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
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

    static void test4() {
        System.out.println("test4");
        // real marker contour
        int[][] origpoints = {
                {89, 28},
                {89, 29},
                {89, 30},
                {89, 31},
                {89, 32},
                {89, 33},
                {89, 34},
                {89, 35},
                {89, 36},
                {89, 37},
                {89, 38},
                {89, 39},
                {89, 40},
                {89, 41},
                {89, 42},
                {89, 43},
                {89, 44},
                {89, 45},
                {89, 46},
                {89, 47},
                {88, 47},
                {88, 48},
                {88, 49},
                {88, 50},
                {88, 51},
                {88, 52},
                {88, 53},
                {88, 54},
                {88, 55},
                {88, 56},
                {88, 57},
                {88, 58},
                {88, 59},
                {88, 60},
                {88, 61},
                {88, 62},
                {87, 62},
                {87, 63},
                {87, 64},
                {87, 65},
                {87, 66},
                {87, 67},
                {87, 68},
                {87, 69},
                {87, 70},
                {87, 71},
                {87, 72},
                {87, 73},
                {87, 74},
                {87, 75},
                {87, 76},
                {87, 77},
                {87, 78},
                {86, 78},
                {86, 79},
                {86, 80},
                {86, 81},
                {86, 82},
                {86, 83},
                {86, 84},
                {86, 85},
                {86, 86},
                {86, 87},
                {86, 88},
                {86, 89},
                {86, 90},
                {86, 91},
                {85, 91},
                {85, 92},
                {85, 93},
                {85, 94},
                {85, 95},
                {85, 96},
                {85, 97},
                {85, 98},
                {85, 99},
                {85, 100},
                {85, 101},
                {85, 102},
                {85, 103},
                {85, 104},
                {85, 105},
                {85, 106},
                {85, 107},
                {85, 108},
                {85, 109},
                {85, 110},
                {84, 110},
                {84, 111},
                {84, 112},
                {84, 113},
                {84, 114},
                {84, 115},
                {84, 116},
                {84, 117},
                {84, 118},
                {84, 119},
                {84, 120},
                {84, 121},
                {84, 122},
                {84, 123},
                {84, 124},
                {84, 125},
                {83, 125},
                {83, 126},
                {83, 127},
                {83, 128},
                {83, 129},
                {83, 130},
                {83, 131},
                {83, 132},
                {83, 133},
                {83, 134},
                {83, 135},
                {83, 136},
                {83, 137},
                {83, 138},
                {83, 139},
                {83, 140},
                {83, 141},
                {83, 142},
                {82, 142},
                {82, 143},
                {82, 144},
                {82, 145},
                {82, 146},
                {82, 147},
                {82, 148},
                {82, 149},
                {82, 150},
                {82, 151},
                {82, 152},
                {82, 153},
                {82, 154},
                {82, 155},
                {82, 156},
                {82, 157},
                {82, 158},
                {82, 159},
                {82, 160},
                {82, 161},
                {82, 162},
                {82, 163},
                {81, 163},
                {81, 164},
                {81, 165},
                {81, 166},
                {81, 167},
                {81, 168},
                {81, 169},
                {81, 170},
                {81, 171},
                {81, 172},
                {81, 173},
                {81, 174},
                {81, 175},
                {81, 176},
                {81, 177},
                {81, 178},
                {81, 179},
                {81, 180},
                {81, 181},
                {81, 182},
                {81, 183},
                {81, 184},
                {81, 185},
                {80, 185},
                {80, 186},
                {80, 187},
                {80, 188},
                {80, 189},
                {80, 190},
                {80, 191},
                {80, 192},
                {80, 193},
                {80, 194},
                {80, 195},
                {80, 196},
                {80, 197},
                {80, 198},
                {80, 199},
                {80, 200},
                {80, 201},
                {80, 202},
                {80, 203},
                {80, 204},
                {79, 204},
                {79, 205},
                {79, 206},
                {79, 207},
                {79, 208},
                {79, 209},
                {79, 210},
                {79, 211},
                {79, 212},
                {79, 213},
                {79, 214},
                {79, 215},
                {79, 216},
                {79, 217},
                {79, 218},
                {79, 219},
                {79, 220},
                {79, 221},
                {79, 222},
                {79, 223},
                {80, 223},
                {80, 224},
                {81, 224},
                {82, 224},
                {83, 224},
                {84, 224},
                {85, 224},
                {86, 224},
                {87, 224},
                {88, 224},
                {89, 224},
                {90, 224},
                {91, 224},
                {92, 224},
                {93, 224},
                {94, 224},
                {95, 224},
                {96, 224},
                {97, 224},
                {98, 224},
                {99, 224},
                {100, 224},
                {100, 225},
                {101, 225},
                {102, 225},
                {103, 225},
                {104, 225},
                {105, 225},
                {106, 225},
                {107, 225},
                {108, 225},
                {109, 225},
                {110, 225},
                {111, 225},
                {112, 225},
                {113, 225},
                {114, 225},
                {115, 225},
                {116, 225},
                {117, 225},
                {118, 225},
                {119, 225},
                {120, 225},
                {121, 225},
                {122, 225},
                {123, 225},
                {124, 225},
                {125, 225},
                {126, 225},
                {127, 225},
                {128, 225},
                {129, 225},
                {130, 225},
                {131, 225},
                {132, 225},
                {133, 225},
                {134, 225},
                {135, 225},
                {135, 226},
                {136, 226},
                {137, 226},
                {138, 226},
                {139, 226},
                {140, 226},
                {141, 226},
                {142, 226},
                {143, 226},
                {144, 226},
                {145, 226},
                {146, 226},
                {147, 226},
                {148, 226},
                {149, 226},
                {150, 226},
                {151, 226},
                {152, 226},
                {153, 226},
                {154, 226},
                {155, 226},
                {156, 226},
                {157, 226},
                {158, 226},
                {159, 226},
                {160, 226},
                {161, 226},
                {162, 226},
                {163, 226},
                {164, 226},
                {164, 227},
                {165, 227},
                {166, 227},
                {167, 227},
                {168, 227},
                {169, 227},
                {170, 227},
                {171, 227},
                {172, 227},
                {173, 227},
                {174, 227},
                {175, 227},
                {176, 227},
                {177, 227},
                {178, 227},
                {179, 227},
                {180, 227},
                {181, 227},
                {182, 227},
                {183, 227},
                {184, 227},
                {185, 227},
                {186, 227},
                {187, 227},
                {188, 227},
                {189, 227},
                {190, 227},
                {191, 227},
                {192, 227},
                {192, 228},
                {193, 228},
                {194, 228},
                {195, 228},
                {196, 228},
                {197, 228},
                {198, 228},
                {199, 228},
                {200, 228},
                {201, 228},
                {202, 228},
                {203, 228},
                {204, 228},
                {205, 228},
                {206, 228},
                {207, 228},
                {208, 228},
                {209, 228},
                {210, 228},
                {211, 228},
                {212, 228},
                {213, 228},
                {214, 228},
                {215, 228},
                {216, 228},
                {217, 228},
                {218, 228},
                {219, 228},
                {220, 228},
                {220, 229},
                {221, 229},
                {222, 229},
                {223, 229},
                {224, 229},
                {225, 229},
                {226, 229},
                {227, 229},
                {228, 229},
                {229, 229},
                {230, 229},
                {231, 229},
                {232, 229},
                {233, 229},
                {234, 229},
                {235, 229},
                {236, 229},
                {237, 229},
                {238, 229},
                {239, 229},
                {240, 229},
                {241, 229},
                {242, 229},
                {243, 229},
                {244, 229},
                {245, 229},
                {246, 229},
                {246, 230},
                {247, 230},
                {248, 230},
                {249, 230},
                {250, 230},
                {251, 230},
                {252, 230},
                {253, 230},
                {254, 230},
                {255, 230},
                {256, 230},
                {257, 230},
                {258, 230},
                {259, 230},
                {260, 230},
                {261, 230},
                {262, 230},
                {263, 230},
                {264, 230},
                {265, 230},
                {266, 230},
                {267, 230},
                {268, 230},
                {269, 230},
                {270, 230},
                {271, 230},
                {272, 230},
                {273, 230},
                {273, 229},
                {273, 228},
                {273, 227},
                {273, 226},
                {273, 225},
                {273, 224},
                {273, 223},
                {273, 222},
                {273, 221},
                {273, 220},
                {273, 219},
                {273, 218},
                {274, 218},
                {274, 217},
                {274, 216},
                {274, 215},
                {274, 214},
                {274, 213},
                {274, 212},
                {274, 211},
                {274, 210},
                {274, 209},
                {274, 208},
                {274, 207},
                {274, 206},
                {274, 205},
                {274, 204},
                {274, 203},
                {274, 202},
                {275, 202},
                {275, 201},
                {275, 200},
                {275, 199},
                {275, 198},
                {275, 197},
                {275, 196},
                {275, 195},
                {275, 194},
                {275, 193},
                {275, 192},
                {275, 191},
                {275, 190},
                {275, 189},
                {275, 188},
                {275, 187},
                {275, 186},
                {275, 185},
                {275, 184},
                {275, 183},
                {275, 182},
                {275, 181},
                {275, 180},
                {276, 180},
                {276, 179},
                {276, 178},
                {276, 177},
                {276, 176},
                {276, 175},
                {276, 174},
                {276, 173},
                {276, 172},
                {276, 171},
                {276, 170},
                {276, 169},
                {276, 168},
                {276, 167},
                {276, 166},
                {277, 166},
                {277, 165},
                {277, 164},
                {277, 163},
                {277, 162},
                {277, 161},
                {277, 160},
                {277, 159},
                {277, 158},
                {277, 157},
                {277, 156},
                {277, 155},
                {277, 154},
                {277, 153},
                {277, 152},
                {277, 151},
                {277, 150},
                {277, 149},
                {277, 148},
                {278, 148},
                {278, 147},
                {278, 146},
                {278, 145},
                {278, 144},
                {278, 143},
                {278, 142},
                {278, 141},
                {278, 140},
                {278, 139},
                {278, 138},
                {278, 137},
                {278, 136},
                {278, 135},
                {278, 134},
                {278, 133},
                {278, 132},
                {278, 131},
                {278, 130},
                {278, 129},
                {279, 129},
                {279, 128},
                {279, 127},
                {279, 126},
                {279, 125},
                {279, 124},
                {279, 123},
                {279, 122},
                {279, 121},
                {279, 120},
                {279, 119},
                {279, 118},
                {279, 117},
                {279, 116},
                {279, 115},
                {279, 114},
                {279, 113},
                {279, 112},
                {279, 111},
                {279, 110},
                {279, 109},
                {280, 109},
                {280, 108},
                {280, 107},
                {280, 106},
                {280, 105},
                {280, 104},
                {280, 103},
                {280, 102},
                {280, 101},
                {280, 100},
                {280, 99},
                {280, 98},
                {280, 97},
                {280, 96},
                {280, 95},
                {280, 94},
                {280, 93},
                {280, 92},
                {280, 91},
                {280, 90},
                {281, 90},
                {281, 89},
                {281, 88},
                {281, 87},
                {281, 86},
                {281, 85},
                {281, 84},
                {281, 83},
                {281, 82},
                {281, 81},
                {281, 80},
                {281, 79},
                {281, 78},
                {281, 77},
                {281, 76},
                {281, 75},
                {281, 74},
                {281, 73},
                {281, 72},
                {281, 71},
                {281, 70},
                {281, 69},
                {281, 68},
                {281, 67},
                {281, 66},
                {282, 66},
                {282, 65},
                {282, 64},
                {282, 63},
                {282, 62},
                {282, 61},
                {282, 60},
                {282, 59},
                {282, 58},
                {282, 57},
                {282, 56},
                {282, 55},
                {282, 54},
                {282, 53},
                {282, 52},
                {282, 51},
                {283, 51},
                {283, 50},
                {283, 49},
                {283, 48},
                {283, 47},
                {283, 46},
                {283, 45},
                {283, 44},
                {283, 43},
                {283, 42},
                {283, 41},
                {283, 40},
                {283, 39},
                {283, 38},
                {283, 37},
                {283, 36},
                {283, 35},
                {283, 34},
                {283, 33},
                {282, 33},
                {282, 32},
                {281, 32},
                {280, 32},
                {279, 32},
                {278, 32},
                {277, 32},
                {276, 32},
                {275, 32},
                {274, 32},
                {273, 32},
                {272, 32},
                {271, 32},
                {270, 32},
                {269, 32},
                {268, 32},
                {267, 32},
                {266, 32},
                {265, 32},
                {264, 32},
                {263, 32},
                {262, 32},
                {261, 32},
                {260, 32},
                {259, 32},
                {258, 32},
                {257, 32},
                {256, 32},
                {255, 32},
                {254, 32},
                {253, 32},
                {252, 32},
                {251, 32},
                {250, 32},
                {249, 32},
                {248, 32},
                {248, 31},
                {247, 31},
                {246, 31},
                {245, 31},
                {244, 31},
                {243, 31},
                {242, 31},
                {241, 31},
                {240, 31},
                {239, 31},
                {238, 31},
                {237, 31},
                {236, 31},
                {235, 31},
                {234, 31},
                {233, 31},
                {232, 31},
                {231, 31},
                {230, 31},
                {229, 31},
                {228, 31},
                {227, 31},
                {226, 31},
                {225, 31},
                {224, 31},
                {223, 31},
                {222, 31},
                {221, 31},
                {220, 31},
                {219, 31},
                {218, 31},
                {217, 31},
                {216, 31},
                {216, 30},
                {215, 30},
                {214, 30},
                {213, 30},
                {212, 30},
                {211, 30},
                {210, 30},
                {209, 30},
                {208, 30},
                {207, 30},
                {206, 30},
                {205, 30},
                {204, 30},
                {203, 30},
                {202, 30},
                {201, 30},
                {200, 30},
                {199, 30},
                {198, 30},
                {197, 30},
                {196, 30},
                {195, 30},
                {194, 30},
                {193, 30},
                {192, 30},
                {191, 30},
                {190, 30},
                {189, 30},
                {188, 30},
                {187, 30},
                {186, 30},
                {186, 29},
                {185, 29},
                {184, 29},
                {184, 30},
                {183, 30},
                {183, 29},
                {182, 29},
                {181, 29},
                {180, 29},
                {179, 29},
                {178, 29},
                {177, 29},
                {176, 29},
                {175, 29},
                {174, 29},
                {173, 29},
                {172, 29},
                {171, 29},
                {170, 29},
                {169, 29},
                {168, 29},
                {167, 29},
                {166, 29},
                {165, 29},
                {164, 29},
                {163, 29},
                {162, 29},
                {161, 29},
                {160, 29},
                {159, 29},
                {158, 29},
                {157, 29},
                {156, 29},
                {155, 29},
                {154, 29},
                {153, 29},
                {153, 28},
                {152, 28},
                {151, 28},
                {150, 28},
                {149, 28},
                {148, 28},
                {147, 28},
                {146, 28},
                {145, 28},
                {144, 28},
                {143, 28},
                {142, 28},
                {141, 28},
                {140, 28},
                {139, 28},
                {138, 28},
                {137, 28},
                {136, 28},
                {135, 28},
                {134, 28},
                {133, 28},
                {132, 28},
                {131, 28},
                {130, 28},
                {129, 28},
                {128, 28},
                {127, 28},
                {126, 28},
                {125, 28},
                {124, 28},
                {123, 28},
                {123, 27},
                {122, 27},
                {121, 27},
                {120, 27},
                {119, 27},
                {118, 27},
                {117, 27},
                {116, 27},
                {115, 27},
                {114, 27},
                {113, 27},
                {112, 27},
                {111, 27},
                {110, 27},
                {109, 27},
                {108, 27},
                {107, 27},
                {106, 27},
                {105, 27},
                {104, 27},
                {103, 27},
                {102, 27},
                {101, 27},
                {100, 27},
                {99, 27},
                {98, 27},
                {97, 27},
                {96, 27},
                {95, 27},
                {94, 27},
                {93, 27},
                {92, 27},
                {91, 27},
                {90, 27},
                {90, 28}};

        int[][] smplpoints = {
                {89, 28},
                {79, 223},
                {273, 230},
                {283, 33}};

        List<Pnt2d> poly1 = makePoly(origpoints);
        List<Pnt2d> poly2 = makePoly(smplpoints);
        double accuracyRate = 0.03;
        double tol = poly1.size() * accuracyRate;
        boolean closed = true;
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

    private static List<Pnt2d> makePoly(int[][] coords) {
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
