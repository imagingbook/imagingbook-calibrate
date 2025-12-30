package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.ArucoDictionary;
import imagingbook.jaruco.ArucoMarker;

import java.awt.Graphics2D;
import java.nio.file.Path;

/**
 * Represents a marke board with all markers in the same plane and in a regular M x N grid layout.
 * The board contains only markers from the specified dictionary, without any additional
 * geometric shapes.
 */
public class GridBoard extends AbstractBoard {

    private final BoardElement[][] boardElements;             // holds all MxN board elements
    private final int[][] markerRegistry;                     // marker column/row grid coordinates

    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. ArucoMarker ids are
     * assigned sequentially starting from zero in row-major order, e.g.,
     * <pre>
     *     0  1  2  3  4  5
     *     6  7  8  ...
     * </pre>
     * on a 6 x N board. All markers are arranged in canonical orientation (rotation 0). The board
     * has no surrounding border, i.e., the first marker is placed at the coordinate origin. All
     * board coordinates are in mm.
     * Markers are spaced at {@code squareWidth} steps in x/y and their size is
     * {@code markerWidth}. Thus, the spacing between adjacent markers is
     * {@code markerSeparation = squareWidth - markerWidth}.
     *
     * @param gridCols number of markers in x direction
     * @param gridRows number of markers in y direction
     * @param markerWidth marker side length in real board space (in mm)
     * @param squareWidth size of the marker grid, marker position step width (in mm)
     * @param dictionary the dictionary of markers
     * @param borderBits the number of border bits around the inner of each marker
     * @param pdfPageSize the recommended PDF document size (may be null)
     */
    public GridBoard(int gridCols, int gridRows, double markerWidth, double squareWidth,
                     ArucoDictionary dictionary, int borderBits, PageFmt pdfPageSize) {
        super(gridCols, gridRows, squareWidth, markerWidth, dictionary, borderBits, pdfPageSize);

        boardElements = new BoardElement[gridCols][gridRows];

        // insert a unique marker at each grid position:
        int idCnt = 0;
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                ArucoMarker marker = new ArucoMarker(idCnt, 0, dictionary, borderBits);
                boardElements[col][row] = new BoardMarker(this, marker, col, row);
                idCnt++;
            }
        }

        // collect marker ids and grid positions:
        markerRegistry = new int[idCnt][2];
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (boardElements[col][row] instanceof BoardMarker marker) {
                    markerRegistry[marker.getId()][0] = marker.getColIndex();    // = column
                    markerRegistry[marker.getId()][1] = marker.getRowIndex();       // = row
                }
            }
        }
        checkMarkerRegistry();
    }

    private void checkMarkerRegistry() {
        // System.out.println("checking markers: " + markerRegistry.length);
        for (int i = 0; i < markerRegistry.length; i++) {
            BoardMarker marker = getMarker(i);
            if (marker.getId() != i) {
                throw new IllegalStateException(("wrong marker id at " + i));
            }
        }
    }

    @Override
    boolean checkDictionarySize() {
        int totalMarkers = gridCols * gridRows;
        if (totalMarkers > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + totalMarkers);
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public BoardMarker getMarker(int id) {
        int u = markerRegistry[id][0];
        int v = markerRegistry[id][1];
        return (BoardMarker) boardElements[u][v];
    }

    @Override
    public Polygon2d getMarkerCorners(int id) {
        return getMarker(id).getCorners();
    }

    @Override
    public int getMarkerCount() {
        return markerRegistry.length;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    void drawBoardContent(Graphics2D g2, double scale, double xOffset, double yOffset) {
        // draw each marker
        for (int idx = 0; idx < getMarkerCount(); idx++) {
            Polygon2d corners = getMarkerCorners(idx);
            double x0 = corners.getPnt(0).getX() * scale + xOffset;
            double y0 = corners.getPnt(0).getY() * scale + yOffset;
            double mw = markerWidth * scale;
            dictionary.getMarker(idx, 0, borderBits).drawTo(g2, x0, y0, mw);
        }
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        GridBoard board = GridBoardPredefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", board.getBoardWidth(), board.getBoardHeight());
        System.out.println(board.toString());
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

        System.out.println("pdf path = " + board.saveAsPdf(Path.of("tmp/board.pdf")));
    }
}
