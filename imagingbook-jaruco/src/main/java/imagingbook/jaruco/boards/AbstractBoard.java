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

public abstract class AbstractBoard {

    final int gridCols;
    final int gridRows;
    final double markerWidth;
    final double squareWidth;
    final ArucoDictionary dictionary;
    final int borderBits;
    final Rectangle pdfPageSize;

    double boardWidth;
    double boardHeight;


    String name = "none";         // name of this board (used by predefined boards)


    AbstractBoard(int gridCols, int gridRows, double markerWidth, double squareWidth,
                         ArucoDictionary dictionary, int borderBits, Rectangle pdfPageSize) {
        this.gridCols = gridCols;
        this.gridRows = gridRows;
        this.markerWidth = markerWidth;
        this.squareWidth = squareWidth;
        this.dictionary = dictionary;
        this.borderBits = borderBits;
        this.pdfPageSize = pdfPageSize;
    }

    // --------------------------------------------------------------------------------------------

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

    // ------------------------

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
    public int getGridCols() {
        return gridCols;
    }

    /**
     * Returns the number of board markers in vertical direction.
     * @return number of vertical markers
     */
    public int getGridRows() {
        return gridRows;
    }

    /**
     * Returns the size (width and height) of the markers on this board
     * (in millimeters).
     * @return marker size (in mm)
     */
    public double getMarkerWidth() {
        return markerWidth;
    }

    /**
     * Returns the width of the marker grid, marker position step width (in mm)
     * @ width of the marker grid (in mm)
     */
    public double getSquareWidth() {
        return squareWidth;
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

    // --------------------------------------------------------------------------------------------

    static double mmFromPt(double pt) {
        return pt * 25.4 / 72;
    }

    static double ptFromMm(double mm) {
        return mm * 72 / 25.4;
    }

    // -----------------------------------------------------------------------------

    public abstract Pnt2d[] getMarkerCorners(int id);
    public abstract int getMarkerCount();
    public abstract int[] getIds();

    // -----------------------------------------------------------------------------

    /**
     * Draw this board to a {@link Graphics2D} canvas.
     * The specified {@code scale} must be chosen to map board units (mm) to canvas units.
     * For example, drawing to a {@link PdfGraphics2D} with unit 'pt' requires the scale pt/mm
     * (= 72/25.4 ~ 2.83).
     * @param g2 the {@link Graphics2D} canvas
     * @param scale the ratio of canvas units to board units.
     * @param xOffset the x-offset (in canvas units)
     * @param yOffset the y-offset (in canvas units)
     */
    abstract void drawBoard(Graphics2D g2, double scale, double xOffset, double yOffset);

    /**
     * Creates and returns a B/W image of this {@link GridBoard} by drawing shapes into a
     * {@link Graphics2D} canvas. See also {@link ImageGraphics}. Only the {@code width} of the
     * image is specified, while its {@code height} is derived from the board's dimensions.
     *
     * @param imgWidth the width of the output image (in pixels)
     * @return an image of this board
     */
    public ByteProcessor createImage(int imgWidth) {
        int imgHeight = (int) Math.ceil(imgWidth * boardHeight / boardWidth);
        double scale = imgWidth / boardWidth;   // scale in pixel/mm
        // System.out.println("scale " + scale);
        ByteProcessor ip = new ByteProcessor(imgWidth, imgHeight);
        ip.setValue(255);
        ip.fill();
        try (ImageGraphics ig = new ImageGraphics(ip)) {
            ig.setAntialiasing(false);  // turn off to avoid thin lines between boxes
            Graphics2D g2 = ig.getGraphics2D();
            drawBoard(g2, scale, 0, 0);
        }
        return ip;
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
    // public abstract String saveAsPdf(Path path, Rectangle pageSize, boolean showLegend);
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

            writeToPdf(document, writer, showLegend);   // by implementing class
        }

        return path.toAbsolutePath().toString();
    }

    /**
     * Saves the board graphics as a PDF (shortcut for {@link #saveAsPdf(Path, Rectangle, boolean)}).
     * @param path the file {@link Path}
     * @return the absolute file path of the stored document
     */
    public String saveAsPdf(Path path) {
        return saveAsPdf(path, this.pdfPageSize, true);
    }

    void writeToPdf(Document document, PdfWriter writer, boolean showLegend) {
        float pageWidth = document.getPageSize().getWidth();    // unit is pt !
        float pageHeight = document.getPageSize().getHeight();
        double boardWidthPt = ptFromMm(this.boardWidth);
        double boardHeightPt = ptFromMm(this.boardHeight);

        // center board on page:
        double xOff = (pageWidth - boardWidthPt) / 2;
        double yOff = (pageHeight - boardHeightPt) / 2;
        double scale = ptFromMm(1);
        // System.out.println("pdf scale " + scale);

        PdfContentByte cb = writer.getDirectContent();
        Graphics2D g2 = new PdfGraphics2D(cb, pageWidth, pageHeight);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        drawBoard(g2, scale, xOff, yOff);

        if (showLegend && this.getName() != null) {
            Font legendFont = new Font(Font.SANS_SERIF, Font.PLAIN, 6);
            Color legendColor = Color.BLACK;
            g2.setColor(legendColor);
            g2.setFont(legendFont);
            g2.drawString(this.getName(), 20, 20);
        }

        g2.dispose();
    }
}
