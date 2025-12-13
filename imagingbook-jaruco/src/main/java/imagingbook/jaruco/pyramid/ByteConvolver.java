package imagingbook.jaruco.pyramid;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.image.OutOfBoundsStrategy;
import imagingbook.common.image.access.ByteAccessor;
import imagingbook.common.image.interpolation.InterpolationMethod;
import imagingbook.common.math.Matrix;

import static imagingbook.common.math.Arithmetic.sqr;

@Deprecated // useless!
public class ByteConvolver {

    private final float[] kernel;
    private final float scale;
    private final int offset;       // center idx of kernel

    public ByteConvolver(float[] kernel) {
        if (kernel.length < 3 || kernel.length % 2 == 0) {
            throw new IllegalArgumentException("kernel size must be odd >= 3");
        }
        this.kernel = kernel;
        this.scale = (float) (1.0 / get2DkernelSum(kernel));
        this.offset = kernel.length / 2;
    }

    private static double get2DkernelSum(float[] kernel) {
        double s1 = Matrix.sum(kernel);
        double s2 = 0;
        for (int i = 0 ; i < kernel.length ; i++) {
            s2 += kernel[i] * s1;
        }
        System.out.println("sum = " + s2);
        return s2;
    }

    public ByteProcessor convolve(ByteProcessor ip1, int iterations) {
        final int width = ip1.getWidth();
        final int height = ip1.getHeight();
        ByteProcessor ip2 = (ByteProcessor) ip1.duplicate();

        ByteAccessor ba1 = new
                ByteAccessor(ip1, OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor, 0, 0);
        ByteAccessor ba2 = new
                ByteAccessor(ip2, OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor, 0, 0);

        // visit all pixels
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                // 1D local convolution for pixel (u,v)
                float sum = 0;
                // horizontal pass
                for (int i = 0; i < kernel.length; i++) {
                    sum = sum + ba1.getVal(u - offset + i, v) * kernel[i];
                }
                // vertical pass
                for (int j = 0; j < kernel.length; j++) {
                    sum = sum + ba1.getVal(u, v - offset + j) * kernel[j];
                }
                // set value in input image
                ba2.setVal(u, v, sum * scale);
            }
        }
        return ip2;
    }

    // ------------------------------------------------------------------------
    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";
    static String SAMPLE_IMAGE = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
    static final float[] H = {1, 4, 6, 4, 1};

    public static void main(String[] args) {

        ImagePlus im = IjUtils.openImage(SAMPLE_IMAGE);
        // im.show();
        ByteProcessor ip = im.getProcessor().convertToByteProcessor();
        ByteProcessor ip1 = (ByteProcessor) ip.resize(1000);
        new ImagePlus("input", ip1).show();

        ByteConvolver convolver = new ByteConvolver(H);
        System.out.println("scale = " + convolver.scale);

        ByteProcessor ip2 = convolver.convolve(ip1, 1);
        new ImagePlus("result", ip2).show();

    }
}
