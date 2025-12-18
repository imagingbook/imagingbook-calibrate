package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.gui.ZoomableImagePlus;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts the normalized (canonical) marker image from the input image and
 * reads the marker bits by scanning that image.
 */
public class MarkerScanner {

    private final ImageProcessor ip;    // the input image
    private final int markerSize;       // the number of marker data fields in each direction
    private final int targetSize;       // the size of the canonical image (5x5 pixels for each marker field)
    private final int fieldWidth;
    private final int sampleOffset;
    private final Pnt2d[] targetPts;

    // ColorProcessor colorIp = null;

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
     * @param outline the corners of the marker quad (image coordinates)
     * @param threshold the threshold to decide 0/1 field contents
     * @return a {@link BitVector} holding the extracted bit pattern
     */
    public BitVector getMarkerData(List<Pnt2d> outline, int threshold) {
        Pnt2d[] sourcePts = outline.toArray(new Pnt2d[0]);

        // calculate homography mapping (from target to source):
        ProjectiveMapping2D hom = ProjectiveMapping2D.fromPoints(targetPts, sourcePts);

        ByteProcessor canonicalIm = new ByteProcessor(targetSize, targetSize);
        new ImageMapper(hom).map(ip, canonicalIm);

        // this.colorIp = canonicalIm.convertToColorProcessor();
        // new ZoomableImagePlus("canonical", canonicalIm).show(20);

        BitVector result = parseImage(canonicalIm, threshold);
        // new ZoomableImagePlus("canonical", colorIp).show(20);
        return result;
    }


    // /**
    //  * @param unitMapping homography from the unit square to marker quad
    //  * @return
    //  */
    // private ByteProcessor getCanonicalImage(ProjectiveMapping2D unitMapping) {
    //     //int targetSize = 5 * (markerSize + 2);
    //     // ProjectiveMapping2D unitMapping = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping
    //     // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
    //     // 1: shift 1/2 pixel (in target space)
    //     // 2. scale from MARKER_SIZE to 1
    //     // 3. map to the source image
    //     LinearMapping2D mapping =  // shift+scale target frame to unit square, then concat with uniMapping
    //             new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / targetSize)).concat(unitMapping);
    //     ByteProcessor targetIp = new ByteProcessor(targetSize, targetSize);
    //     new ImageMapper(mapping).map(ip, targetIp);
    //
    //     targetIp.flipVertical();    // TODO: still unclear why we have to flip here (check CCW/CW fitting)
    //
    //     // listCornerMappingsUnit(new ProjectiveMapping2D(A).getInverse());
    //     // listCornerMappingsSized(mapping, targetSize);
    //     return targetIp;
    // }

    // private static void listCornerMappingsUnit(LinearMapping2D map) {
    //     System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
    //     System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(1,0)));
    //     System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(1,1)));
    //     System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,1)));
    // }
    //
    // private static void listCornerMappingsSized(LinearMapping2D map, int n) {
    //     System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
    //     System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(n,0)));
    //     System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(n,n)));
    //     System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,n)));
    // }

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
                // colorIp.set(u, v, Color.green.getRGB());
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
