package imagingbook.jaruco.boards;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGraphics2D;
import com.lowagie.text.pdf.PdfWriter;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.image.ImageGraphics;
import imagingbook.jaruco.ArucoDictionary;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
                     ArucoDictionary dictionary, int borderBits, Rectangle pdfPageSize) {
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

    /**
     * Returns the board coordinates of corner points for the specified marker.
     * Each marker has 4 corners in CW order:
     * corners[0]: left-top corner
     * corners[1]: right-top corner
     * corners[2]: right-bottom corner
     * corners[3]: left-bottom corner
     * @param id the marker id
     * @ an array with the four corner points
     */
    @Override
    public Pnt2d[] getMarkerCorners(int id) {
        return cornerPoints.get(id);
    }

    /**
     * Returns an array with all marker ids.
     * @return all marker ids
     */
    @Override
    public int[] getIds() {
        return ids;
    }

    /**
     * Returns the number of markers on this board.
     * @return the number of markers
     */
    @Override
    public int getMarkerCount() {
        return ids.length;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Creates and returns a B/W image of this {@link GridBoard} by drawing shapes into a
     * {@link Graphics2D} canvas. See also {@link ImageGraphics}. Only the {@code width} of the
     * image is specified, while its {@code height} is derived from the board's dimensions.
     *
     * @param imgWidth the width of the output image (in pixels)
     * @return an image of this board
     */
    @Override
    public ByteProcessor createImage(int imgWidth) {
        int imgHeight = (int) Math.ceil(imgWidth * boardHeight / boardWidth);
        double scale = imgWidth / boardWidth;

        ByteProcessor ip = new ByteProcessor(imgWidth, imgHeight);
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
                double x2 = corners[2].getX() * scale;
                double mrkWidth = x2 - x0;
                dictionary.getMarker(idx, 0, borderBits).drawTo(g2, x0, y0, mrkWidth);
            }
        }
        return ip;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Saves the board graphics as a PDF (shortcut for {@link #saveAsPdf(Path, Rectangle, boolean)}).
     * @param path the file {@link Path}
     * @return the absolute file path of the stored document
     */
    @Override
    public String saveAsPdf(Path path) {
        return saveAsPdf(path, this.pdfPageSize, true);
    }

    /**
     * Saves the board graphics as a PDF.
     * The directory of the specified {@code path} must exist in advance (no new directories
     * are created) and must be writable, otherwise an exception is thrown. For example,
     * <pre>
     *     Path.of("tmp/board.pdf")</pre>
     * will try to save the document to file {@code <currendDir>/tmp/board.pdf}.
     * The {@code pageSize} argument may be {@code null}, in
     * which case the board's own PDF size is used. If this is {@code null} too, an exception
     * is thrown. Note that each pre-defined board listed in {@link GridBoardPredefined} do have a
     * specific document size and name.
     *
     * @param path the file {@link Path}
     * @param pageSize the document page size, e.g. {@link PageSize#A4}{@code .rotate()}.
     * @param showLegend set true to show the board's name in the PDF
     * @return the absolute file path of the stored document
     */
    public String saveAsPdf(Path path, Rectangle pageSize, boolean showLegend) {
        if (pageSize == null)
            pageSize = this.pdfPageSize;
        if (pageSize == null) {
            throw new IllegalArgumentException("no PDF page size specified");
        }

        try (Document document = new Document(pageSize)) {
            PdfWriter writer;
            try {
                writer = PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
            } catch (DocumentException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            document.open();
            document.addTitle(this.getName());
            document.addCreationDate();
            document.addProducer(this.getClass().getCanonicalName());

            PdfContentByte cb = writer.getDirectContent();
            float pageWidth = document.getPageSize().getWidth();    // unit is pt !
            float pageHeight = document.getPageSize().getHeight();
            double boardWidthPt = ptFromMm(this.boardWidth);
            double boardHeightPt = ptFromMm(this.boardHeight);

            // center board on page:
            double xOff = (pageWidth - boardWidthPt) / 2;
            double yOff = (pageHeight - boardHeightPt) / 2;

            Graphics2D g2 = new PdfGraphics2D(cb, pageWidth, pageHeight);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            for (int idx = 0; idx < getMarkerCount(); idx++) {
                Pnt2d[] corners = getMarkerCorners(idx);    // marke corners in board coordinates
                double x0 = ptFromMm(corners[0].getX());
                double y0 = ptFromMm(corners[0].getY());
                double x2 = ptFromMm(corners[2].getX());
                double mrkWidth = x2 - x0;
                dictionary.getMarker(idx, 0, borderBits).drawTo(g2, xOff + x0, yOff + y0, mrkWidth);
            }
            if (showLegend && this.getName() != null) {
                Font legendFont = new Font(Font.SANS_SERIF, Font.PLAIN, 6);
                Color legendColor = Color.BLACK;
                g2.setColor(legendColor);
                g2.setFont(legendFont);
                g2.drawString(this.getName(), 20, 20);
            }

            g2.dispose();
        }

        return path.toAbsolutePath().toString();
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", gb.getBoardWidth(), gb.getBoardHeight());
        ImageProcessor ip = gb.createImage(1200);
        new ImagePlus("Board " + gb.getName(), ip).show();

        // System.out.println("pdf path = " + gb.saveAsPdf(Path.of("tmp/board.pdf")));
    }
}
