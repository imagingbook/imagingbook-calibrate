package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.ArucoDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static imagingbook.jaruco.ArucoDictionaryPredefined.DICT_5X5_100;

/**
 * Represents a marke board with all markers in the same plane and in a regular M x N grid layout.
 * The board contains only markers from the specified dictionary, without any additional
 * geometric shapes.
 */
public class GridBoard {

    private final int markersX;
    private final int markersY;
    private final double markerLength;
    private final double markerSeparation;
    private final ArucoDictionary dictionary;
    private final int borderBits;
    private final int[] ids;
    private final List<Pnt2d[]> objPoints;
    private final Pnt2d rightBottomBorder;

    /**
     * Constructor. Creates a board with Aruco markers placed on a rectangular grid. Marker ids are
     * assigned sequentially starting from zero in row-major order, e.g.,
     * <pre>
     *     0  1  2  3  4  5
     *     6  7  8  ...
     * </pre>
     * on a 6 x N board.
     *
     * @param markersX number of markers in x directions
     * @param markersY number of markers in y directions
     * @param markerLength marker side length (normally in meters)
     * @param markerSeparation separation between two markers (same unit as markerLength)
     * @param dictionary dictionary of markers indicating the type of markers
     * @param borderBits
     */
    public GridBoard(int markersX, int markersY, double markerLength, double markerSeparation,
                     ArucoDictionary dictionary, int borderBits) {
        this.markersX = markersX;
        this.markersY = markersY;
        this.markerLength = markerLength;
        this.markerSeparation = markerSeparation;
        this.dictionary = dictionary;
        this.borderBits = borderBits;

        double onePin = markerLength / (dictionary.getMarkerSize() + 2);    // size of one marker bitfield
        if (markerSeparation < onePin * 0.7) {
            System.out.println("Marker border " + markerSeparation + " is less than 70% of ArUco pin size " + onePin);
            System.out.println("Please increase markerSeparation or decrease markerLength for stable board detection");
        }
        int totalMarkers = markersX * markersY;
        if (totalMarkers > dictionary.getNumberOfCodes()) {
            throw new IllegalArgumentException("number of board markers exceeds dictionary size: " + totalMarkers);
        }
        this.ids = new int[totalMarkers];
        Arrays.setAll(ids, i -> i);

        // calculate Board objPoints
        this.objPoints = new ArrayList<Pnt2d[]>();
        for (int y = 0; y < markersY; y++) {
            for (int x = 0; x < markersX; x++) {
                Pnt2d[] corners = new Pnt2d[4];
                corners[0] = Pnt2d.from(
                        x * (markerLength + markerSeparation),
                        y * (markerLength + markerSeparation));
                corners[1] = corners[0].plus(markerLength, 0);
                corners[2] = corners[0].plus(markerLength, markerLength);
                corners[3] = corners[0].plus(0, markerLength);
                objPoints.add(corners);
            }
        }

        this.rightBottomBorder =
                Pnt2d.from(markersX * markerLength + markerSeparation * (markersX - 1),
                           markersY * markerLength + markerSeparation * (markersY - 1));
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
    public int getMarkersX() {
        return markersX;
    }

    /**
     * Returns the number of board markers in vertical direction.
     * @return number of vertical markers
     */
    public int getMarkersY() {
        return markersY;
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
        return objPoints.get(id);
    }

    public int[] getIds() {
        return ids;
    }

    public int getMarkerCount() {
        return ids.length;
    }

    /**
     * Returns the board coordinate of the bottom right corner of the board.
     * @return the bottom right corner
     */
    Pnt2d getRightBottomBorder() {
        return rightBottomBorder;
    }


    /**
     *
     * @param width the width of the output image in pixels
     * @param marginSize minimum margins (in pixels) of the board in the output image
     * @return an image of this board
     */
    public ByteProcessor generateImage(int width, int marginSize) {
        double contentWidth = rightBottomBorder.getX();
        double contentHeight = rightBottomBorder.getY();
        int innerWidth = width - 2 * marginSize;
        int innerHeight = (int) Math.ceil(innerWidth * contentHeight / contentWidth);
        int height = innerHeight + 2 * marginSize;
        double scale = innerWidth / contentWidth;
        int xOff = marginSize;
        int yOff = marginSize;

        ByteProcessor ip = new ByteProcessor(width, height);
        ip.setValue(255);
        ip.fill();
        ip.setValue(0);

        for (int idx = 0; idx < getMarkerCount(); idx++) {
            Pnt2d[] corners = getCorners(idx);
            int x0 = (int) Math.round(corners[0].getX() * scale);
            int y0 = (int) Math.round(corners[0].getY() * scale);
            int x2 = (int) Math.round(corners[2].getX() * scale);
            // int y2 = (int) Math.round(corners[2].getY() * scale);
            // ip.fillRect(xOff + x0, yOff + y0, x2 - x0, y2 - y0);
            // System.out.printf("filling %d: %d %d %d %d\n", idx, xOff + x0, yOff + y0, x2 - x0, y2 - y0);
            ImageProcessor markerIp = dictionary.getMarkerImage(idx, 0, this.borderBits).resize(x2 - x0);
            // double markerScale = 5; // (x2 - x0) / (double) markerIp.getWidth();
            // System.out.println("markerScale = " + markerScale);
            // markerIp.scale(markerScale, markerScale);
            // markerIp.resize(x2 - x0);
            // new ImagePlus("Marker " + idx, markerIp).show();
            ip.insert(markerIp, xOff + x0, yOff + y0);
        }

        return ip;
    }
    // -------------------------------------------------------------------

    public static void main(String[] args) {
        GridBoard gb = new GridBoard(7, 5, 0.02, 0.01, DICT_5X5_100.getInstance(), 2);
        int i = 0;
        for (int y = 0; y < gb.getMarkersY(); y++) {
            for (int x = 0; x < gb.getMarkersX(); x++) {
                System.out.print(i + ": " + gb.getCorners(i)[0] + " | ");
                i++;
            }
            System.out.println();
        }
        System.out.println("getRightBottomBorder = " + gb.getRightBottomBorder());

        ImageProcessor ip = gb.generateImage(1200, 10);
        new ImagePlus("Board", ip).show();
    }
}
