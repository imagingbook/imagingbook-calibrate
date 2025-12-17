package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

public class MarkerChecker {

    private final ImageProcessor ip;
    private final ArucoDictionary dictionary;



    public MarkerChecker(ImageProcessor ip, ArucoDictionary dictionary) {
        this.ip = ip;
        this.dictionary = dictionary;
    }

    public MarkerDetectionResult checkMarker(ByteProcessor canonical, int threshold) {
        // STEP 4c - sample marker fields to generate the 1D marker pattern
        BitVector sampleBits = extractMarkerBits(canonical, threshold);

        double maxCorrectionRate = 1.0; // TODO: CHECK!!!
        ArucoDictionary.LookupResult lookup =
                dictionary.lookup(sampleBits, maxCorrectionRate);

        // rotation - how to recalculate the mapping?



        // if (lookup != null) {
        //     // Collections.rotate(markerOutline.polygon, lookup.rotation);
        //     markerOutline.rotatePolygon(lookup.rotation);   // rotate vertices to canonical state
        //     return new MarkerDetectionResult(lookup, markerOutline, null);   // TODO: rejectedPoints?
        // }
        // else {
        //     return null;
        // }
        return null;
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
     * @param markerPatch
     * @param threshold
     * @return a {@link BitVector} holding the extracted bit sequence
     */
    BitVector extractMarkerBits(ByteProcessor markerPatch, int threshold) {
        // optionally wrap markerIp into an ImageAccessor to handle image borders (not strictly needed)
        // ScalarAccessor ia = ScalarAccessor.create(markerIp,
        //         OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        int w = markerPatch.getWidth();
        int N = dictionary.getMarkerSize();
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitVector bits = new BitVector(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int y = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int x = (int) Math.round((1.5 + j) * d);
                int g = get3x3Median(markerPatch, x, y);
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
