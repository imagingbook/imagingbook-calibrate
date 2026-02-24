package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagingbook.jaruco.dict.ArucoDictionary;
import imagingbook.jaruco.marker.ArucoMarker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;

import static imagingbook.jaruco.dict.ArucoDictionaryPredefined.DICT_5X5_100;


/**
 * Represents a "Charuco"-type marker board, which consists of ArUco alternating markers and
 * solid black squares as board elements.
 */
public class CharucoBoard extends AbstractBoard {

    public enum Predefined {
        DICT_5x5_CharucoBoard_12x8_A4L {
            @Override
            CharucoBoard makeInstance() {
                return new CharucoBoard(12, 8, 22.0, 16.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
            }
        };

        // TODO: hide makeInstance()
        abstract CharucoBoard makeInstance();

        public CharucoBoard getInstance() {
            CharucoBoard gb = makeInstance();
            gb.setName(this.name());
            return gb;
        }
    }

    // ----------------------------------------------------------------------------------------

    private final BoardElement[][] boardElements;        // hold all MxN board elements
    private final int[][] markerMap;                     // marker column/row grid coordinates

    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. ArucoMarker ids are
     * assigned sequentially starting from zero in row-major order, e.g.,
     * <pre>
     *     0  1  2  3  4  5
     *     6  7  8  ...
     * </pre>
     * on a 6 x N board. All markers are arranged in canonical orientation (rotation 0). The board has
     * no surrounding border, i.e., the first marker is placed at the coordinate origin. All board
     * coordinates are in mm. Markers are spaced at {@code squareWidth} steps in x/y and their size is
     * {@code markerWidth}. Thus the spacing between adjacent markers is
     * {@code markerSeparation = squareWidth - markerWidth}. TODO: check again!
     *
     * @param nCols       number of markers in x direction
     * @param nRows       number of markers in y direction
     * @param squareWidth size of the marker grid, marker position step width (in mm)
     * @param markerWidth marker side length in real board space (in mm)
     * @param dictionary  the dictionary of markers
     * @param borderBits  the number of border bits around the inner of each marker
     * @param pdfPageSize the recommended PDF document size (may be null)
     */
    public CharucoBoard(int nCols, int nRows, double squareWidth, double markerWidth,
                        ArucoDictionary dictionary, int borderBits, PageFmt pdfPageSize) {
        super(nCols, nRows, squareWidth, markerWidth, dictionary, borderBits, pdfPageSize);

        boardElements = new BoardElement[nCols][nRows];

        // fill in squares and markers
        int idCnt = 0;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                if (row % 2 == col % 2) {           // black checkerboard square, no marker
                    boardElements[col][row] = new CheckerBoardSquare(this, col, row);
                }
                else {                              // here comes a Aruco marker
                    ArucoMarker marker = new ArucoMarker(idCnt, 0, dictionary, borderBits);
                    boardElements[col][row] = new BoardMarker(this, marker, col, row);
                    idCnt++;
                }
            }
        }

        // collect marker ids and grid positions:
        markerMap = new int[idCnt][2];
        for (int v = 0; v < nRows; v++) {
            for (int u = 0; u < nCols; u++) {
                if (boardElements[u][v] instanceof BoardMarker marker) {
                    markerMap[marker.getId()][0] = marker.getColIndex();    // = u
                    markerMap[marker.getId()][1] = marker.getRowIndex();
                }
            }
        }
        checkMarkers();
    }

    void checkMarkers() {
        System.out.println("checking markers: " + markerMap.length);
        for (int i = 0; i < markerMap.length; i++) {
            BoardMarker marker = getMarker(i);
            if (marker.getId() != i) {
                throw new IllegalStateException(("wrong marker id at " + i));
            }
        }
    }

    @Override
    boolean checkDictionarySize() {
        int idCnt = 0;
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (row % 2 == col % 2) {
                    idCnt++;
                }
            }
        }
        if (idCnt > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + idCnt);
        }
        return true;
    }

    // ----------------------------------------------------------------------------------

    @Override
    public BoardMarker getMarker(int id) {
        int u = markerMap[id][0];
        int v = markerMap[id][1];
        return (BoardMarker) boardElements[u][v];
    }

    @Override
    public int getMarkerCount() {
        return markerMap.length;
    }

    @Override
    void drawBoardContent(Graphics2D g2, double scale, double xOffset, double yOffset) {
        for (int v = 0; v < gridRows; v++) {
            for (int u = 0; u < gridCols; u++) {
                BoardElement elem = boardElements[u][v];
                if (elem instanceof BoardMarker marker) {
                    double x0 = marker.getCorner(0).getX() * scale + xOffset;   // TODO: make BoardMarker self-draw
                    double y0 = marker.getCorner(0).getY() * scale + yOffset;
                    double mw = markerWidth * scale;
                    marker.drawTo(g2, x0, y0, mw);
                }
                else if (elem instanceof CheckerBoardSquare sqr) {
                    g2.setColor(Color.black);
                    double x0 = sqr.getCorner(0).getX() * scale + xOffset;   // TODO: make BlackSquare self-draw
                    double y0 = sqr.getCorner(0).getY() * scale + yOffset;
                    double sw = squareWidth * scale;
                    g2.fill(new Rectangle2D.Double(x0, y0, sw, sw));
                }
            }
        }
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        // GridBoard board = GridBoard.Predefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard board = GridBoard.Predefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard board = GridBoard.Predefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        // CharucoBoard board = new CharucoBoard(12, 8, 22.0, 16.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
        CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", board.getBoardWidth(), board.getBoardHeight());
        System.out.println(board.toString());
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

        System.out.println("pdf path = " + board.saveAsPdf(Path.of("tmp/chboard.pdf")));
    }

}
