package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math4.legacy.core.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static imagingbook.jaruco.Polygons.checkSame;
import static imagingbook.jaruco.Polygons.makePolygon;
import static imagingbook.jaruco.Polygons.simplify;
import static imagingbook.jaruco.QuadFInder.simplifyPolygon;
import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.*;

class QuadFInderTest {

    @Test
    void extractQuadTest() {
    }

    @Test
    void simplifyPolygonTest() {
        String RES_PATH = "test-contours/";     // these are long contours!
        runSimplifyTest(RES_PATH + "single-marker-5-0-contour.json", RES_PATH + "single-marker-5-0-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-1-contour.json", RES_PATH + "single-marker-5-1-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-2-contour.json", RES_PATH + "single-marker-5-2-corners.json");
        runSimplifyTest(RES_PATH + "single-marker-5-3-contour.json", RES_PATH + "single-marker-5-3-corners.json");
    }

    static final double polygonalApproxAccuracyRate =  0.03;  // from ArUco parameters

    void runSimplifyTest(String contourPath, String cornersPath) {
        double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
        double[][] corners1 = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
        // List<Pnt2d> cornerList2 = simplifyPolygon(makePolygon(contour), contour.length * polygonalApproxAccuracyRate);
        QuadFInder.MyPair result = simplifyPolygon(makePolygon(contour), contour.length * polygonalApproxAccuracyRate);

        List<Integer> cornerIdx = result.indxs();
        List<Pnt2d> cornerListAll = result.poly();
        // System.out.println("cornerListAll.length =  " + cornerListAll.size());
        List<Pnt2d> cornerList2 = new ArrayList<>(4);
        for (int i : cornerIdx) {
            cornerList2.add(cornerListAll.get(i));
            // System.out.println("added corner " + i + " = " + cornerListAll.get(i));
        }
        // System.out.println("corners1 = " + Polygons.toString(makePolygon(corners1)));
        System.out.println("corners2 = " + Polygons.toString(cornerList2));
        assertTrue(checkSame(makePolygon(corners1), cornerList2));
    }

}