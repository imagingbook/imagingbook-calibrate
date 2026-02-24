package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectionResult;

import java.util.ArrayList;
import java.util.List;

public class CollinearPointsExtractorr {

    private final CharucoBoard board;
    private final DetectionResult[][] detectionArray;
    private final List<List<Pnt2d>> pointSets;


    public CollinearPointsExtractorr(CharucoBoard board, List<DetectionResult> detections) {
        this.board = board;
        int cols = board.getGridCols();
        int rows = board.getGridRows();
        this.detectionArray = new DetectionResult[cols][rows];
        fillDetectionArray(detections);

        this.pointSets = new ArrayList<>();
        makeCollinearPointSets();
    }

    /**
     * Collects all detected markers in the 2D {@link #detectionArray}.
     * @param detections the list of marker detections
     */
    void fillDetectionArray(List<DetectionResult> detections) {
        for (DetectionResult det : detections) {
            int markerId = det.getLookup().markerId();
            BoardMarker marker = board.getMarker(markerId);
            if (marker != null) {
                int row = board.getMarker(markerId).getRowIndex();  // u grid coordinate
                int col = board.getMarker(markerId).getColIndex();  // v grid coordinate
                if (detectionArray[col][row] != null) {
                    throw new RuntimeException("duplicate marker detected: " + markerId);
                }
                detectionArray[col][row] = det;
            }
        }
    }

    /**
     * Scan {@link #detectionArray} and collect corner coordinates into associated point sets.
     */
    void makeCollinearPointSets() {
        int rows = board.getGridRows();
        int cols = board.getGridCols();
        int mc = board.getMarkerCount();

        // make horizontal point sets (two for each marker row)
        for (int row = 0; row < rows; row++) {
            List<Pnt2d> topSet = new ArrayList<>();
            List<Pnt2d> botSet = new ArrayList<>();
            for (int col = 0; col < cols; col++) {
                DetectionResult det = detectionArray[col][row];
                int markerId = det.getLookup().markerId();
                Pnt2d[] corners = det.getCorners();

                // add to line passing through corners on top of marker
                topSet.add(corners[0]);
                topSet.add(corners[1]);

                // add to line passing through corners on bottom of marker
                botSet.add(corners[3]);
                botSet.add(corners[2]);
            }
            pointSets.add(topSet);
            pointSets.add(botSet);
        }

        // make vertical point sets (two for each marker column)
        for (int col = 0; col < cols; col++) {
            List<Pnt2d> leftSet = new ArrayList<>();
            List<Pnt2d> rightSet = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                DetectionResult det = detectionArray[col][row];
                int markerId = det.getLookup().markerId();
                Pnt2d[] corners = det.getCorners();

                // add to line passing through corners on top of marker
                leftSet.add(corners[0]);
                leftSet.add(corners[3]);

                // add to line passing through corners on bottom of marker
                rightSet.add(corners[2]);
                rightSet.add(corners[3]);
            }
            pointSets.add(leftSet);
            pointSets.add(rightSet);
        }
    }

    public List<List<Pnt2d>> getCollinearPointSets() {
        return pointSets;
    }

}
