package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a closed, convex 4-corner polygon extracted from a sequence of image
 * points. The original (full set) of contour points is preserved for quad
 * fitting.
 */
public class QuadContour {

    private Pnt2d[] contourPoints;
    private int[] cornerIndexes;
    private int N;
    private List<Pnt2d>[] segments = null; //new List<>[4];

    /**
     * Constructor.
     * @param contour the original sequence of contour points
     * @param cornerIndexes the indexes of the corners in {@code contourPoints}
     */
    public QuadContour(List<Pnt2d> contour, int[] cornerIndexes) {
        this.contourPoints = contour.toArray(new Pnt2d[0]);
        this.cornerIndexes = cornerIndexes;
        this.N = contourPoints.length;

        List<Pnt2d> allPnts = Arrays.asList(this.contourPoints);
        segments[0] = allPnts.subList(cornerIndexes[0] + 1, cornerIndexes[1]);
        segments[1] = allPnts.subList(cornerIndexes[1] + 1, cornerIndexes[2]);
        segments[2] = allPnts.subList(cornerIndexes[2] + 1, cornerIndexes[3]);
        segments[3] = allPnts.subList(cornerIndexes[3] + 1, N);
        // use Simplifier at this stage or accept only segmented contours?
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
