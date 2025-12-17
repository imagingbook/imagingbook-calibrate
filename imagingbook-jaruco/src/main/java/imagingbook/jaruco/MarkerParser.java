package imagingbook.jaruco;

import ij.process.ByteProcessor;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

public class MarkerParser {

    private final int markerSize;

    public MarkerParser(int markerSize) {
        this.markerSize = markerSize;
    }

    /**
     * Takes a square marker patch image and extracts the bit pattern assuming
     * the marker structure specified by the current directory.
     * This is done by sampling the greyscale {@code markerPatch} image at the
     * associated N x N grid positions.
     * Each sample value is taken as the median of a 3x3 neighborhood around its
     * grid position. The size of the marker patch is assumed large enough
     * that each marker field has at least 5 x 5 pixels.
     * The median sample value for each field is then compared to
     * {@code threshold}, which is typically the threshold applied to obtain
     * the binary image for region and contour extraction.
     *
     * @param canonical
     * @param threshold
     * @return a {@link BitVector} holding the extracted bit sequence
     */
    public BitVector parseImage(ByteProcessor canonical, int threshold) {
        // optionally wrap markerIp into an ImageAccessor to handle image borders (not strictly needed)
        // ScalarAccessor ia = ScalarAccessor.create(markerIp,
        //         OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        int w = canonical.getWidth();
        int N = this.markerSize;
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitVector bits = new BitVector(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int y = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int x = (int) Math.round((1.5 + j) * d);
                int g = get3x3Median(canonical, x, y);
                if (g >= threshold) {    // use threshold from initial thresholding
                    bits.setBit(k);
                }
                k++;
            }
        }
        return bits;
    }

    static int get3x3Median(ByteProcessor ip, int u, int v) {
        // collect 3x3 values
        int[] vals = new int[9];
        int k = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                vals[k] = ip.getPixel(u - 1 + i, v - 1 + j);
                k++;
            }
        }
        Arrays.sort(vals);  // calculate median
        return vals[4];
    }
}
