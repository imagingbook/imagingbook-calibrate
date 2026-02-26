package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.gui.Overlay;
import imagingbook.common.color.iterate.CssColorSequencer;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PolyLine2d;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.marker.ArucoMarkerDetector;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectedMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects sets of (presumably) collinear image points from marker detections by evaluating the
 * associated marker board geometry.
 * The points in a collinear point set should not be assumed to be ordered (though they are).
 */
public class CollinearPointsExtractor {

    private final AbstractMarkerBoard board;

    /**
     * Constructor. Creates a new instance for a specific type of marker board.
     * @param board
     */
    public CollinearPointsExtractor(AbstractMarkerBoard board) {
        this.board = board;
    }

    /**
     * Collects all detected markers.
     * @param detections the list of marker detections
     */
    private ArucoMarkerDetector.DetectedMarker[][] fillDetectionArray(List<ArucoMarkerDetector.DetectedMarker> detections) {
        int cols = board.getGridCols();
        int rows = board.getGridRows();
        DetectedMarker[][] detectionArray = new ArucoMarkerDetector.DetectedMarker[cols][rows];
        for (ArucoMarkerDetector.DetectedMarker det : detections) {
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
        return detectionArray;
    }

    /**
     * Scan {@code detectionArray} and collect corner coordinates into associated point sets.
     */
    private List<List<Pnt2d>> makeCollinearPointSets(ArucoMarkerDetector.DetectedMarker[][] detectionArray) {
        int rows = board.getGridRows();
        int cols = board.getGridCols();
        List<List<Pnt2d>> allPointSets = new ArrayList<>();

        // lambda expression for adding point sets:
        Consumer<List<Pnt2d>> pointSetAdd = pointSet -> {
            if (pointSet.size() >= 3) {  // need at least 3 collinear points
                allPointSets.add(pointSet);
            }
        };

        // make horizontal point sets (two for each marker row)
        for (int v = 0; v < rows; v++) {
            List<Pnt2d> topSet = new ArrayList<>();
            List<Pnt2d> botSet = new ArrayList<>();
            for (int u = 0; u < cols; u++) {
                ArucoMarkerDetector.DetectedMarker marker = detectionArray[u][v];
                if (marker == null) {  // no marker detected for field (u,v)
                    continue;
                }
                Pnt2d[] corners = marker.getCorners();

                // add to line passing through corners on top of marker
                topSet.add(corners[0]);
                topSet.add(corners[1]);

                // add to line passing through corners on bottom of marker
                botSet.add(corners[3]);
                botSet.add(corners[2]);
            }
            pointSetAdd.accept(topSet);
            pointSetAdd.accept(botSet);
        }

        // make vertical point sets (two for each marker column)
        for (int u = 0; u < cols; u++) {
            List<Pnt2d> leftSet = new ArrayList<>();
            List<Pnt2d> rightSet = new ArrayList<>();
            for (int v = 0; v < rows; v++) {
                ArucoMarkerDetector.DetectedMarker marker = detectionArray[u][v];
                if (marker == null) {  // no marker detected for field (u,v)
                    continue;
                }
                Pnt2d[] corners = marker.getCorners();

                // add to line passing through corners on top of marker
                leftSet.add(corners[0]);
                leftSet.add(corners[3]);

                // add to line passing through corners on bottom of marker
                rightSet.add(corners[1]);
                rightSet.add(corners[2]);
            }
            pointSetAdd.accept(leftSet);
            pointSetAdd.accept(rightSet);
        }

        return allPointSets;
    }

    public List<List<Pnt2d>> getCollinearPointSets(List<ArucoMarkerDetector.DetectedMarker> detections) {
        ArucoMarkerDetector.DetectedMarker[][] detectionArray = fillDetectionArray(detections);
        return makeCollinearPointSets(detectionArray);
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Creates and returns an ImageJ {@link Overlay} to be attached to and displayed on top of a
     * {@link ImagePlus} instance.
     * @param pointSets a list of collinear sets of 2D points
     * @return
     */
    public static Overlay getOverlay(List<List<Pnt2d>> pointSets) {
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
