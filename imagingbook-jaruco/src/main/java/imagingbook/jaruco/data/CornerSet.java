package imagingbook.jaruco.data;

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
}
