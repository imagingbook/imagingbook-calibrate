package imagingbook.jaruco.pyramid;

import ij.process.ByteProcessor;
import imagingbook.common.filter.linear.Kernel1D;
import imagingbook.common.filter.linear.LinearFilterSeparable;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.IjUtils;

import java.util.Arrays;

/**
 * Represents a simple Gaussian pyramid with 2:1 subsampling between
 * each pyramid level.
 */
public class GaussianPyramid {

    static Kernel1D kernel = new Kernel1D(new float[] {1, 4, 6, 4, 1});
    static LinearFilterSeparable filter = new LinearFilterSeparable(kernel);

    private final PyramidLevel[] levels;

    /**
     * Constructor.
     * @param ip original (input) image
     * @param levelCnt the total number of pyramid levels
     */
    public GaussianPyramid(ByteProcessor ip, int levelCnt) {
        this.levels = new PyramidLevel[levelCnt];
        buildFrom(ip);
    }

    private void buildFrom(ByteProcessor ip) {
        levels[0] = new PyramidLevel(0, (ByteProcessor) ip.duplicate());
        for (int k = 1; k < levels.length; k++) {
            levels[k] = new PyramidLevel(k, reduce(levels[k-1]));
        }
    }

    private ByteProcessor reduce(PyramidLevel prevLevel) {
        // duplicate the previous image
        ByteProcessor ii = (ByteProcessor) prevLevel.getImage().duplicate();
        // apply a Gaussian filter
        // filter.applyTo(ii);
        IjUtils.convolveXY(ii, kernel.getH());
        // subsample 2:1 and return result
        int newWidth = ii.getWidth() / 2;
        return (ByteProcessor) ii.resize(newWidth);
    }

    /**
     * Converts the level-based pixel coordinate to the corresponding
     * position in the original image.
     * @param xy
     * @param levelNo
     * @return
     */
    public Pnt2d getRealPosition(Pnt2d xy, int levelNo) {
        return null;
    }

    // ----------------------------------------------------------------------

    /**
     * Represents one level of a Gaussian pyramid.
     * Implemented as a non-static inner class of {@link GaussianPyramid}.
     */
    public class PyramidLevel {
        final int id;
        final ByteProcessor levelIp;

        PyramidLevel(int id, ByteProcessor levelIp) {
            this.id = id;
            this.levelIp = levelIp;
        }

        /**
         * Returns a reference to the containing {@link GaussianPyramid}.
         * @return the pyramid
         */
        GaussianPyramid getPyramid() {
            return GaussianPyramid.this;
        }

        ByteProcessor getImage() {
            return this.levelIp;
        }

    } // end PyramidLevel

    // ---------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("kernel = " + Arrays.toString(kernel.getH()));
    }

}
