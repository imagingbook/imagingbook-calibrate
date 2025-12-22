package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.jaruco.math.Parabola;
import imagingbook.jaruco.util.Polygons;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;
import static imagingbook.jaruco.util.Polygons.makePolygon;
import static imagingbook.jaruco.util.Polygons.toPointArray;
import static imagingbook.jaruco.util.Polygons.toXYArray;

/**
 * Uses piecewise quadratic approximation (pair of parabolas)!
 * Implementation of {@link MarkerLocator} which (1) )finds an initial
 * homography by mapping marker corners to the unit square, (2) maps all contour
 * points to normalized space, (3) fits a parabola to each segment,
 * (4) find the intersections of parabola pairs  and (5) maps these back
 * as refined marker corners.
 */
public class ParabolicMarkerLocator2 implements MarkerLocator {

    private static final double[][] UNIT_SQUARE_CCW =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    // private static final double[][] UNIT_SQUARE_CW =    // corners of the unit square (CW)
    //         {{0,0}, {0,1}, {1,1}, {1,0}};

    /**
     * Constructor.
     */
    public ParabolicMarkerLocator2() {
    }

    @Override
    public List<Pnt2d> getCorners (SegmentedContour poly) {
        Pnt2d[] corners = toPointArray(poly.getCorners());
        Pnt2d[] unitPts = toPointArray(makePolygon(UNIT_SQUARE_CCW));

        ProjectiveMapping2D forwdMap = ProjectiveMapping2D.fromPoints(corners, unitPts);

        // Copy all contour points and map to normalized space
        Pnt2d[][] segments = new Pnt2d[4][];
        for (int k = 0; k < 4; k++) {
            segments[k] = poly.getSegment(k);
            for (int i = 0; i < segments[k].length; i++) {
                segments[k][i] = forwdMap.applyTo(segments[k][i]);
            }
        }

        // Fit a pair of parabolas to each segment (including the original corner points):
        // [0] is the L-parabola, [1] is the R-parabola
        Parabola.ParabolaX[] par0 = fitOverX(Arrays.asList(segments[0]), 0.5);
        Parabola.ParabolaY[] par1 = fitOverY(Arrays.asList(segments[1]), 0.5);
        Parabola.ParabolaX[] par2 = fitOverX(Arrays.asList(segments[2]), 0.5);
        Parabola.ParabolaY[] par3 = fitOverY(Arrays.asList(segments[3]), 0.5);

        // Calculate intersections between successive pairs of segments:
        Pnt2d[] ix = new Pnt2d[4];
        int L = 0, R = 1;
        ix[0] = par3[L].getIntersection(par0[L], 0, 0);
        ix[1] = par0[R].getIntersection(par1[L], 1, 0);
        ix[2] = par1[R].getIntersection(par2[R], 1, 1);
        ix[3] = par2[L].getIntersection(par3[R], 0, 1);

        // Use the inverse of the original transformation to map the intersections back to real space
        ProjectiveMapping2D invMap = forwdMap.getInverse();

        // DEBUG
        Parabola[][] parabolas = {par0, par1, par2, par3};
        List<List<Pnt2d>> parabCurves = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            Parabola parL = parabolas[k][0];
            parabCurves.add(invMap.applyTo(parL.sample(-0.5, 1.5, 20)));
            Parabola parR = parabolas[k][1];
            parabCurves.add(invMap.applyTo(parR.sample(-0.5, 1.5, 20)));
        }

        Main.parabCurves = parabCurves;
        return invMap.applyTo(Arrays.asList(ix));
    }

    // Parabola fitting: -------------------------------------------------------

    record PiecewiseParabola(double aL, double aR, double c, double d) {}

    /**
     * Fit a parabola over x (vertical axis positioned at x=d) to given points.
     * @param pts point coordinates {@code (xi,yi)}
     * @param xd x-position of vertical parabola axis
     * @return @return a {@link Parabola.ParabolaX} instance
     */
    public static Parabola.ParabolaX[] fitOverX(List<Pnt2d> pts, double xd) {
        double[][] XY = toXYArray(pts);
        PiecewiseParabola pp = doPiecewiseFit(XY[0], XY[1], xd);
        return new Parabola.ParabolaX[]{
                new Parabola.ParabolaX(pp.aL, pp.c, pp.d),
                new Parabola.ParabolaX(pp.aR, pp.c, pp.d)
        };
    }

    /**
     * Fit a parabola over y (horizontal axis positioned at y=d) to given points.
     * @param pts point coordinates {@code (xi,yi)}
     * @param yd y-position of horizontal parabola axis
     * @return a {@link Parabola.ParabolaY} instance
     */
    public static Parabola.ParabolaY[] fitOverY(List<Pnt2d> pts, double yd) {
        double[][] XY = toXYArray(pts);
        PiecewiseParabola pp = doPiecewiseFit(XY[1], XY[0], yd);  // swap X/Y
        return new Parabola.ParabolaY[]{
                new Parabola.ParabolaY(pp.aL, pp.c, pp.d),
                new Parabola.ParabolaY(pp.aR, pp.c, pp.d)
        };
    }

    /**
     * Fit a parabola over x (with vertical axis positioned at x=d) to given
     * point coordinates. In this case, yi = f(xi) is assumed, i.e., {@code X} are the
     * independent coordinates, {@code Y} are the dependent coordinates.
     * The parabola's axis is perpendicular to the independent coordinate axis.
     * Parameter {@code d} marks the position of the parabola on the dependent
     * axis.
     * To perform a fit to a rotated parabola just swap the X/Y coordinate
     * arrays.
     *
     * @param X independent coordinates
     * @param Y dependent coordinates
     * @param d position of the parabola's axis
     * @return
     */
    private static PiecewiseParabola doPiecewiseFit(double[] X, double[] Y, double d) {
        int m = X.length;
        if (m < 3) {
            throw new IllegalArgumentException("Not enough points to fit parabola: " + m);
        }
        RealMatrix M = new Array2DRowRealMatrix(m, 3);
        RealVector b = new ArrayRealVector(m);

        for (int r = 0; r < m; r++) {
            if (X[r] < d) {
                M.setEntry(r, 0, sqr(X[r] - d));    // = Li
                M.setEntry(r, 1, 0);
            }
            else {
                M.setEntry(r, 0, 0);
                M.setEntry(r, 1, sqr(X[r] - d));    // = Ri
            }
            M.setEntry(r, 2, 1);
            b.setEntry(r, Y[r]);
        }
        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        double[] aac = solver.solve(b).toArray();    // ac = (a, c) parabola parameters
        double aL = aac[0];
        double aR = aac[1];
        double c = aac[2];
        System.out.println(new PiecewiseParabola(aL, aR, c, d));
        return new PiecewiseParabola(aL, aR, c, d);
    }

    // -----------------------------------------------------------------------

    // public static void main(String[] args) {
    //     List<Pnt2d> polyX = Polygons.makePolygon(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
    //     Parabola.ParabolaX fitX = fitOverX(polyX, 0.5);
    //     System.out.println("fitX = " + fitX);
    //
    //     List<Pnt2d> polyY = Polygons.makePolygon(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
    //     Parabola.ParabolaY fitY = fitOverY(polyY, 0.5);
    //     System.out.println("fitY = " + fitY);
    //
    // }

}
