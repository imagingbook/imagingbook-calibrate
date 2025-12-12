package imagingbook.jaruco.pyramid;

import ij.process.ByteProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GaussianPyramidTest {
    static int W = 1000;
    static int H = 700;
    static ByteProcessor IP = new ByteProcessor(W, H);


    @Test
    void generalLevelTest() {
        int testVal = 117;
        IP.set(testVal);
        GaussianPyramid pyramid = new GaussianPyramid(IP, 5);
        Pnt2d ctrOrig = Pnt2d.from(0.5 * W, 0.5 * H);

        for (int k = 0; k < 5; k++) {
            // all levels exist
            assertNotNull(pyramid.getLevel(k));
            // all level images exist
            assertNotNull(pyramid.getLevel(k).getImage());
            // image widths and fields match
            assertEquals(
                    pyramid.getLevel(k).getWidth(),
                    pyramid.getLevel(k).getImage().getWidth());
            // image heights and fields match
            assertEquals(
                    pyramid.getLevel(k).getHeight(),
                    pyramid.getLevel(k).getImage().getHeight());

            // pixel values at centers are all the same
            Pnt2d ctrLevel = pyramid.getLevelPosition(ctrOrig, k);
            int u = ctrLevel.getXint();
            int v = ctrLevel.getYint();
            assertEquals(
                    testVal,
                    pyramid.getLevel(k).getImage().getPixel(u, v));
        }

        for (int k = 1; k < 5; k++) {
            // level has half width of previous
            assertEquals(
                    pyramid.getLevel(k-1).getWidth() / 2,
                    pyramid.getLevel(k).getWidth());
            // level has half height of previous
            assertEquals(
                    pyramid.getLevel(k-1).getHeight() / 2,
                    pyramid.getLevel(k).getHeight());
            // level has half scale of previous
            assertEquals(
                    pyramid.getLevel(k-1).getScale() / 2,
                    pyramid.getLevel(k).getScale());
            // level images are disjoint, don't share data
            assertNotSame(
                    pyramid.getLevel(k-1).getImage(),
                    pyramid.getLevel(k).getImage());
        }
    }

    @Test
    void getRealPositionTest() {
    }

    @Test
    void decimateTest() {
    }
}