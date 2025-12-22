package imagingbook.jaruco.math;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.util.Polygons;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.List;

import static imagingbook.common.math.Arithmetic.sqr;
import static imagingbook.jaruco.util.Polygons.toXYArray;

/**
 *  Tries to fit parabolic surve to a list of 2D points, either along
 *  the horizontal or vertical axis in interval [0,1].
 *
 *  At the moment, the line points are assumed to be scaled to the
 *  horizontal segment (0,0) - (1,0).
 */
@Deprecated
public class ParabolicLineFit {

    public ParabolicLineFit() {
    }

    /**
     * Fit a parabola over x (vertical axis positioned at x=d) to given points.
     * @param pts point coordinates {@code (xi,yi)}
     * @param xd x-position of vertical parabola axis
     * @return @return a {@link Parabola.ParabolaX} instance
     */
    public static Parabola.ParabolaX fitOverX(List<Pnt2d> pts, double xd) {
        double[][] XY = toXYArray(pts);
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
        double[][] XY = toXYArray(pts);
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
        List<Pnt2d> polyX = Polygons.makePolygon(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
        Parabola.ParabolaX fitX = fitOverX(polyX, 0.5);
        System.out.println("fitX = " + fitX);

        List<Pnt2d> polyY = Polygons.makePolygon(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
        Parabola.ParabolaY fitY = fitOverY(polyY, 0.5);
        System.out.println("fitY = " + fitY);

    }
}
