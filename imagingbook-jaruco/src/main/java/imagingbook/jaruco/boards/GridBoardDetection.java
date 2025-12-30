package imagingbook.jaruco.boards;

import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.ArucoMarkerDetector;
import imagingbook.jaruco.ArucoDictionary;

import java.util.List;

/**
 * Performs detection of the specified {@link GridBoard} in an image
 * and holds the result for evaluation.
 * TODO: hold the result in another object?
 * Typical usage:
 * <pre>
 *     GridBoard board = new GridBoard();
 *     GridBoardDetector detector = new GridBoardDetector(board);
 *     ByteProcessor ip = ... // input image
 *     Result result = detector.detectBoard(ip);
 *
 *     List&lt;PntMatch&gt; allMatches = result.getAllPointMatches();
 *     List&lt;LineMatch&gt; lineMatches = result.getAllLineMatches();
 *     List&lt;List&lt;PntMatch&gt;&gt; markerMatches = .....
 *     List&lt;List&lt;PntMatch&gt;&gt; checkerMatches = .....
 *     List&lt;PntMatchGroup&gt; anyGroup = ....
 * </pre>
 * {@code PntMatch} is a pair of {@code (imagePnt, modelPnt)},
 * a {@code PntMatchGroup} is simply a set of {@code PntMatch} instances.
 * Is it important to group point matches? Certainly for straight lines.
 * how about for marker boxes?
 * All this should be as generic as possible.
 */
public class GridBoardDetection {

    private final GridBoard board;

    public GridBoardDetection(GridBoard board) {
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
        // 1. Detect Aruco markers
        ArucoMarkerDetector detector = new ArucoMarkerDetector(dict);
        List<ArucoMarkerDetector.DetectionResult> detResults = detector.detectMarkers(ip);

        for (ArucoMarkerDetector.DetectionResult detResult : detResults) {
            int markerId = detResult.lookup().markerId();
            Polygon2d imCorners = detResult.corners();
            Polygon2d boardCorners = board.getMarkerCorners(markerId);
        }

    }
}
