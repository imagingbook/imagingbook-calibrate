package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

class BlackSquare implements BoardElement {
    // private final AbstractBoard board;
    private final int u, v;
    private final Polygon2d corners;

    BlackSquare(AbstractBoard board, int u, int v) {
        // this.board = board;
        this.u = u;
        this.v = v;
        double x0 = board.squareWidth * u;
        double y0 = board.squareWidth * v;
        this.corners = new Polygon2d(
                Pnt2d.from(x0, y0),
                Pnt2d.from(x0 + board.squareWidth, y0),
                Pnt2d.from(x0 + board.squareWidth, y0 + board.squareWidth),
                Pnt2d.from(x0, y0 + board.squareWidth));
    }

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
