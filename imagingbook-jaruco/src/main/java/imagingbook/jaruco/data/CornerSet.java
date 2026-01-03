package imagingbook.jaruco.data;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PntUtils;

public abstract class CornerSet {

    private double[][] corners;

    protected CornerSet() {
    }

    protected abstract double[][] initCorners();

    public double[][] getCorners() {
        if (corners == null) {
            corners = initCorners();
        }
        return corners;
    }

    public Pnt2d[] getCornerPoints() {
        return PntUtils.fromDoubleArray(getCorners());
    }
}
