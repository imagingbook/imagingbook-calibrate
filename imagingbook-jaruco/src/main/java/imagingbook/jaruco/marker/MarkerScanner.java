package imagingbook.jaruco.marker;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.dict.ArucoDictionary;

import java.util.Arrays;

/**
 * Extracts the normalized (canonical) marker image from the input image and
 * reads the marker bits by sampling that image.
 */
public class MarkerScanner {

    private final ImageProcessor ip;    // the input image
    private final int markerSize;       // the number of marker data fields in each direction
    private final int targetSize;       // the size of the canonical image (5x5 pixels for each marker field)
    private final int fieldWidth;       // bits per marker field (fixed)
    private final int sampleOffset;     // where to start sampling
    private final Pnt2d[] targetPts;    // the exact rectangle to map the marker quad to

    /**
     * Constructor.
     * @param ip the image to read markers from
     * @param dictionary the {@link ArucoDictionary} for looking up bitcodes
     */
    public MarkerScanner(ImageProcessor ip, ArucoDictionary dictionary) {
        this.ip = ip;
        this.markerSize = dictionary.getMarkerSize();
        this.fieldWidth = 5;    // each data field has 5x5 pixels
        this.targetSize = fieldWidth * (markerSize + 2); // fields with 5x5 pixels (parameter!?)
        this.sampleOffset = 3 * fieldWidth / 2;
        double d = 0.5;     // enlarge target square by 1/2 pixel
        this.targetPts = new Pnt2d[] {
                Pnt2d.from(0, 0).plus(-d, -d),
                Pnt2d.from(0, targetSize-1).plus(-d, d),
                Pnt2d.from(targetSize-1, targetSize-1).plus(d, d),
                Pnt2d.from(targetSize-1, 0).plus(d, -d)};
        // TODO: pre-calculate sample raster positions?
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
     * @param poly the corners of the marker quad (image coordinates)
     * @param threshold the threshold to decide 0/1 field contents
     * @return a {@link BitVector} holding the extracted bit pattern
     */
    public BitVector getMarkerData(Polygon2d poly, int threshold) {
        Pnt2d[] sourcePts = poly.getPntList().toArray(new Pnt2d[0]);
        // calculate homography mapping (from target to source):
        ProjectiveMapping2D hom = ProjectiveMapping2D.fromPoints(targetPts, sourcePts);
        ByteProcessor canonicalIm = new ByteProcessor(targetSize, targetSize);
        new ImageMapper(hom).map(ip, canonicalIm);
        return parseImage(canonicalIm, threshold);
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
        BitVector bits = new BitVector(markerSize * markerSize);
        int k = 0;  // TODO: precalculate sample positions?
        for (int j = 0; j < markerSize; j++) {   // vertical loop
            int v = sampleOffset + j * fieldWidth;
            for (int i = 0; i < markerSize; i++) {   // horizontal loop
                int u = sampleOffset + i * fieldWidth;
                int g = get3x3Median(canonical, u, v);
                if (g >= threshold) {    // use threshold from initial thresholding
                    bits.setBit(k);
                }
                k++;
            }
        }
        return bits;
    }

    /**
     * Calculates the median gray value in a 3x3 image neighborhood centered at
     * {@code u}, {@code v}.
     * @param ip the image
     * @param u position  in x
     * @param v position in y
     * @return the median value
     */
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
        return vals[4];     // the mid value
    }

}
