package imagingbook.jaruco.pyramid;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import imagingbook.common.corners.GradientCornerDetector;
import imagingbook.common.corners.ShiTomasiCornerDetector;
import imagingbook.common.filter.linear.Kernel1D;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.IjUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents a simple Gaussian pyramid with 2:1 subsampling between
 * each pyramid level.
 */
public class GaussianPyramid {

    static boolean ADD_CORNERS = true;

    /*
    It’s cheap to compute (integer arithmetic possible) and is exactly the 4th
    row of Pascal’s triangle — i.e. convolving twice with [1,2,1]/4 yields this kernel:
     */
    static final float[] H = Kernel1D.normalize(new float[] {1, 4, 6, 4, 1});

    public int getLevelCount() {
        return levels.length;
    }

    public PyramidLevel getLevel(int level) {
        return levels[level];
    }

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
            levels[k] = levels[k - 1].createNext();
        }
        if (ADD_CORNERS) {
            for (int k = 0; k < levels.length; k++) {
                levels[k].makeCorners();
            }
        }
    }

    /**
     * Calculates the original image position (x, y) for the specified in-level
     * position (u, v).
     * @param levelPos point in local level coordinates
     * @param level pyramid level index
     * @return the corresponding original image position
     */
    public Pnt2d getOriginalPosition(Pnt2d levelPos, int level) {
        double scale = getLevel(level).scale;
        double x = levelPos.getX() / scale;
        double y = levelPos.getY() / scale;
        return Pnt2d.from(x, y);
    }

    /**
     * Calculates the in-level position (u, v) that corresponds to the specified
     * position in the original image (x, y).
     * @param origPos
     * @param level
     * @return the corresponding in-level position
     */
    public Pnt2d getLevelPosition(Pnt2d origPos, int level) {
        double scale = getLevel(level).scale;
        double u = origPos.getX() * scale;
        double v = origPos.getY() * scale;
        return Pnt2d.from(u, v);
    }

    // ----------------------------------------------------------------------

    /**
     * Represents one level of a Gaussian pyramid.
     * Implemented as a non-static inner class of {@link GaussianPyramid}.
     */
    public class PyramidLevel {
        final int id;
        final int width, height;
        final double scale;
        final ByteProcessor levelIp;
        FloatProcessor cornersIp = null;

        public int getId() {
            return id;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public double getScale() {
            return scale;
        }

        public ByteProcessor getImage() {
            return levelIp;
        }

        public FloatProcessor getCornerScore() {
            return cornersIp;
        }

        public GaussianPyramid getPyramid() {
            return GaussianPyramid.this;
        }

        private PyramidLevel(int id, ByteProcessor levelIp) {
            this.id = id;
            this.width = levelIp.getWidth();
            this.height = levelIp.getHeight();
            this.scale = Math.pow(2, -id);
            this.levelIp = levelIp;
        }

        private PyramidLevel createNext() {
            ByteProcessor tmpIp = (ByteProcessor) levelIp.duplicate();
            // apply a Gaussian filter
            // IjUtils.convolveXY(tmpIp, H);
            Convolver conv = new Convolver();   // TODO: find more efficient (integer) implementation?
            // NOTE: We only use every other pixel, i.e., 1/4th of all spixels
            // in the smoothes image. Why calculate the rest too?
            conv.setNormalize(false);   // kernel H is already normalized
            conv.convolve(tmpIp, H, H.length, 1);
            conv.convolve(tmpIp, H, 1, H.length);
            // subsample 2:1
            ByteProcessor nextIp = decimate(tmpIp);
            return new PyramidLevel(id + 1, nextIp);
        }

        private void makeCorners() {
            GradientCornerDetector.Parameters params = new GradientCornerDetector.Parameters();
            for (int k = 0; k < levels.length; k++) {
                GradientCornerDetector cornerDetector = new ShiTomasiCornerDetector(levelIp, params);
                this.cornersIp = cornerDetector.getQ();
            }
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%s[id=%d w=%d, h=%d, scale=%.6f]",
                    this.getClass().getSimpleName(), id, width, height, scale);
        }

    } // end PyramidLevel

    /**
     * Decimate the image given in {@code ip1} 2:1 by subsampling every
     * other pixel (without any interpolation).
     * The input image is not modified.
     * @param ip1 the input image
     * @return the decimated image
     */
    static ByteProcessor decimate(ByteProcessor ip1) {
        final int w1 = ip1.getWidth();
        final int h1 = ip1.getHeight();
        final int w2 = w1 / 2;
        final int h2 = h1 / 2;
        ByteProcessor ip2 = new ByteProcessor(w2, h2);
        // resample data:
        for (int v2 = 0 ; v2 < h2; v2++) {
            int v1 = 2 * v2;
            for (int u2 = 0 ; u2 < w2; u2++) {
                int u1 = 2 * u2;
                ip2.putPixel(u2, v2, ip1.getPixel(u1, v1));
            }
        }
        return ip2;
    }

    // ---------------------------------------------------------

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";
    // static String SAMPLE_IMAGE = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
    static String SAMPLE_IMAGE = SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg";

    public static void main(String[] args) {
        System.out.println("kernel = " + Arrays.toString(H));
        ImagePlus im = IjUtils.openImage(SAMPLE_IMAGE);
        //im.show();
        ByteProcessor ip = im.getProcessor().convertToByteProcessor();
        // ip = (ByteProcessor) ip.resize(ip.getWidth() - 1);

        int K = 5;
        GaussianPyramid pyramid = new GaussianPyramid(ip, K);
        for (int k = 0; k < K; k++) {
            new ImagePlus("Level" + k, pyramid.getLevel(k).getImage()).show();
            if (pyramid.getLevel(k).getCornerScore() != null)
                new ImagePlus("Corners" + k, pyramid.getLevel(k).getCornerScore()).show();
            System.out.println(pyramid.getLevel(k).toString());
        }

    }

}
