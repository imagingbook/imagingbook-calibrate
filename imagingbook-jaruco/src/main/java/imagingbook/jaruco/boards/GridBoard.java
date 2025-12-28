package imagingbook.jaruco.boards;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGraphics2D;
import com.lowagie.text.pdf.PdfWriter;
import ij.process.ByteProcessor;
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
public class GridBoard {

    private final int markerCols;
    private final int markerRows;
    private final double markerWidth;
    private final double markerSeparation;
    private final ArucoDictionary dictionary;
    private final int borderBits;
    private final int[] ids;
    private final List<Pnt2d[]> cornerPoints;
    private final double boardWidth;
    private final double boardHeight;
    private String name = "none";         // name of this board (used by predefined boards)
    private Rectangle pdfPageSize;

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
     *
     * @param markerCols number of markers in x direction
     * @param markerRows number of markers in y direction
     * @param markerWidth marker side length in real board space (in mm)
     * @param markerSeparation space between two markers (in mm)
     * @param dictionary the dictionary of markers
     * @param borderBits the number of border bits around the inner of each marker
     * @param pdfPageSize the recommended PDF document size (may be null)
     */
    public GridBoard(int markerCols, int markerRows, double markerWidth, double markerSeparation,
                     ArucoDictionary dictionary, int borderBits, Rectangle pdfPageSize) {
        this.markerCols = markerCols;
        this.markerRows = markerRows;
        this.markerWidth = markerWidth;
        this.markerSeparation = markerSeparation;
        this.dictionary = dictionary;
        this.borderBits = borderBits;
        this.pdfPageSize = pdfPageSize;

        double onePin = markerWidth / (dictionary.getMarkerSize() + 2);    // size of one marker bitfield
        if (markerSeparation < onePin * 0.7) {
            System.out.println("Marker border " + markerSeparation + " is less than 70% of ArUco pin size " + onePin);
            System.out.println("Please increase markerSeparation or decrease markerLength for stable board detection");
        }
        int totalMarkers = markerCols * markerRows;
        if (totalMarkers > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + totalMarkers);
        }
        this.ids = new int[totalMarkers];
        Arrays.setAll(ids, (i) -> i); // fill ids = 0, 1, 2, ...

        // calculate markers' corner points in board coordinates
        this.cornerPoints = new ArrayList<>();
        for (int y = 0; y < markerRows; y++) {
            for (int x = 0; x < markerCols; x++) {
                Pnt2d[] corners = new Pnt2d[4];
                corners[0] = Pnt2d.from(
                        x * (markerWidth + markerSeparation),
                        y * (markerWidth + markerSeparation));
                corners[1] = corners[0].plus(markerWidth, 0);
                corners[2] = corners[0].plus(markerWidth, markerWidth);
                corners[3] = corners[0].plus(0, markerWidth);
                cornerPoints.add(corners);
            }
        }

        this.boardWidth = markerCols * markerWidth + markerSeparation * (markerCols - 1);
        this.boardHeight = markerRows * markerWidth + markerSeparation * (markerRows - 1);
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Returns the dictionary associated with this board.
     * @return the dictionary
     */
    public ArucoDictionary getDictionary() {
        return dictionary;
    }

    /**
     * Returns the number of board markers in horizontal direction.
     * @return number of horizontal markers
     */
    public int getMarkerCols() {
        return markerCols;
    }

    /**
     * Returns the number of board markers in vertical direction.
     * @return number of vertical markers
     */
    public int getMarkerRows() {
        return markerRows;
    }

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
    Pnt2d[] getMarkerCorners(int id) {
        return cornerPoints.get(id);
    }

    /**
     * Returns an array with all marker ids.
     * @return all marker ids
     */
    public int[] getIds() {
        return ids;
    }

    /**
     * Returns the number of markers on this board.
     * @return the number of markers
     */
    public int getMarkerCount() {
        return ids.length;
    }

    /**
     * Returns the size (width and height) of the markers on this board
     * (in millimeters).
     * @return marker size (in mm)
     */
    public double getMarkerWidth() {
        return markerWidth;
    }

    /** Returns the space between adjacent markers (in millimeters).
     * @ space between markers (in mm)
     */
    public double getMarkerSeparation() {
        return markerSeparation;
    }

    /**
     * Returns the overall width of this board, which is the width of the bounding rectangle
     * comprising all markers (in millimeters).
     * @return the overall width of this board (in mm)
     */
    public double getBoardWidth() {
        return boardWidth;
    }

    /**
     * Returns the overall height of this board, which is the height of the bounding rectangle
     * comprising all markers (in millimeters).
     * @return the overall height of this board (in mm)
     */
    public double getBoardHeight() {
        return boardHeight;
    }

    /**
     * Set the name of this board (used by {@link GridBoardPredefined#getInstance()}).
     * @param name the board's name
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this board.
     * @return the board's name
     */
    public String getName() {
        return this.name;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Creates and returns a B/W image of this {@link GridBoard} by drawing shapes into a
     * {@link Graphics2D} canvas. See also {@link ImageGraphics}.
     * Only the {@code width} of the image is specified, while its {@code height} is derived from the board's
     * dimensions.
     *
     * @param width the width of the output image (in pixels)
     * @param marginSize margins (in pixels) of the board in the output image
     * @return an image of this board
     */
    public ByteProcessor createImage(int width, int marginSize) {
        int innerWidth = width - 2 * marginSize;
        int innerHeight = (int) Math.ceil(innerWidth * boardHeight / boardWidth);
        int height = innerHeight + 2 * marginSize;
        double scale = innerWidth / boardWidth;
        double xOff = marginSize;
        double yOff = marginSize;

        ByteProcessor ip = new ByteProcessor(width, height);
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
                dictionary.getMarker(idx, 0, borderBits).drawTo(g2, xOff + x0, yOff + y0, mrkWidth);
            }
        }
        return ip;
    }

    // --------------------------------------------------------------------------------------------

    private static double mmFromPt(double pt) {
        return pt * 25.4 / 72;
    }

    private static double ptFromMm(double mm) {
        return mm * 72 / 25.4;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Saves the board graphics as a PDF (shortcut for {@link #saveAsPdf(Path, Rectangle, boolean)}).
     * @param path the file {@link Path}
     * @return the absolute file path of the stored document
     */
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
        GridBoard gb = GridBoardPredefined.DICT_5X5_GridBoard_8x5_A4L.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_GridBoard_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", gb.getBoardWidth(), gb.getBoardHeight());
        // ImageProcessor ip = gb.createImage(1200, 10);
        // new ImagePlus("Board " + gb.getName(), ip).show();

        System.out.println("pdf path = " + gb.saveAsPdf(Path.of("tmp/board.pdf")));
    }
}
