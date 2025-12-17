package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.points.LinearFit2d;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

/**
 * A special fitter, which calculates the optimal projective transformation
 * (homography) from a segmented closed contour
 * (of type {@link SegmentedContour})
 * to the unit square by least-squares error minimization.
 *
 * @author WB
 * @version 2025
 */
public class QuadHomographyFit implements LinearFit2d {

    private static final double[][] UNIT_SQUARE =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    private final SegmentedContour quad;
    private RealMatrix A = null;		// the calculated transformation matrix
    private double err = Double.NaN;		    // the calculated error

    private RealMatrix M = null;    // linear problem M a = b
    private RealVector b = null;    // keep for error calculation
    private RealVector a = null;

    // TODO: currently no weighting, add point weighting policy
    public QuadHomographyFit(SegmentedContour quad) {
        this.quad = quad;
        doFit();
    }

    // -------------------------------------------------------------------------

    private void doFit() {
        int n = quad.length();
        // set up vector b and matrix M as arrays, each with n + 4 rows:
        double[] bb = new double[n + 4];
        double[][] MM = new double[n + 4][];

        // corner 0 -> (0,0)
        // corner 1 -> (1,0)
        // corner 2 -> (1,1)
        // corner 3 -> (0,1)

        // process each of the 4 segments
        int row = 0;    // row counter
        for (int k = 0; k < n; k++) {
            Pnt2d[] segmentPnts = quad.getSegment(k);

            // insert 2 rows for the corner (first point)
            double px = segmentPnts[0].getX();      // corner of segment k (source point)
            double py = segmentPnts[0].getY();
            double qx = UNIT_SQUARE[k][0];  // target point on unit square
            double qy = UNIT_SQUARE[k][1];

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

            this.M = MatrixUtils.createRealMatrix(MM);
            this.b = MatrixUtils.createRealVector(bb);

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
    }



    // --------------------------------------------------------

    @Override
    public double[][] getTransformationMatrix() {
        return new double[0][];
    }

    @Override
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
