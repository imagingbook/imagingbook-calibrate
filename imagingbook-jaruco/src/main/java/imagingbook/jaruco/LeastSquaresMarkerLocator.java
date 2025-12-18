package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;

import java.util.ArrayList;
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

    /**
     * Constructor.
     */
    public LeastSquaresMarkerLocator() {
    }

    @Override
    public List<Pnt2d> getCorners (SegmentedContour poly) {
        QuadHomographyFit fit = new QuadHomographyFit(poly);
        double[][] A = fit.getTransformationMatrix(); // maps image quad to unit square
        ProjectiveMapping2D mapping = new ProjectiveMapping2D(A).getInverse(); // target to source mapping
        List<Pnt2d> corners = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            corners.add(mapping.applyTo(Pnt2d.from(UNIT_SQUARE_CCW[i])));
        }
        return corners;
    }

}
