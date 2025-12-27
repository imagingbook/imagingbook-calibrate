package imagingbook.jaruco.boards;

import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGraphics2D;
import com.lowagie.text.pdf.PdfWriter;
import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.image.ImageGraphics;
import imagingbook.core.Info;
import imagingbook.jaruco.ArucoDictionary;

import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lowagie.text.Document;

/**
 * Represents a marke board with all markers in the same plane and in a regular M x N grid layout.
 * The board contains only markers from the specified dictionary, without any additional
 * geometric shapes.
 */
public class GridBoard {

    private final int nMarkersX;
    private final int nMarkersY;
    private final double markerWidth;
    private final double markerSeparation;
    private final ArucoDictionary dictionary;
    private final int borderBits;
    private final int[] ids;
    private final List<Pnt2d[]> cornerPoints;
    private final double boardWidth;
    private final double boardHeight;
    private String name = "unnamed";         // name of this board (used by predefined boards)

    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. Marker ids are
     * assigned sequentially starting from zero in row-major order, e.g.,
     * <pre>
     *     0  1  2  3  4  5
     *     6  7  8  ...
     * </pre>
     * on a 6 x N board. All markers are arranged in canonical orientation (rotation 0).
     * The board has no surrounding border, i.e., the first marker is placed at the coordinate
     * origin. All board coordinates are in mm.
     *
     * @param nMarkersX number of markers in x direction
     * @param nMarkersY number of markers in y direction
     * @param markerWidth marker side length in real board space (in mm)
     * @param markerSeparation space between two markers (in mm)
     * @param dictionary the dictionary of markers
     * @param borderBits the number of border bits around the inner of each marker
     */
    public GridBoard(int nMarkersX, int nMarkersY, double markerWidth, double markerSeparation,
                     ArucoDictionary dictionary, int borderBits) {
        this.nMarkersX = nMarkersX;
        this.nMarkersY = nMarkersY;
        this.markerWidth = markerWidth;
        this.markerSeparation = markerSeparation;
        this.dictionary = dictionary;
        this.borderBits = borderBits;

        double onePin = markerWidth / (dictionary.getMarkerSize() + 2);    // size of one marker bitfield
        if (markerSeparation < onePin * 0.7) {
            System.out.println("Marker border " + markerSeparation + " is less than 70% of ArUco pin size " + onePin);
            System.out.println("Please increase markerSeparation or decrease markerLength for stable board detection");
        }
        int totalMarkers = nMarkersX * nMarkersY;
        if (totalMarkers > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + totalMarkers);
        }
        this.ids = new int[totalMarkers];
        Arrays.setAll(ids, (i) -> i); // fill ids = 0, 1, 2, ...

        // calculate markers' corner points in board coordinates
        this.cornerPoints = new ArrayList<>();
        for (int y = 0; y < nMarkersY; y++) {
            for (int x = 0; x < nMarkersX; x++) {
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

        this.boardWidth = nMarkersX * markerWidth + markerSeparation * (nMarkersX - 1);
        this.boardHeight = nMarkersY * markerWidth + markerSeparation * (nMarkersY - 1);

        // this.rightBottomBorder =
        //         Pnt2d.from(nMarkersX * markerWidth + markerSeparation * (nMarkersX - 1),
        //                    nMarkersY * markerWidth + markerSeparation * (nMarkersY - 1));
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
    public int getnMarkersX() {
        return nMarkersX;
    }

    /**
     * Returns the number of board markers in vertical direction.
     * @return number of vertical markers
     */
    public int getnMarkersY() {
        return nMarkersY;
    }

    /**
     * Returns the board coordinates for corner points of the specified marker.
     * Each marker has 4 corners in CW order:
     * corners[0]: left-top corner
     * corners[1]: right-top corner
     * corners[2]: right-bottom corner
     * corners[3]: left-bottom corner
     * @param id the marker id
     * @ an array with the four corner points
     */
    Pnt2d[] getCorners(int id) {
        return cornerPoints.get(id);
    }

    public int[] getIds() {
        return ids;
    }

    public int getMarkerCount() {
        return ids.length;
    }

    public double getMarkerWidth() {
        return markerWidth;
    }

    public double getMarkerSeparation() {
        return markerSeparation;
    }

    public double getBoardWidth() {
        return boardWidth;
    }

    public double getBoardHeight() {
        return boardHeight;
    }


    void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // /**
    //  *
    //  * @param width the width of the output image in pixels
    //  * @param marginSize minimum margins (in pixels) of the board in the output image
    //  * @return an image of this board
    //  */
    // public ByteProcessor generateImage(int width, int marginSize) {
    //     double contentWidth = rightBottomBorder.getX();
    //     double contentHeight = rightBottomBorder.getY();
    //     int innerWidth = width - 2 * marginSize;
    //     int innerHeight = (int) Math.ceil(innerWidth * contentHeight / contentWidth);
    //     int height = innerHeight + 2 * marginSize;
    //     double scale = innerWidth / contentWidth;
    //     int xOff = marginSize;
    //     int yOff = marginSize;
    //
    //     ByteProcessor ip = new ByteProcessor(width, height);
    //     ip.setValue(255);
    //     ip.fill();
    //     ip.setValue(0);
    //
    //     for (int idx = 0; idx < getMarkerCount(); idx++) {
    //         Pnt2d[] corners = getCorners(idx);
    //         int x0 = (int) Math.round(corners[0].getX() * scale);
    //         int y0 = (int) Math.round(corners[0].getY() * scale);
    //         int x2 = (int) Math.round(corners[2].getX() * scale);
    //         // int y2 = (int) Math.round(corners[2].getY() * scale);
    //         // ip.fillRect(xOff + x0, yOff + y0, x2 - x0, y2 - y0);
    //         // System.out.printf("filling %d: %d %d %d %d\n", idx, xOff + x0, yOff + y0, x2 - x0, y2 - y0);
    //         ImageProcessor markerIp = dictionary.getMarkerImage(idx, 0, this.borderBits).resize(x2 - x0);
    //         // double markerScale = 5; // (x2 - x0) / (double) markerIp.getWidth();
    //         // System.out.println("markerScale = " + markerScale);
    //         // markerIp.scale(markerScale, markerScale);
    //         // markerIp.resize(x2 - x0);
    //         // new ImagePlus("Marker " + idx, markerIp).show();
    //         ip.insert(markerIp, xOff + x0, yOff + y0);
    //     }
    //     return ip;
    // }


    /**
     * Generates and returns a b/w image of this {@link GridBoard} by drawing shapes into a
     * {@link Graphics2D} canvas. See also {@link ImageGraphics}.
     *
     * @param width the width of the output image in pixels
     * @param marginSize margins (in pixels) of the board in the output image
     * @return an image of this board
     */
    public ByteProcessor generateImage(int width, int marginSize) {
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
                Pnt2d[] corners = getCorners(idx);
                double x0 = corners[0].getX() * scale;
                double y0 = corners[0].getY() * scale;
                double x2 = corners[2].getX() * scale;
                double mrkWidth = x2 - x0;
                dictionary.drawTo(g2, idx, 0, borderBits, xOff + x0, yOff + y0, mrkWidth);
            }
        }
        return ip;
    }

    double mmFromPnts(double points) {
        return points * 25.4 / 72;
    }

    double pntsFromMm(double mm) {
        return mm * 73 / 25.4;
    }

    public String saveAsPdf(String filename) {
        // Rectangle rect = PageSize.A4;
        Path currentDir = Paths.get("tmp");
        Path path = currentDir.resolve(filename);
        boolean embedCoreFonts = true;

        try (Document document = new Document(PageSize.A4.rotate())) {
            System.out.printf("document size = %.2f x %.2f mm\n",
                    mmFromPnts(document.getPageSize().getWidth()),
                    mmFromPnts(document.getPageSize().getHeight()));

            PdfWriter writer = null;
            try {
                writer = PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
            } catch (DocumentException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            document.open();
            document.addTitle("My Title");
            document.addAuthor("The Author");
            document.addSubject("The subject");
            document.addKeywords("The keywords");
            document.addCreationDate();
            document.addCreator("Me");
            document.addProducer(this.getClass().getSimpleName() + " " + Info.getVersionInfo());

            PdfContentByte cb = writer.getDirectContent();

            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();
            Graphics2D g2 = new PdfGraphics2D(cb, width, height); // no core font embedding

            double scale = 1.0 / mmFromPnts(1);
            double xOff = pntsFromMm(12.5);
            double yOff = pntsFromMm(20);
            for (int idx = 0; idx < getMarkerCount(); idx++) {
                Pnt2d[] corners = getCorners(idx);
                double x0 = corners[0].getX() * scale;
                double y0 = corners[0].getY() * scale;
                double x2 = corners[2].getX() * scale;
                double mrkWidth = x2 - x0;
                dictionary.drawTo(g2, idx, 0, borderBits, xOff + x0, yOff + y0, mrkWidth);
            }

            g2.dispose();
        }

        return path.toAbsolutePath().toString();
    }

    // -------------------------------------------------------------------


    // public static void drawPixels() {
    //     GridBoard gb = new GridBoard(7, 5, 0.02, 0.01, DICT_5X5_100.getInstance(), 2);
    //     int i = 0;
    //     for (int y = 0; y < gb.getMarkersY(); y++) {
    //         for (int x = 0; x < gb.getMarkersX(); x++) {
    //             System.out.print(i + ": " + gb.getCorners(i)[0] + " | ");
    //             i++;
    //         }
    //         System.out.println();
    //     }
    //     System.out.println("getRightBottomBorder = " + gb.getRightBottomBorder());
    //
    //     ImageProcessor ip = gb.generateImage(1200, 10);
    //     new ImagePlus("Board", ip).show();
    // }

    // public static void drawToImage() {
    //     // GridBoard gb = new GridBoard(7, 5, 20.0, 10.0, DICT_5X5_100.getInstance(), 2);
    //     GridBoard gb = GridBoardPredefined.DICT_5X5_BOARD_8x5_A4.getInstance();
    //     // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_BOARD_12x8_A4.getInstance();
    //     System.out.printf("board size = %.2f x %.2f mm\n", gb.getBoardWidth(), gb.getBoardHeight());
    //     ImageProcessor ip = gb.generateImage(1200, 10);
    //     new ImagePlus("Board", ip).show();
    // }

    public static void main(String[] args) {
        GridBoard gb = GridBoardPredefined.DICT_5X5_BOARD_8x5_A4.getInstance();
        // GridBoard gb = GridBoardPredefined.DICT_5X5_1000_BOARD_12x8_A4.getInstance();
        System.out.printf("board size = %.2f x %.2f mm\n", gb.getBoardWidth(), gb.getBoardHeight());
        // ImageProcessor ip = gb.generateImage(1200, 10);
        // new ImagePlus("Board " + gb.getName(), ip).show();

        System.out.println("pdf path = " + gb.saveAsPdf("board.pdf"));
    }
}
