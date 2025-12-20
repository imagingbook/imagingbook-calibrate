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

/**
 *  Tries to fit parabolic surve to a list of 2D points, either along
 *  the horizontal or vertical axis in interval [0,1].
 *
 *  At the moment, the line points are assumed to be scaled to the
 *  horizontal segment (0,0) - (1,0).
 */
public class ParabolicLineFit {

    public ParabolicLineFit() {
    }

    /**
     * Fit a parabola over x (vertical axis positioned at x=d) to given points,
     * assumed to be mapped to the interval x = [0,..,1].
     * @param pts (xi,yi), with xi in [0,..,1]
     * @return
     */
    public static Parabola.OverX fitOverX(List<Pnt2d> pts, double d) {
        int m = pts.size();
        if (m < 3) {
            throw new IllegalArgumentException("Not enough points to fit parabola: " + m);
        }
        RealMatrix M = new Array2DRowRealMatrix( m, 2);
        RealVector b = new ArrayRealVector(m);

        for (int r = 0; r < m; r++) {
            Pnt2d p = pts.get(r);
            M.setEntry(r, 0, sqr(p.getX() - d));
            M.setEntry(r, 1, 1);
            b.setEntry(r, p.getY());
        }
        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        double[] ac = solver.solve(b).toArray();    // ac = (a, c) parabola parameters
        return new Parabola.OverX(ac[0], ac[1], d);
    }

    /**
     * Fit a parabola over y (horizontal axis positioned at y=d) to given points,
     * assumed to be mapped to the interval y = [0,..,1].
     * @param pts (xi,yi), with yi in [0,..,1]
     * @return
     */
    public static Parabola.OverY fitOverY(List<Pnt2d> pts, double d) {
        int m = pts.size();
        if (m < 3) {
            throw new IllegalArgumentException("Not enough points to fit parabola: " + m);
        }
        RealMatrix M = new Array2DRowRealMatrix( m, 2);
        RealVector b = new ArrayRealVector(m);

        for (int r = 0; r < m; r++) {
            Pnt2d p = pts.get(r);
            M.setEntry(r, 0, sqr(p.getY() - d));
            M.setEntry(r, 1, 1);
            b.setEntry(r, p.getX());
        }
        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        double[] ac = solver.solve(b).toArray();    // ac = (a, c) parabola parameters
        return new Parabola.OverY(ac[0], ac[1], d);
    }


    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        List<Pnt2d> polyX = Polygons.makePolygon(0, 0, 0.25, 0.5, 0.5, 0.75, 0.2, 1.0, 1, 0);  // (x,y)
        Parabola.OverX fitX = fitOverX(polyX, 0.5);
        System.out.println("fitX = " + fitX);

        List<Pnt2d> polyY = Polygons.makePolygon(0, 0, 0.5, 0.25, 0.75, 0.5, 1.0, 0.2, 0, 1);  // (y,x)
        Parabola.OverY fitY = fitOverY(polyY, 0.5);
        System.out.println("fitY = " + fitY);

    }
}
