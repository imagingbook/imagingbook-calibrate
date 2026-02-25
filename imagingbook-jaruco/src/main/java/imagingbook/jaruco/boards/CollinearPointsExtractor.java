package imagingbook.jaruco.boards;

import ij.gui.Overlay;
import imagingbook.common.color.iterate.CssColorSequencer;
import imagingbook.common.color.iterate.FiniteLinearColorSequencer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PolyLine2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectionResult;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CollinearPointsExtractor {

    private final CharucoBoard board;
    private final DetectionResult[][] detectionArray;
    private final List<List<Pnt2d>> pointSets;


    public CollinearPointsExtractor(CharucoBoard board, List<DetectionResult> detections) {
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
    private void fillDetectionArray(List<DetectionResult> detections) {
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
    private void makeCollinearPointSets() {
        int rows = board.getGridRows();
        int cols = board.getGridCols();
        int mc = board.getMarkerCount();

        // make horizontal point sets (two for each marker row)
        for (int v = 0; v < rows; v++) {
            List<Pnt2d> topSet = new ArrayList<>();
            List<Pnt2d> botSet = new ArrayList<>();
            for (int u = 0; u < cols; u++) {
                DetectionResult det = detectionArray[u][v];
                if (det == null) {  // no marker detected for field (u,v)
                    continue;
                }
                int markerId = det.getLookup().markerId();
                Pnt2d[] corners = det.getCorners();

                // add to line passing through corners on top of marker
                topSet.add(corners[0]);
                topSet.add(corners[1]);

                // add to line passing through corners on bottom of marker
                botSet.add(corners[3]);
                botSet.add(corners[2]);
            }
            addPointSet(topSet);
            addPointSet(botSet);
        }

        // make vertical point sets (two for each marker column)
        for (int u = 0; u < cols; u++) {
            List<Pnt2d> leftSet = new ArrayList<>();
            List<Pnt2d> rightSet = new ArrayList<>();
            for (int v = 0; v < rows; v++) {
                DetectionResult det = detectionArray[u][v];
                if (det == null) {  // no marker detected for field (u,v)
                    continue;
                }
                int markerId = det.getLookup().markerId();
                Pnt2d[] corners = det.getCorners();

                // add to line passing through corners on top of marker
                leftSet.add(corners[0]);
                leftSet.add(corners[3]);

                // add to line passing through corners on bottom of marker
                rightSet.add(corners[1]);
                rightSet.add(corners[2]);
            }
            addPointSet(leftSet);
            addPointSet(rightSet);
        }
    }

    private void addPointSet(List<Pnt2d> pointSet) {
        if (pointSet.size() >= 3) {  // need at least 3 collinear points
            pointSets.add(pointSet);
        }
    }

    public List<List<Pnt2d>> getCollinearPointSets() {
        return pointSets;
    }

    // -------------------------------------------------------------------------------------------

    public Overlay getOverlay() {
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        CssColorSequencer colSeq = new CssColorSequencer();

        for (List<Pnt2d> pointSet : pointSets) {
            PolyLine2d poly = new PolyLine2d(pointSet);
            ColoredStroke lineStroke = new ColoredStroke(1.0, colSeq.next());
            ola.addShape(poly.getShape(), lineStroke);
        }

        return ola.getOverlay();
    }

}
