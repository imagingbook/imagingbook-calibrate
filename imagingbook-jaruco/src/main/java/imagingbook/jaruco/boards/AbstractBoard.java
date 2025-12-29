package imagingbook.jaruco.boards;

import com.lowagie.text.Rectangle;
import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.ArucoDictionary;

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
    public abstract ByteProcessor createImage(int width);
    public abstract String saveAsPdf(Path path);
}
