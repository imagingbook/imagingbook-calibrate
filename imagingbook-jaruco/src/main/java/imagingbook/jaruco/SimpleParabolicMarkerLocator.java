package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;
import imagingbook.common.geometry.basic.PolyLine2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.jaruco.math.Parabola;
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

/**
 * Implementation of {@link MarkerLocator} which (1) )finds an initial
 * homography by mapping marker corners to the unit square, (2) maps all contour
 * points to normalized space, (3) fits a parabola to each segment,
 * (4) find the intersections of parabola pairs  and (5) maps these back
 * as refined marker corners.
 */
@Deprecated
public class SimpleParabolicMarkerLocator implements MarkerLocator {

    private static final double[][] UNIT_SQUARE_CCW =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    // private static final double[][] UNIT_SQUARE_CW =    // corners of the unit square (CW)
    //         {{0,0}, {0,1}, {1,1}, {1,0}};

    /**
     * Constructor.
     */
    public SimpleParabolicMarkerLocator() {
    }

    @Override
    public Polygon2d getCandidateCorners(SegmentedPolygon poly) {
        Pnt2d[] corners = poly.getCornerPolygon().getPntList().toArray(new Pnt2d[0]);
        Pnt2d[] unitPts = PntUtils.makePntList(UNIT_SQUARE_CCW).toArray(new Pnt2d[0]);

        ProjectiveMapping2D forwdMap = ProjectiveMapping2D.fromPoints(corners, unitPts);

        // Copy all contour points and map to normalized space
        Pnt2d[][] segments = new Pnt2d[4][];
        for (int k = 0; k < 4; k++) {
            segments[k] = poly.getSegment(k);
            for (int i = 0; i < segments[k].length; i++) {
                segments[k][i] = forwdMap.applyTo(segments[k][i]);
            }
        }

        // Fit a parabola to each segment (including the original corner points):
        Parabola.ParabolaX par0 = fitOverX(Arrays.asList(segments[0]), 0.5);
        Parabola.ParabolaY par1 = fitOverY(Arrays.asList(segments[1]), 0.5);
        Parabola.ParabolaX par2 = fitOverX(Arrays.asList(segments[2]), 0.5);
        Parabola.ParabolaY par3 = fitOverY(Arrays.asList(segments[3]), 0.5);

        // Calculate intersections between successive pairs of segments:
        Pnt2d[] ix = new Pnt2d[4];
        ix[0] = par3.getIntersection(par0, 0, 0);
        ix[1] = par0.getIntersection(par1, 1, 0);
        ix[2] = par1.getIntersection(par2, 1, 1);
        ix[3] = par2.getIntersection(par3, 0, 1);

        // Use the inverse of the original transformation to map the intersections back to real space
        ProjectiveMapping2D invMap = forwdMap.getInverse();
        // DEBUG --------------------------------------------
        Parabola[] parabolas = {par0, par1, par2, par3};
        List<PolyLine2d> parabCurves = new ArrayList<>();
        for (Parabola par : parabolas) {
            List<Pnt2d> curve = par.sample(-0.5, 1.5, 20);
            parabCurves.add(new PolyLine2d(invMap.applyTo(curve)));
        }
        Main.parabCurves = parabCurves;
        // ----------------------------------------------------
        return new Polygon2d(invMap.applyTo(Arrays.asList(ix)));
    }

    // Parabola fitting: -------------------------------------------------------

    /**
     * Fit a parabola over x (vertical axis positioned at x=d) to given points.
     * @param pts point coordinates {@code (xi,yi)}
     * @param xd x-position of vertical parabola axis
     * @return @return a {@link Parabola.ParabolaX} instance
     */
    public static Parabola.ParabolaX fitOverX(List<Pnt2d> pts, double xd) {
        double[][] XY = PntUtils.toXYArray(pts);
        double[] ac = doFit(XY[0], XY[1], xd);
        return new Parabola.ParabolaX(ac[0], ac[1], xd);
    }

    /**
     * Fit a parabola over y (horizontal axis positioned at y=d) to given points.
     * @param pts point coordinates {@code (xi,yi)}
     * @param yd y-position of horizontal parabola axis
     * @return a {@link Parabola.ParabolaY} instance
     */
    public static Parabola.ParabolaY fitOverY(List<Pnt2d> pts, double yd) {
        double[][] XY = PntUtils.toXYArray(pts);
        double[] ac = doFit(XY[1], XY[0], yd);  // swap X/Y
        return new Parabola.ParabolaY(ac[0], ac[1], yd);
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
    private static double[] doFit(double[] X, double[] Y, double d) {
        int m = X.length;
        if (m < 3) {
            throw new IllegalArgumentException("Not enough points to fit parabola: " + m);
        }
        RealMatrix M = new Array2DRowRealMatrix(m, 2);
        RealVector b = new ArrayRealVector(m);

        for (int r = 0; r < m; r++) {
            M.setEntry(r, 0, sqr(X[r] - d));
            M.setEntry(r, 1, 1);
            b.setEntry(r, Y[r]);
        }
        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        return solver.solve(b).toArray();    // ac = (a, c) parabola parameters
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        List<Pnt2d> polyX = PntUtils.makePntList(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
        Parabola.ParabolaX fitX = fitOverX(polyX, 0.5);
        System.out.println("fitX = " + fitX);

        List<Pnt2d> polyY = PntUtils.makePntList(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
        Parabola.ParabolaY fitY = fitOverY(polyY, 0.5);
        System.out.println("fitY = " + fitY);

    }

}
