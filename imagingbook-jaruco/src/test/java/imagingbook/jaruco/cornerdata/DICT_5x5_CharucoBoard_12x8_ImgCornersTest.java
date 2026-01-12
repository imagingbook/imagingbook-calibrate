package imagingbook.jaruco.cornerdata;

import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DICT_5x5_CharucoBoard_12x8_ImgCornersTest {

    @Test
    void getPointsTest() {
        for (var item : DICT_5x5_CharucoBoard_12x8_ImgCorners.values()) {
            Pnt2d[] corners = item.getPoints();
            assertNotNull(corners);
            assertTrue(corners.length > 0);
        }
    }
}