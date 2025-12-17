package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.mappings.linear.LinearMapping2D;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.geometry.mappings.linear.Scaling2D;
import imagingbook.common.geometry.mappings.linear.Translation2D;
import imagingbook.common.image.ImageMapper;

public class MarkerExtractor {

    private final ImageProcessor ip;
    private final int markerSize;

    public MarkerExtractor(ImageProcessor ip, int markerSize) {
        this.ip = ip;
        this.markerSize = markerSize;
    }

    public ByteProcessor extractMarker(SegmentedContour poly) {
        QuadHomographyFit fit = new QuadHomographyFit(poly);
        double[][] A = fit.getTransformationMatrix();
        ProjectiveMapping2D map = new ProjectiveMapping2D(A).getInverse();  // target-to-source mapping
        return extractMarkerImage(ip, map);
    }

    /**
     *
     * @param sourceIp
     * @param unitMapping target-to-source-mapping from the unit square to the marker's quad
     * @return
     */
    ByteProcessor extractMarkerImage(ImageProcessor sourceIp, LinearMapping2D unitMapping) {
        ByteProcessor targetIp = new ByteProcessor(markerSize, markerSize);
        // adjust the unit-square mapping to the MARKER_SIZE x MARKER_SIZE target:
        // 1: shift 1/2 pixel (in target space)
        // 2. scale from MARKER_SIZE to 1
        // 3. map to the source image
        LinearMapping2D map = new Translation2D(0.5, 0.5).concat(new Scaling2D(1.0 / markerSize)).concat(unitMapping);
        new ImageMapper(map).map(sourceIp, targetIp);
        return targetIp;
    }

}
