package imagingbook.jaruco.boards;

import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.ArucoDetector;
import imagingbook.jaruco.ArucoDictionary;

import java.util.List;

public class GridBoardDetector {

    private final GridBoard board;

    public GridBoardDetector(GridBoard board) {
        this.board = board;
    }

    // ----------------------------------------------------------------------------------

    /**
     * Search the supplied image for Aruco markers and return a list of associated
     * image and board positions.
     * @param ip
     */
    public void detectBoard(ByteProcessor ip) {
        ArucoDictionary dict = board.getDictionary();
        ArucoDetector detector = new ArucoDetector(dict);
        List<ArucoDetector.DetectionResult> detResults = detector.detectMarkers(ip);

        for (ArucoDetector.DetectionResult detResult : detResults) {
            int markerId = detResult.lookup().markerId();
            Polygon2d imCorners = detResult.corners();
            Polygon2d boardCorners = board.getMarkerCorners(markerId);

        }
    }
}
