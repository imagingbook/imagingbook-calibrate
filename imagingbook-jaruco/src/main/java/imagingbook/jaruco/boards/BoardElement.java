package imagingbook.jaruco.boards;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;

/**
 * A geometrical element which knows the grid position on the associated board and its outline.
 */
public interface BoardElement {

    /**
     * Returns the horizontal grid index of this item.
     * @return the horizontal grid index
     */
    public int getColumnIndex();

    /**
     * Returns the vertical grid index of this item.
     * @return the hvertical grid index
     */
    public int getRowIndex();

    /**
     * Returns the board coordinates of the four corners of this item.
     * @return the four corners
     */
    public Polygon2d getCorners();

    /**
     * Returns the coordinates of the i-th corner (CW in Y-down coordinate system).
     * @param i
     * @return
     */
    public default Pnt2d getCorner(int i) {
        return getCorners().getPnt(i);
    }

}
