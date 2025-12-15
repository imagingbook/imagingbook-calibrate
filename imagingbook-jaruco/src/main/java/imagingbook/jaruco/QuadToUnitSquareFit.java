package imagingbook.jaruco;

import imagingbook.common.geometry.fitting.points.LinearFit2d;

/**
 * A special fitter, which calculates the optimal projective transformation
 * (homography) from a segmented closed contour
 * (of type {@link QuadContour})
 * to the unit square by least-squares error minimization.
 *
 * @author WB
 * @version 2025
 */
public class QuadToUnitSquareFit implements LinearFit2d {



    @Override
    public double[][] getTransformationMatrix() {
        return new double[0][];
    }

    @Override
    public double getError() {
        return 0;
    }
}
