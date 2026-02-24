package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.dict.ArucoDictionary;
import imagingbook.jaruco.marker.ArucoMarker;

import java.awt.Graphics2D;
import java.nio.file.Path;

import static imagingbook.jaruco.dict.ArucoDictionaryPredefined.DICT_5X5_100;
import static imagingbook.jaruco.dict.ArucoDictionaryPredefined.DICT_5X5_1000;

/**
 * Represents a marker board with all markers in the same plane and in a regular M x N grid layout.
 * The board contains only markers from the specified dictionary, without any additional
 * geometric shapes.
 */
public class GridBoard extends AbstractBoard {

    public enum Predefined {
        DICT_5X5_GridBoard_8x5_A4L {
            @Override
            GridBoard makeInstance() {
                return new GridBoard(8, 5, 35.0, 25.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
            }
        },
        DICT_5X5_GridBoard_12x8_A4L {
            @Override
            GridBoard makeInstance() {
                return new GridBoard(12, 8, 21.0, 15.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
            }
        },
        DICT_5X5_GridBoard_18x12_A3L {
            @Override
            GridBoard makeInstance() {
                return new GridBoard(18, 12, 21.0, 15.0, DICT_5X5_1000.getInstance(), 1, PageFmt.A3_Landscape);
            }
        };

        // TODO: hide makeInstance()
        abstract GridBoard makeInstance();

        public GridBoard getInstance() {
            GridBoard gb = makeInstance();
            gb.setName(this.name());
            return gb;
        }
    }

    // ---------------------------------------------------------------------------------------------

    private final BoardElement[][] boardElements;             // holds all MxN board elements
    private final int[][] markerMap;                     // marker column/row grid coordinates

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
     * @param nCols       number of markers in x direction
     * @param nRows       number of markers in y direction
     * @param squareWidth size of the marker grid, marker position step width (in mm)
     * @param markerWidth marker side length in real board space (in mm)
     * @param dictionary  the dictionary of markers
     * @param borderBits  the number of border bits around the inner of each marker
     * @param pdfPageSize the recommended PDF document size (may be null)
     */
    public GridBoard(int nCols, int nRows, double squareWidth, double markerWidth,
                     ArucoDictionary dictionary, int borderBits, PageFmt pdfPageSize) {
        super(nCols, nRows, squareWidth, markerWidth, dictionary, borderBits, pdfPageSize);

        boardElements = new BoardElement[nCols][nRows];

        // insert a unique marker at each grid position:
        int idCnt = 0;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                ArucoMarker marker = new ArucoMarker(idCnt, 0, dictionary, borderBits);
                boardElements[col][row] = new BoardMarker(this, marker, col, row);
                idCnt++;
            }
        }

        // collect marker ids and grid positions:
        markerMap = new int[idCnt][2];
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                if (boardElements[col][row] instanceof BoardMarker marker) {
                    markerMap[marker.getId()][0] = marker.getColIndex();    // = col
                    markerMap[marker.getId()][1] = marker.getRowIndex();    // = row
                }
            }
        }
        checkMarkerRegistry();
    }

    private void checkMarkerRegistry() {
        // System.out.println("checking markers: " + markerRegistry.length);
        for (int i = 0; i < markerMap.length; i++) {
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
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: "
                    + totalMarkers);
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public BoardMarker getMarker(int id) {
        int col = markerMap[id][0];
        int row = markerMap[id][1];
        return (BoardMarker) boardElements[col][row];
    }

    @Override
    public Pnt2d[] getMarkerCorners(int id) {
        return getMarker(id).getCorners();
    }

    @Override
    public int getMarkerCount() {
        return markerMap.length;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    void drawBoardContent(Graphics2D g2, double scale, double xOffset, double yOffset) {
        // draw each marker
        for (int idx = 0; idx < getMarkerCount(); idx++) {
            Pnt2d[] corners = getMarkerCorners(idx);
            double x0 = corners[0].getX() * scale + xOffset;
            double y0 = corners[0].getY() * scale + yOffset;
            double mw = markerWidth * scale;
            dictionary.getMarker(idx, 0, borderBits).drawTo(g2, x0, y0, mw);
        }
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        GridBoard board = GridBoard.Predefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard gb = GridBoard.Predefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoard.Predefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", board.getBoardWidth(), board.getBoardHeight());
        System.out.println(board.toString());
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

        System.out.println("pdf path = " + board.saveAsPdf(Path.of("tmp/board.pdf")));
    }


}
