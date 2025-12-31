package imagingbook.jaruco.boards;

import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.marker.ArucoMarkerDetector;

import java.util.ArrayList;
import java.util.List;

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

    // may be overridden by subclasses
    List<ArucoMarkerDetector.DetectionResult> detectMarkers(AbstractBoard board, ByteProcessor ip) {
        ArucoMarkerDetector markerDetector = new ArucoMarkerDetector(board.getDictionary());
        return markerDetector.detectMarkers(ip);
    }

    // ----------------------------------------------------------------------------------

    // inheriting classes may/should override:
    public void checkBoard() {
        // check if all marker corners are in CCW order:
        for (ArucoMarkerDetector.DetectionResult detResult : detResults) {
            Polygon2d imageCorners = detResult.corners();
            if (!imageCorners.isClockwiseOnScreen()) {
                throw new RuntimeException("corners not screen-clockwise for id = " + detResult.lookup().markerId());
            }
        }

    }

    // ----------------------------------------------------------------------------------

    public int getDetectedMarkerCount() {
        return detResults.size();
    }

    public List<Integer> getDetectedMarkerIds() {
        List<Integer> ids = new ArrayList<>();
        for (ArucoMarkerDetector.DetectionResult detectionResult : detResults) {
            ids.add(detectionResult.lookup().markerId());
        }
        return ids;
    }

    public List<PntPair> getAllMarkerPointMatches() {
        List<PntPair> matches = new ArrayList<>();
        for (ArucoMarkerDetector.DetectionResult detResult : detResults) {
            int markerId = detResult.lookup().markerId();
            Polygon2d imageCorners = detResult.corners();
            Polygon2d boardCorners = board.getMarkerCorners(markerId);
            for (int i = 0; i < 4; i++) {
                matches.add(new PntPair(imageCorners.getPnt(i), boardCorners.getPnt(i)));
            }
        }
        return matches;
    }
}
