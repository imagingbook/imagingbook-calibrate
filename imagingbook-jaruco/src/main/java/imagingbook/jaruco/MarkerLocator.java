package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for calculating a marker's corner coordinates with more or
 * less precision.
 */
public class MarkerLocator {

    private static final double[][] UNIT_SQUARE_CCW =    // corners of the unit square (CCW)
            {{0,0}, {1,0}, {1,1}, {0,1}};

    // private static final double[][] UNIT_SQUARE_CW =    // corners of the unit square (CW)
    //         {{0,0}, {0,1}, {1,1}, {1,0}};


    private final ProjectiveMapping2D mapping;

    public MarkerLocator(SegmentedContour poly) {
        QuadHomographyFit fit = new QuadHomographyFit(poly);
        double[][] A = fit.getTransformationMatrix(); // maps image quad to unit square
        this.mapping = new ProjectiveMapping2D(A).getInverse(); // target to source mapping

        // System.out.println("corners1 = " + Polygons.toString(poly.getCorners()));
        // System.out.println("corners2 = " + Polygons.toString(this.getCorners()));
    }

    /**
     * Returns the image corner coordinates for the marker outline associated
     * with this {@link MarkerLocator}. The resulting corners
     * should be close to the original (detected) corners or may even be
     * identical.
     *
     * @return the corner points in image coordinates
     */
    public List<Pnt2d> getCorners () {
        List<Pnt2d> corners = new ArrayList<Pnt2d>();
        for (int i = 0; i < 4; i++) {
            corners.add(mapping.applyTo(Pnt2d.from(UNIT_SQUARE_CCW[i])));
        }
        return corners;
    }

    /**
     * Returns the projective transformation from the unit square to
     * the marker corners' image coordinates (both in CCW direction).
     * @return the projective mapping
     */
    public ProjectiveMapping2D getUnitMapping() {
        return mapping;
    }



}
