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
     * @param sourceIp the input image
     * @param A homography from the image quad to the unit square
     * @return
     */
    public ByteProcessor getCanonicalImage(ImageProcessor sourceIp, double[][] A) {
        int targetSize = 5 * (markerSize + 2);
        ProjectiveMapping2D unitMapping = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping
        // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
        // 1: shift 1/2 pixel (in target space)
        // 2. scale from MARKER_SIZE to 1
        // 3. map to the source image
        LinearMapping2D mapping =  // shift+scale target frame to unit square, then concat with uniMapping
                new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / targetSize)).concat(unitMapping);
        // LinearMapping2D mapping =
        //         new Scaling2D(1.0 / targetSize).concat(unitMapping);

        ByteProcessor targetIp = new ByteProcessor(targetSize, targetSize);
        new ImageMapper(mapping).map(sourceIp, targetIp);

        targetIp.flipVertical();    // TODO: still unclear why we have to flip here (check CCW/CW fitting)

        // listCornerMappingsUnit(new ProjectiveMapping2D(A).getInverse());
        // listCornerMappingsSized(mapping, targetSize);

        return targetIp;
    }

    static void listCornerMappingsUnit(LinearMapping2D map) {
        System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
        System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(1,0)));
        System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(1,1)));
        System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,1)));
    }

    static void listCornerMappingsSized(LinearMapping2D map, int n) {
        System.out.println("corner 0,0 -> " + map.applyTo(Pnt2d.from(0,0)));
        System.out.println("corner 1,0 -> " + map.applyTo(Pnt2d.from(n,0)));
        System.out.println("corner 1,1 -> " + map.applyTo(Pnt2d.from(n,n)));
        System.out.println("corner 0,1 -> " + map.applyTo(Pnt2d.from(0,n)));
    }

}
