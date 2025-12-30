package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.jaruco.ArucoMarker;

public class BoardMarker extends ArucoMarker implements BoardElement {

    private final int u, v;           // x/y board position
    private Polygon2d corners;

    BoardMarker(AbstractBoard board, ArucoMarker marker, int u, int v) {
        super(marker);
        this.u = u;
        this.v = v;
        double markerMargin = (board.squareWidth - board.markerWidth) / 2;
        double x0 = board.squareWidth * u + markerMargin;
        double y0 = board.squareWidth * v + markerMargin;
        this.corners = new Polygon2d(
                Pnt2d.from(x0, y0),
                Pnt2d.from(x0 + board.markerWidth, y0),
                Pnt2d.from(x0 + board.markerWidth, y0 + board.markerWidth),
                Pnt2d.from(x0, y0 + board.markerWidth));
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public int getColumnIndex() {
        return u;
    }

    @Override
    public int getRowIndex() {
        return v;
    }

    @Override
    public Polygon2d getCorners() {
        return corners;
    }

}
