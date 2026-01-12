package imagingbook.jaruco.util;

import imagingbook.common.math.Matrix;

/**
 * Example JSON resources of type double[]. For testing only.
 */
public enum JsonEnumDoubleArray implements JsonResource {
    item1,
    item2
    ;

    @Override
    public Class<double[][]> getResourceClass() {
        return double[][].class;
    }

}
