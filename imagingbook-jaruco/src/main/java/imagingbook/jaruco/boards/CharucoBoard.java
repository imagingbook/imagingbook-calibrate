package imagingbook.jaruco.boards;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.image.ImageGraphics;
import imagingbook.jaruco.ArucoDictionary;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static imagingbook.jaruco.ArucoDictionaryPredefined.DICT_5X5_100;

public class CharucoBoard extends AbstractBoard {

    private final int[] ids;
    final List<Pnt2d[]> cornerPoints;
    final List<Pnt2d[]> chessboardCorners;
    List<Rectangle2D> squares;

    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. Marker ids are
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
     * @param gridCols number of markers in x direction
     * @param gridRows number of markers in y direction
     * @param markerWidth marker side length in real board space (in mm)
     * @param squareWidth size of the marker grid, marker position step width (in mm)
     * @param dictionary the dictionary of markers
     * @param borderBits the number of border bits around the inner of each marker
     * @param pdfPageSize the recommended PDF document size (may be null)
     */
    public CharucoBoard(int gridCols, int gridRows, double markerWidth, double squareWidth,
                        ArucoDictionary dictionary, int borderBits, Rectangle pdfPageSize) {
        super(gridCols, gridRows, markerWidth, squareWidth, dictionary, borderBits, pdfPageSize);

        double markerSep = squareWidth - markerWidth;
        // double diffSquareMarkerLength = (squareWidth - markerWidth) / 2;
        System.out.println("squareWidth = " + squareWidth);
        System.out.println("markerWidth = " + markerWidth);
        System.out.println("diffSquareMarkerLength = " + markerSep/2);

        // calculate markers' corner points in board coordinates
        cornerPoints = new ArrayList<>();
        List<Pnt2d[]> cPoints = new ArrayList<>();
        ArrayList<Integer> mIds = new ArrayList<>();

        squares = new ArrayList<>();

        int nextId = 0;
        for (int v = 0; v < gridRows; v++) {
            double y = v * squareWidth;
            for (int u = 0; u < gridCols; u++) {
                double x = u * squareWidth;
                if(v % 2 == u % 2) {
                    //continue; // black corner, no marker here
                    squares.add(new Rectangle2D.Double(x, y, squareWidth, squareWidth));
                }
                else {
                    Pnt2d[] corners = new Pnt2d[4];
                    corners[0] = Pnt2d.from(x + markerSep/2, y + markerSep/2);
                    corners[1] = corners[0].plus(markerWidth, 0);
                    corners[2] = corners[0].plus(markerWidth, markerWidth);
                    corners[3] = corners[0].plus(0, markerWidth);
                    cornerPoints.add(corners);
                    mIds.add(nextId);
                    nextId++;
                }
            }
        }

        //this.ids = markerIds.toArray(new Integer[0]);
        this.ids = new int[mIds.size()];
        Arrays.setAll(ids, (i) -> i); // fill ids = 0, 1, 2, ...

        this.boardWidth = gridCols * squareWidth;
        this.boardHeight = gridRows * squareWidth;
        chessboardCorners = new ArrayList<>();
    }

    // ----------------------------------------------------------------------------------

    @Override
    public Pnt2d[] getMarkerCorners(int id) {
        return cornerPoints.get(id);
    }

    @Override
    public int getMarkerCount() {
        return ids.length;
    }

    @Override
    public int[] getIds() {
        return ids;
    }

    @Override
    public ByteProcessor createImage(int imgWidth) {
        int height = (int) Math.ceil(imgWidth * boardHeight / boardWidth);
        double scale = imgWidth / boardWidth;

        ByteProcessor ip = new ByteProcessor(imgWidth, height);
        ip.setValue(255);
        ip.fill();
        try (ImageGraphics ig = new ImageGraphics(ip)) {
            ig.setAntialiasing(false);  // turn off to avoid thin lines between boxes
            Graphics2D g2 = ig.getGraphics2D();
            // draw each marker
            for (int idx = 0; idx < getMarkerCount(); idx++) {
                Pnt2d[] corners = getMarkerCorners(idx);
                double x0 = corners[0].getX() * scale;
                double y0 = corners[0].getY() * scale;
                double mw = markerWidth * scale;
                dictionary.getMarker(idx, 0, borderBits).drawTo(g2, x0, y0, mw);
            }
            g2.setColor(Color.black);
            for (Rectangle2D sqr : squares) {
                g2.fill(scale(sqr, scale));
            }
        }
        return ip;
    }

    private Rectangle2D scale(Rectangle2D rect, double scale) {
        return new Rectangle2D.Double(
                rect.getX()*scale, rect.getY()*scale,
                rect.getWidth()*scale, rect.getHeight()*scale);
    }

    @Override
    public String saveAsPdf(Path path) {
        return "";
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        // GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        CharucoBoard board = new CharucoBoard(12, 8, 15.0, 21.0, DICT_5X5_100.getInstance(), 1, PageSize.A4.rotate());
        System.out.printf("board size = %.2f x %.2f mm\n", board.getBoardWidth(), board.getBoardHeight());
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

        // System.out.println("pdf path = " + gb.saveAsPdf(Path.of("tmp/board.pdf")));
    }

}
