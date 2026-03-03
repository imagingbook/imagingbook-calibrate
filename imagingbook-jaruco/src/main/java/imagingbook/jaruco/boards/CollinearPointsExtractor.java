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
    private final int rows;
    private final int cols;

    /**
     * Constructor. Creates a new instance for a specific type of marker board.
     * @param board
     */
    public CollinearPointsExtractor(AbstractMarkerBoard board) {
        this.board = board;
        this.rows = board.getGridRows();
        this.cols = board.getGridCols();
    }

    /**
     * Collects all detected markers.
     * @param detections the list of marker detections
     */
    private DetectedMarker[][] fillDetectionArray(List<DetectedMarker> detections) {
        int cols = board.getGridCols();
        int rows = board.getGridRows();
        DetectedMarker[][] detectionArray = new DetectedMarker[cols][rows];
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
     * Scan {@code detectionArray} and collect corner coordinates into collinear point sets.
     * @param detectionArray a 2D array of Arucomarker detections
     * @param pointSetCollector {@link Consumer} function object to add a collinear point set
     */
    private void collectHorizontalLines(DetectedMarker[][] detectionArray, Consumer<List<Pnt2d>> pointSetCollector) {
        // make horizontal point sets (two for each marker row)
        for (int v = 0; v < rows; v++) {
            List<Pnt2d> topSet = new ArrayList<>();
            List<Pnt2d> botSet = new ArrayList<>();
            for (int u = 0; u < cols; u++) {
               DetectedMarker marker = detectionArray[u][v];
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
            pointSetCollector.accept(topSet);
            pointSetCollector.accept(botSet);
        }
    }

    private void collectVerticalLines(DetectedMarker[][] detectionArray, Consumer<List<Pnt2d>> pointSetCollector) {
        // make vertical point sets (two for each marker column)
        for (int u = 0; u < cols; u++) {
            List<Pnt2d> lftSet = new ArrayList<>();
            List<Pnt2d> rgtSet = new ArrayList<>();
            for (int v = 0; v < rows; v++) {
                DetectedMarker marker = detectionArray[u][v];
                if (marker == null) {  // no marker detected for field (u,v)
                    continue;
                }
                Pnt2d[] corners = marker.getCorners();

                // add to line passing through corners on top of marker
                lftSet.add(corners[0]);
                lftSet.add(corners[3]);

                // add to line passing through corners on bottom of marker
                rgtSet.add(corners[1]);
                rgtSet.add(corners[2]);
            }
            pointSetCollector.accept(lftSet);
            pointSetCollector.accept(rgtSet);
        }
    }

    private void collectDiagonalsLR(DetectedMarker[][] detectionArray, Consumer<List<Pnt2d>> pointSetCollector) {
        // 1. Diagonals starting on the Top Row (Row 0, Column j)
        for (int i = 0; i < cols; i++) {
            addSingleDiagonalLR(detectionArray, i, 0, pointSetCollector);
        }

        // 2. Diagonals starting on the Left Column (Column 0, Row j)
        for (int j = 1; j < rows; j++) {
            addSingleDiagonalLR(detectionArray, 0, j, pointSetCollector);
        }
    }

    private void addSingleDiagonalLR(DetectedMarker[][] detectionArray, int startCol, int startRow, Consumer<List<Pnt2d>> pointSetCollector) {
        List<Pnt2d> lftSet = new ArrayList<>();
        List<Pnt2d> ctrSet = new ArrayList<>();
        List<Pnt2d> rgtSet = new ArrayList<>();
        for (int u = startCol, v = startRow; u < cols && v < rows; u++, v++) {
            if (detectionArray[u][v] != null) {
                Pnt2d[] corners = detectionArray[u][v].getCorners();
                lftSet.add(corners[1]);
                ctrSet.add(corners[0]);
                ctrSet.add(corners[2]);
                rgtSet.add(corners[3]);
            }
        }
        pointSetCollector.accept(lftSet);
        pointSetCollector.accept(ctrSet);
        pointSetCollector.accept(rgtSet);
    }

    private void collectDiagonalsRL(DetectedMarker[][] detectionArray, Consumer<List<Pnt2d>> pointSetCollector) {
        // 1. Diagonals starting on the Top Row (Row 0, Column j)
        for (int i = 0; i < cols; i++) {
            addSingleDiagonalRL(detectionArray, i, 0, pointSetCollector);
        }

        // 2. Diagonals starting on the Right Column (Column cols-1, Row j)
        for (int j = 1; j < rows; j++) {
            addSingleDiagonalRL(detectionArray, cols - 1, j, pointSetCollector);
        }
    }

    private void addSingleDiagonalRL(DetectedMarker[][] detectionArray, int startCol, int startRow, Consumer<List<Pnt2d>> pointSetCollector) {
        List<Pnt2d> lftSet = new ArrayList<>();
        List<Pnt2d> ctrSet = new ArrayList<>();
        List<Pnt2d> rgtSet = new ArrayList<>();
        for (int u = startCol, v = startRow; u >= 0 && v < rows; u--, v++) {
            if (detectionArray[u][v] != null) {
                Pnt2d[] corners = detectionArray[u][v].getCorners();
                lftSet.add(corners[2]);
                ctrSet.add(corners[1]);
                ctrSet.add(corners[3]);
                rgtSet.add(corners[0]);
            }
        }
        pointSetCollector.accept(lftSet);
        pointSetCollector.accept(ctrSet);
        pointSetCollector.accept(rgtSet);
    }

    // ------------------------------------------------

    public List<List<Pnt2d>> getCollinearPointSets(List<ArucoMarkerDetector.DetectedMarker> detections) {
        ArucoMarkerDetector.DetectedMarker[][] detectionArray = fillDetectionArray(detections);
        List<List<Pnt2d>> allPointSets = new ArrayList<>();

        // function for adding point sets:
        Consumer<List<Pnt2d>> pointSetCollector = pointSet -> {
            if (pointSet.size() >= 3) {  // need at least 3 collinear points
                allPointSets.add(pointSet);
            }
        };

        collectHorizontalLines(detectionArray, pointSetCollector);
        collectVerticalLines(detectionArray, pointSetCollector);
        collectDiagonalsLR(detectionArray, pointSetCollector);
        collectDiagonalsRL(detectionArray, pointSetCollector);

        return allPointSets;
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Creates and returns an ImageJ {@link Overlay} to be attached to and displayed on top of a
     * {@link ImagePlus} instance.
     * @param pointSets a list of collinear sets of 2D points
     * @return an ImageJ {@link Overlay} instance for the supplied point sets
     */
    public static Overlay getOverlay(List<List<Pnt2d>> pointSets) {
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        CssColorSequencer colSeq = new CssColorSequencer();
        for (List<Pnt2d> pointSet : pointSets) {
            ColoredStroke lineStroke = new ColoredStroke(1.0, colSeq.next());
            ola.setStroke(lineStroke);

            PolyLine2d poly = new PolyLine2d(pointSet);
            ola.addShape(poly.getShape());
            for (Pnt2d point : pointSet) {
                ola.addShape(point.getShape(15));
            }
        }

        return ola.getOverlay();
    }

}
