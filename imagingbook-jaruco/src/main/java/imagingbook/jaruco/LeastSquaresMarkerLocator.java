package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import org.apache.commons.math4.legacy.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Constructor.
     */
    public LeastSquaresMarkerLocator() {
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
        return corners;
    }

    // -------------------------------------------------------------------------

    private void doFit(SegmentedContour poly) {
        double[][] unitSquare = UNIT_SQUARE_CCW;
        // System.out.println("QuadHomographyFit: convexity =" + Polygons.convexity(quad.getCorners()));
        int n = poly.length();
        // set up vector b and matrix M as arrays, each with n + 4 rows:
        double[] bb = new double[n + 4]; Arrays.fill(bb, Double.NaN);
        double[][] MM = new double[n + 4][];

        // corner 0 -> (0,0)
        // corner 1 -> (1,0)
        // corner 2 -> (1,1)
        // corner 3 -> (0,1)

        // Mount matrix M and vector b:
        int row = 0;    // row counter
        for (int k = 0; k < 4; k++) {   // process each of the 4 segments
            Pnt2d[] segmentPnts = poly.getSegment(k);

            // insert 2 rows for the corner (first point)
            double px = segmentPnts[0].getX();      // corner of segment k (source point)
            double py = segmentPnts[0].getY();
            double qx = unitSquare[k][0];  // target point on unit square
            double qy = unitSquare[k][1];

            bb[row] = qx;
            MM[row] = new double[] { px, py, 1, 0, 0, 0, -qx * px, -qx * py };
            row++;
            bb[row] = qy;
            MM[row] = new double[] { 0, 0, 0, px, py, 1, -qy * px, -qy * py };
            row++;

            // now process the remaining points of this segment (1 row each):
            for (int i = 1; i < segmentPnts.length; i++) {
                px = segmentPnts[i].getX();
                py = segmentPnts[i].getY();
                if (k % 2 == 0) {       // even-numbered segment (enforcing qy)
                    bb[row] = qy;
                    MM[row] = new double[] { 0, 0, 0, px, py, 1, -qy * px, -qy * py };
                }
                else {                  // odd-numbered segment (enforcing qx)
                    bb[row] = qx;
                    MM[row] = new double[] { px, py, 1, 0, 0, 0, -qx * px, -qx * py };
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

        this.M = new Array2DRowRealMatrix(MM, false);
        this.b = new ArrayRealVector(bb, false);

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

}
