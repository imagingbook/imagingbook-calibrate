package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.jaruco.math.Parabola;
import imagingbook.jaruco.math.ParabolicLineFit;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.makePolygon;
import static imagingbook.jaruco.util.Polygons.toPointArray;

/**
 * Implementation of {@link MarkerLocator} which (1) )finds an initial
 * homography by mapping marker corners to the unit square, (2) maps all contour
 * points to normalized space, (3) fits a parabola to each segment,
 * (4) find the intersections of parabola pairs  and (5) maps these back
 * as refined marker corners.
 */
public class ParabolicMarkerLocator implements MarkerLocator {

    private static final double[][] UNIT_SQUARE_CCW =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    // private static final double[][] UNIT_SQUARE_CW =    // corners of the unit square (CW)
    //         {{0,0}, {0,1}, {1,1}, {1,0}};

    /**
     * Constructor.
     */
    public ParabolicMarkerLocator() {
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

        // Fit a parabola to each segment (including the original corner points):
        Parabola.ParabolaX par0 = ParabolicLineFit.fitOverX(Arrays.asList(segments[0]), 0.5);
        Parabola.ParabolaY par1 = ParabolicLineFit.fitOverY(Arrays.asList(segments[1]), 0.5);
        Parabola.ParabolaX par2 = ParabolicLineFit.fitOverX(Arrays.asList(segments[2]), 0.5);
        Parabola.ParabolaY par3 = ParabolicLineFit.fitOverY(Arrays.asList(segments[3]), 0.5);

        // Calculate intersections between successive pairs of segments:
        Pnt2d[] ix = new Pnt2d[4];
        ix[0] = par3.getIntersection(par0, 0, 0);
        ix[1] = par0.getIntersection(par1, 1, 0);
        ix[2] = par1.getIntersection(par2, 1, 1);
        ix[3] = par2.getIntersection(par3, 0, 1);

        // Use the inverse of the original transformation to map the intersections back to real space
        ProjectiveMapping2D invMap = forwdMap.getInverse();
        // DEBUG
        Parabola[] parabolas = {par0, par1, par2, par3};
        List<List<Pnt2d>> parabCurves = new ArrayList<>();
        for (Parabola par : parabolas) {
            List<Pnt2d> curve = par.sample(-0.5, 1.5, 20);
            parabCurves.add(invMap.applyTo(curve));
        }
        Main.parabCurves = parabCurves;

        return invMap.applyTo(Arrays.asList(ix));
    }

}
