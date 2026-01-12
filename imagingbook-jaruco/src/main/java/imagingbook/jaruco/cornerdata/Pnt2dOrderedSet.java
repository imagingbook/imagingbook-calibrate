package imagingbook.jaruco.cornerdata;

import imagingbook.common.geometry.basic.Pnt2d;

/**
 * To be implemented by enums in conjunction with {@link imagingbook.jaruco.util.JsonResource}.
 */
public interface Pnt2dOrderedSet {

    public Pnt2d[] getPoints();
}
