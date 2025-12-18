package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.geometry.mappings.linear.Scaling2D;
import imagingbook.common.geometry.mappings.linear.Translation2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.math.Matrix;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

/**
 * Takes care of extracting the canonical marker image from the imput image.
 */
public class MarkerExtractor {

    private final ImageProcessor ip;
    private final int markerSize;

    public MarkerExtractor(ImageProcessor ip, int markerSize) {
        this.ip = ip;
        this.markerSize = markerSize;
    }

    /**
     * Extracts a square marker patch image and parses the bit pattern assuming
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
     * @param unitMapping homography from the unit square to marker quad
     * @param threshold the threshold to decide 0/1 contents
     * @return a {@link BitVector} holding the extracted bit sequence
     */
    public BitVector getMarkerBits(ProjectiveMapping2D unitMapping, int threshold) {
        ByteProcessor canonical = getCanonicalImage(unitMapping);
        return parseImage(canonical, threshold);
    }

    /**
     * @param unitMapping homography from the unit square to marker quad
     * @return
     */
    private ByteProcessor getCanonicalImage(ProjectiveMapping2D unitMapping) {
        int targetSize = 5 * (markerSize + 2);
        // ProjectiveMapping2D unitMapping = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping
        // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
        // 1: shift 1/2 pixel (in target space)
        // 2. scale from MARKER_SIZE to 1
        // 3. map to the source image
        LinearMapping2D mapping =  // shift+scale target frame to unit square, then concat with uniMapping
                new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / targetSize)).concat(unitMapping);
        ByteProcessor targetIp = new ByteProcessor(targetSize, targetSize);
        new ImageMapper(mapping).map(ip, targetIp);

        targetIp.flipVertical();    // TODO: still unclear why we have to flip here (check CCW/CW fitting)

        // listCornerMappingsUnit(new ProjectiveMapping2D(A).getInverse());
        // listCornerMappingsSized(mapping, targetSize);
        return targetIp;
    }

    private static void listCornerMappingsUnit(LinearMapping2D map) {
        System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
        System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(1,0)));
        System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(1,1)));
        System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,1)));
    }

    private static void listCornerMappingsSized(LinearMapping2D map, int n) {
        System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
        System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(n,0)));
        System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(n,n)));
        System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,n)));
    }

    // ------------------------------------------------------------------------

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
     * @param canonical the canonical image taken from the original (gray) image
     * @param threshold the threshold to decide 0/1 contents
     * @return a {@link BitVector} holding the extracted bit sequence
     */
    private BitVector parseImage(ByteProcessor canonical, int threshold) {
        // optionally wrap markerIp into an ImageAccessor to handle image borders (not strictly needed)
        // ScalarAccessor ia = ScalarAccessor.create(markerIp,
        // OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        int w = canonical.getWidth();
        int N = this.markerSize;
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitVector bits = new BitVector(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int v = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int u = (int) Math.round((1.5 + j) * d);
                int g = get3x3Median(canonical, u, v);
                if (g >= threshold) {    // use threshold from initial thresholding
                    bits.setBit(k);
                }
                k++;
            }
        }
        return bits;
    }

    private static int get3x3Median(ByteProcessor ip, int u, int v) {
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
