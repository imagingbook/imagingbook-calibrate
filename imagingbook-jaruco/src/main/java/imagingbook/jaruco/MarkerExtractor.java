package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.geometry.mappings.linear.Scaling2D;
import imagingbook.common.geometry.mappings.linear.Translation2D;
import imagingbook.common.image.ImageMapper;

/**
 * Takes care of extracting the canonical marker image from the imput image.
 */
public class MarkerExtractor {

    private final ImageProcessor ip;
    private final int markerImageSize;

    public MarkerExtractor(ImageProcessor ip, int markerImageSize) {
        this.ip = ip;
        this.markerImageSize = markerImageSize;
    }

    /**
     * @param sourceIp the input image
     * @param A homography from the image quad to the unit square
     * @return
     */
    public ByteProcessor getCanonicalImage(ImageProcessor sourceIp, double[][] A) {
        ProjectiveMapping2D unitMapping = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping
        // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
        // 1: shift 1/2 pixel (in target space)
        // 2. scale from MARKER_SIZE to 1
        // 3. map to the source image
        LinearMapping2D map =
                new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / markerImageSize)).concat(unitMapping);
        ByteProcessor targetIp = new ByteProcessor(markerImageSize, markerImageSize);
        new ImageMapper(map).map(sourceIp, targetIp);
        return targetIp;
    }

}
