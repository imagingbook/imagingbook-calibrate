package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.marker.ArucoMarker;

public class BoardMarker extends ArucoMarker implements BoardElement {

    private final int col, row;           // column/row board position
    // private final Polygon2d corners;
    private final Pnt2d[] corners;

    BoardMarker(AbstractBoard board, ArucoMarker marker, int col, int row) {
        super(marker);
        this.col = col;
        this.row = row;
        double markerMargin = (board.squareWidth - board.markerWidth) / 2;
        double x0 = board.squareWidth * col + markerMargin;
        double y0 = board.squareWidth * row + markerMargin;
        this.corners = new Pnt2d[]{
                Pnt2d.from(x0, y0),
                Pnt2d.from(x0 + board.markerWidth, y0),
                Pnt2d.from(x0 + board.markerWidth, y0 + board.markerWidth),
                Pnt2d.from(x0, y0 + board.markerWidth)
        };

        // this.corners = new Polygon2d(
        //         Pnt2d.from(x0, y0),
        //         Pnt2d.from(x0 + board.markerWidth, y0),
        //         Pnt2d.from(x0 + board.markerWidth, y0 + board.markerWidth),
        //         Pnt2d.from(x0, y0 + board.markerWidth));

    }

    // --------------------------------------------------------------------------------------------

    @Override
    public int getColIndex() {
        return col;
    }

    @Override
    public int getRowIndex() {
        return row;
    }

    @Override
    public Pnt2d[] getCorners() {
        // return new Polygon2d(corners);  // TODO: CHECK, Polygon needed?!!
        return corners;
    }

}
