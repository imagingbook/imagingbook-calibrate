package imagingbook.jaruco.boards;

import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.marker.ArucoMarkerDetector;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract super class for Aruco/Charuco board detectors.
 */
public abstract class AbstractBoardDetector {

    /**
     * A pair of corresponding points.
     * @param imagePnt point located in the image
     * @param modelPnt corresponding point on the board
     */
    public record PntPair(Pnt2d imagePnt, Pnt2d modelPnt) { }

    final AbstractBoard board;
    final ByteProcessor ip;
    final List<ArucoMarkerDetector.DetectionResult> detResults;


    public AbstractBoardDetector(AbstractBoard board, ByteProcessor ip) {
        this.ip = ip;
        this.board = board;
        this.detResults = detectMarkers(board, ip);
        checkBoard();
    }

    // ----------------------------------------------------------------------------------

    // may be overridden by subclasses
    List<DetectionResult> detectMarkers(AbstractBoard board, ByteProcessor ip) {
        ArucoMarkerDetector markerDetector = new ArucoMarkerDetector(board.getDictionary());
        List<DetectionResult> detections = markerDetector.detectMarkers(ip);
        Collections.sort(detections);  // sort by marker id
        return detections;
    }

    public boolean allBoardMarkersFound() {
        return detResults.size() == board.getMarkerCount();
    }

    // ----------------------------------------------------------------------------------

    // inheriting classes may/should override:
    public void checkBoard() {
        // check if all marker corners are in CCW order:
        for (DetectionResult detResult : detResults) {
            Pnt2d[] imageCorners = detResult.getCorners();
            Polygon2d poly = new Polygon2d(imageCorners);
            if (!poly.isClockwiseOnScreen()) {
                throw new RuntimeException("corners not screen-clockwise for id = "
                        + detResult.getLookup().markerId());
            }
        }

    }

    // ----------------------------------------------------------------------------------

    public int getDetectedMarkerCount() {
        return detResults.size();
    }

    public List<DetectionResult> getDetectedMarkers() {
        return detResults;
    }

    public List<Integer> getDetectedMarkerIds() {
        List<Integer> ids = new ArrayList<>();
        for (DetectionResult detectionResult : detResults) {
            ids.add(detectionResult.getLookup().markerId());
        }
        return ids;
    }

    public List<PntPair> getAllMarkerPointMatches() {
        List<PntPair> matches = new ArrayList<>();
        for (DetectionResult detResult : detResults) {
            int markerId = detResult.getLookup().markerId();
            Pnt2d[] imageCorners = detResult.getCorners();
            Pnt2d[] boardCorners = board.getMarkerCorners(markerId);
            for (int i = 0; i < 4; i++) {
                matches.add(new PntPair(imageCorners[i], boardCorners[i]));
            }
        }
        return matches;
    }
}
