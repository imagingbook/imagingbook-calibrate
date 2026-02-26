package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;

class CheckerBoardSquare implements BoardElement {
    private final int col, row;
    private final Pnt2d[] corners;

    CheckerBoardSquare(AbstractMarkerBoard board, int col, int row) {
        // this.board = board;
        this.col = col;
        this.row = row;
        double x0 = board.fieldWidth * col;
        double y0 = board.fieldWidth * row;
        this.corners = new Pnt2d[] {
                Pnt2d.from(x0, y0),
                Pnt2d.from(x0 + board.fieldWidth, y0),
                Pnt2d.from(x0 + board.fieldWidth, y0 + board.fieldWidth),
                Pnt2d.from(x0, y0 + board.fieldWidth)
        };
    }

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
        return corners;
    }
}
