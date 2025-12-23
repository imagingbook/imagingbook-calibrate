package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;
import static imagingbook.common.math.Matrix.multiply;

/**
 * Implementation of {@link MarkerLocator} which uses a special minimum
 * least-squares fit incorporating the quad corners and all intermediate
 * contour points.
 */
public class LeastSquaresMarkerLocator implements MarkerLocator {

    private static final double[][] UNIT_SQUARE_CCW =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    // private static final double[][] UNIT_SQUARE_CW =    // corners of the unit square (CW)
    //         {{0,0}, {0,1}, {1,1}, {1,0}};

    //private final SegmentedContour poly;
    private RealMatrix A = null;		// the calculated transformation matrix
    private double err = Double.NaN;	// the calculated error

    private RealMatrix M = null;    // linear problem M a = b
    private RealVector b = null;    // keep for error calculation
    private RealVector a = null;    // transformation parameter vector

    public static final double DEFAULT_CORNER_SUPPORT = 0.05;
    private final double cornerSupport; // the interval [0,cornerSupport] where segment points are included

    /**
     * Constructor.
     */
    public LeastSquaresMarkerLocator(double cornerSupport) {
        this.cornerSupport = cornerSupport;
    }

    public LeastSquaresMarkerLocator() {
        this(DEFAULT_CORNER_SUPPORT);
    }

    @Override
    public List<Pnt2d> getCorners (SegmentedContour poly) {
        doFit(poly);
        ProjectiveMapping2D mapping =
                new ProjectiveMapping2D(A.getData()).getInverse(); // target to source mapping
        // map unit square corners to image coordinates:
        List<Pnt2d> corners = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            corners.add(mapping.applyTo(Pnt2d.from(UNIT_SQUARE_CCW[i])));
        }

        // DEBUGGING
        Main.parabCurves = new ArrayList<>();
        Main.parabCurves.add(Arrays.asList(corners.get(0), corners.get(1)));
        Main.parabCurves.add(Arrays.asList(corners.get(1), corners.get(2)));
        Main.parabCurves.add(Arrays.asList(corners.get(2), corners.get(3)));
        Main.parabCurves.add(Arrays.asList(corners.get(3), corners.get(0)));

        return corners;
    }

    // -------------------------------------------------------------------------

    private void doFit(SegmentedContour poly) {
        double[][] unitSquare = UNIT_SQUARE_CCW;
        System.out.println("cornerSupport = " + cornerSupport);
        // System.out.println("QuadHomographyFit: convexity =" + Polygons.convexity(quad.getCorners()));
        int n = poly.length();
        // set up vector b and matrix M as arrays, each with n + 4 rows:
        double[] bb = new double[n + 4]; Arrays.fill(bb, Double.NaN);
        double[][] MM = new double[n + 4][];

        List<Double> bb_list = new ArrayList<>();
        List<double[]> MM_list = new ArrayList<>();

        // Mount matrix M and vector b:
        int row = 0;    // row counter
        for (int k = 0; k < 4; k++) {   // process each of the 4 segments
            Pnt2d[] segmentCur = poly.getSegment(k);
            Pnt2d[] segmentNxt = poly.getSegment((k + 1) % 4);

            Pnt2d corner0 = segmentCur[0];  // corner at start of current segment
            Pnt2d corner1 = segmentNxt[0];  // corner at start of following segment
            LineSegment lineSegment = new LineSegment(corner0, corner1);

            // insert 2 rows for the corner (first point)
            double cx = corner0.getX();      // corner of segment k (source point)
            double cy = corner0.getY();
            double qx = unitSquare[k][0];   // 0/1 corner on unit square
            double qy = unitSquare[k][1];   // 0/1


            bb[row] = qx;   // map to unit square corner (x)
            MM[row] = new double[] { cx, cy, 1, 0, 0, 0, -qx * cx, -qx * cy };
            bb_list.add(qx);
            MM_list.add(new double[] { cx, cy, 1, 0, 0, 0, -qx * cx, -qx * cy });
            row++;
            bb[row] = qy;   // map to unit square corner (y)
            MM[row] = new double[] { 0, 0, 0, cx, cy, 1, -qy * cx, -qy * cy };
            bb_list.add(qy);
            MM_list.add(new double[] { 0, 0, 0, cx, cy, 1, -qy * cx, -qy * cy });
            row++;

            // now process the remaining points of this segment (1 row each):
            int pntCnt = 0;
            for (int i = 1; i < segmentCur.length; i++) {
                Pnt2d p = segmentCur[i];
                double px = p.getX();
                double py = p.getY();
                // TODO: calculate point's relative position on segment,
                //  omit point or assign zero weight!
                double d = lineSegment.getRelPosition(p);
                // System.out.printf(" %s %s %s  d = %.2f\n", corner0, corner1, p, d);
                double w = (d < cornerSupport || d > 1 - cornerSupport) ? 1 : 0;    // weight for point i

                if (k % 2 == 0) {       // even-numbered segment (enforcing qy)
                    bb[row] = w * qy;
                    MM[row] = multiply(w, new double[] { 0, 0, 0, px, py, 1, -qy * px, -qy * py });
                    if (w > 0) {
                        bb_list.add(qy);
                        MM_list.add(new double[]{0, 0, 0, px, py, 1, -qy * px, -qy * py});
                    }
                }
                else {                  // odd-numbered segment (enforcing qx)
                    bb[row] = w * qx;
                    MM[row] = multiply(w, new double[] { px, py, 1, 0, 0, 0, -qx * px, -qx * py });
                    if (w > 0) {
                        bb_list.add(qx);
                        MM_list.add(new double[] { px, py, 1, 0, 0, 0, -qx * px, -qx * py });
                    }
                }
                row++;
            }
        }

        // check if all rows are filled;
        for (int i = 0; i < bb.length; i++) {
            if (bb[i] == Double.NaN) {
                throw new IllegalStateException("problem in bb row " + i);
            }
            if (MM[i] == null || MM[i].length != 8) {
                throw new IllegalStateException("problem in MM row " + i);
            }

        }

        //this.M = new Array2DRowRealMatrix(MM, false);
        //this.b = new ArrayRealVector(bb, false);

        System.out.println("number of rows: " + bb_list.size());

        if (bb_list.size() != MM_list.size()) {
            throw new RuntimeException("bb not same length as MM");
        }
        this.M = new Array2DRowRealMatrix(MM_list.size(), 8);
        this.b = new ArrayRealVector(bb_list.size());
        for (int r = 0; r < MM_list.size(); r++) {
            M.setRow(r, MM_list.get(r));
            b.setEntry(r, bb_list.get(r));
        }



        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        this.a = solver.solve(b);

        // populate projective transformation matrix A from vector a
        A = MatrixUtils.createRealMatrix(3, 3);
        A.setEntry(0, 0, a.getEntry(0));
        A.setEntry(0, 1, a.getEntry(1));
        A.setEntry(0, 2, a.getEntry(2));
        A.setEntry(1, 0, a.getEntry(3));
        A.setEntry(1, 1, a.getEntry(4));
        A.setEntry(1, 2, a.getEntry(5));
        A.setEntry(2, 0, a.getEntry(6));
        A.setEntry(2, 1, a.getEntry(7));
        A.setEntry(2, 2, 1.0);

        // err = Math.sqrt(LinearFit2d.getSquaredError(P, Q, A.getData()));
    }

    @Deprecated
    static double relPosition(Pnt2d A, Pnt2d B,  Pnt2d C) {
        RealVector a = A.toRealVector();
        RealVector b = B.toRealVector();
        RealVector c = C.toRealVector();
        RealVector b_a = b.subtract(a);
        RealVector c_a = c.subtract(a);
        return c_a.dotProduct(b_a) / sqr(b_a.getNorm());
    }

    // --------------------------------------------------------

    /**
     * Helper class for repeatedly calculating the scalar projection of a point
     * to a fixed line segment, defined by endpoints {@code A} and {@code B}.
     * TODO: This could be added to {@link imagingbook.common.geometry.basic.LineSegment2d}.
     */
    static class LineSegment {
        private final double[] a, b, b_a;
        private final double normAB;

        /**
         * Constructor. Throws an exception if start and end point are identical.
         * @param A start point of the line segment
         * @param B end point of the line segment
         */
        LineSegment(Pnt2d A, Pnt2d B) {
            if (A.isCloseTo(B)) {
                throw new IllegalArgumentException("line segment with identical endpoints encountered");
            }
            this.a = A.toDoubleArray();
            this.b = B.toDoubleArray();
            this.b_a = Matrix.subtract(b, a);
            this.normAB = Matrix.normL2squared(b_a);
        }

        /**
         * Calculates the 'normalized scalar projection' of point {@code C}
         * onto this line segment and returns the relative distance
         * (interpolation factor) from the segment's start point.
         * * d=0: projection of {@code C} is exactly at point {@code A}.
         *   d=1: projection of {@code C} is exactly at point {@code B}.
         * @param C point to be projected
         * @return relative distance of projection from {@code A}
         */
        double getRelPosition(Pnt2d C) {
            return Matrix.dotProduct(Matrix.subtract(C.toDoubleArray(), a), b_a) / normAB;
        }
    }

    // --------------------------------------------------------

    public double getError() {
        if (Double.isNaN(err)) {
            // calculate error now:
            RealVector r = b.subtract(M.operate(a));
            err = r.getNorm();  // L2 norm
            /*
                for weighted LS solution:
                RealVector rw = r.ebeMultiply(sqrtW);
                double weightedNorm = rw.getNorm();
             */
        }
        return err;
    }

    // --------------------------------------------------------

    // public static void main(String[] args) {
    //     Pnt2d A = Pnt2d.from(1, 0);
    //     Pnt2d B = Pnt2d.from(15, -100);
    //     Pnt2d C = Pnt2d.from(-10, -0.5);
    //     System.out.println("d1 = " + relPosition(A, B, C));
    //
    //     LineSegment ls = new LineSegment(A, B);
    //     System.out.println("d2 = " + ls.getRelPosition(C));
    // }

}
