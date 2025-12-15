package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * Represents a closed, convex contour (sequence of image points) that has been
 * simplified and segmented to 4 corners and associated segments.
 */
public class QuadContour {

    private final Pnt2d[] contourPoints;
    private final int[] cornerIndexes;

    public QuadContour(List<Pnt2d> pntList, int[]  cornerIndexes) {
        this.contourPoints = pntList.toArray(new Pnt2d[0]);
        this.cornerIndexes = cornerIndexes;
        // use Simplifier at this stage?


    }
}
