package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.ArucoDictionary;

import java.awt.Graphics2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a marke board with all markers in the same plane and in a regular M x N grid layout.
 * The board contains only markers from the specified dictionary, without any additional
 * geometric shapes.
 */
public class GridBoard extends AbstractBoard {

    private final int[] ids;
    final List<Pnt2d[]> cornerPoints;


    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. Marker ids are
     * assigned sequentially starting from zero in row-major order, e.g.,
     * <pre>
     *     0  1  2  3  4  5
     *     6  7  8  ...
     * </pre>
     * on a 6 x N board. All markers are arranged in canonical orientation (rotation 0). The board
     * has no surrounding border, i.e., the first marker is placed at the coordinate origin. All
     * board coordinates are in mm.
     * Markers are spaced at {@code squareWidth} steps in x/y and their size is
     * {@code markerWidth}. Thus the spacing between adjacent markers is
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
        super(gridCols, gridRows, markerWidth, squareWidth, dictionary, borderBits, pdfPageSize);
        // this.gridCols = gridCols;
        // this.gridRows = gridRows;
        // this.markerWidth = markerWidth;
        // this.squareWidth = squareWidth;
        // this.dictionary = dictionary;
        // this.borderBits = borderBits;
        // this.pdfPageSize = pdfPageSize;

        // distance between adjacent markers
        double markerSep = squareWidth - markerWidth;

        double onePin = markerWidth / (dictionary.getMarkerSize() + 2);    // size of one marker bitfield
        if (markerSep < onePin * 0.7) {
            System.out.println("Marker border " + markerSep + " is less than 70% of ArUco pin size " + onePin);
            System.out.println("Please increase markerSeparation or decrease markerLength for stable board detection");
        }
        int totalMarkers = gridCols * gridRows;
        if (totalMarkers > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + totalMarkers);
        }
        this.ids = new int[totalMarkers];
        Arrays.setAll(ids, (i) -> i); // fill ids = 0, 1, 2, ...

        // calculate markers' corner points in board coordinates
        this.cornerPoints = new ArrayList<>();
        for (int v = 0; v < gridRows; v++) {
            double y = v * squareWidth;
            for (int u = 0; u < gridCols; u++) {
                double x = u * squareWidth;
                Pnt2d[] corners = new Pnt2d[4];
                corners[0] = Pnt2d.from(
                        u * squareWidth + markerSep/2,
                        v * squareWidth + markerSep/2);
                        // u * (markerWidth + markerSep),
                        // v * (markerWidth + markerSep));
                corners[1] = corners[0].plus(markerWidth, 0);
                corners[2] = corners[0].plus(markerWidth, markerWidth);
                corners[3] = corners[0].plus(0, markerWidth);
                cornerPoints.add(corners);
            }
        }

        this.boardWidth = gridCols * squareWidth; // gridCols * markerWidth + markerSep * (gridCols - 1);
        this.boardHeight = gridRows * squareWidth; // gridRows * markerWidth + markerSep * (gridRows - 1);
    }

    // -------------------------------------------------------------------------------------------


    @Override
    public Pnt2d[] getMarkerCorners(int id) {
        return cornerPoints.get(id);
    }


    @Override
    public int[] getIds() {
        return ids;
    }


    @Override
    public int getMarkerCount() {
        return ids.length;
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
        GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", gb.getBoardWidth(), gb.getBoardHeight());
        ImageProcessor ip = gb.createImage(1200);
        new ImagePlus("Board " + gb.getName(), ip).show();

        System.out.println("pdf path = " + gb.saveAsPdf(Path.of("tmp/board.pdf")));
    }
}
