package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a closed, convex contour (sequence of image points) that has been
 * simplified and segmented to 4 corners and associated segments.
 */
public class QuadContour {

    private final Pnt2d[] contourPoints;
    private final int[] cornerIndexes;
    private final int N;
    private final List<Pnt2d>[] segments = new List<>[4];

    public QuadContour(Pnt2d[] contourPoints, int[]  cornerIndexes) {
        this.contourPoints = contourPoints;
        this.cornerIndexes = cornerIndexes;
        this.N = cornerIndexes.length;
        //this.segments = new List<>[4];

        List<Pnt2d> allPnts = Arrays.asList(this.contourPoints);
        segments[0] = allPnts.subList(cornerIndexes[0] + 1, cornerIndexes[1]);
        segments[1] = allPnts.subList(cornerIndexes[1] + 1, cornerIndexes[2]);
        segments[2] = allPnts.subList(cornerIndexes[2] + 1, cornerIndexes[3]);
        segments[3] = allPnts.subList(cornerIndexes[3] + 1, N);
        // use Simplifier at this stage or accept only segmented contours?
    }


    RealVector getTargetVector() {
        RealVector b = new ArrayRealVector(N + 4);
        return b;
    }

    public Pnt2d getContourPoint(int i) {
        return contourPoints[i];
    }

    public Pnt2d getCorner(int k) {
        return contourPoints[cornerIndexes[k]];
    }

    public Iterable<Pnt2d> getSegmentPoints(int k) {
        return segments[k];
    }

    public double getDistance(int segIdx, int pntIdx) { // return value t_i
        return 0;
    }


}
