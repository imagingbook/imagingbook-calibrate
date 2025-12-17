package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.fitting.points.ProjectiveFit2d;
import imagingbook.common.math.Matrix;
import imagingbook.common.math.PrintPrecision;
import imagingbook.jaruco.util.Polygons;
import org.junit.jupiter.api.Test;

import java.util.List;

import static imagingbook.jaruco.JUnit5TestUtils.assert2dArrayEquals;
import static imagingbook.jaruco.util.Polygons.makePolygon;
import static imagingbook.jaruco.util.JsonUtils.loadObject;
import static org.junit.jupiter.api.Assertions.*;

class QuadHomographyFitTest {

    static List< Pnt2d> contour4 = Polygons.makePolygon(10, 7,  23, 1,  19, 21,  12, 15);
    static SegmentedContour segCtr4 = new SegmentedContour(List.of(0, 1, 2, 3), contour4);
    static List< Pnt2d> UnitSquare =Polygons.makePolygon(0, 0, 1, 0, 1, 1, 0, 1);

    static final double polygonalApproxAccuracyRate =  0.03;  // from ArUco parameters

    @Test
    void fitFourPointsOnlyTest() {
        QuadHomographyFit fit = new QuadHomographyFit(segCtr4);
        double[][] A = fit.getTransformationMatrix();
        assertNotNull(A);

        double[][] Aexpected =
                {{-1.66282866, 0.41570717, 13.71833648},
                {-0.23251418, -0.50378072, 5.85160681},
                {-1.11462451, 0.52534800, 1.00000000}};

        assert2dArrayEquals(Aexpected, A, 1e-6);

        System.out.println("A =" + Matrix.toString(A));
    }

    // @Test
    void fitFourPointsOnlyTest_projective() {  // just for comparison in 4-point case
        ProjectiveFit2d fit = new ProjectiveFit2d(contour4.toArray(new Pnt2d[0]), UnitSquare.toArray(new Pnt2d[0]));
        double[][] A = fit.getTransformationMatrix();
        assertNotNull(A);

        PrintPrecision.set(8);
        System.out.println("A =" + Matrix.toString(A));
    }


    @Test
    void fitRealContourTest() {
        String RES_PATH = "test-contours/";     // these are long contours!
        runFitTest(RES_PATH + "single-marker-5-0-contour.json", RES_PATH + "single-marker-5-0-corners.json");
        // runSegmentTest(RES_PATH + "single-marker-5-1-contour.json", RES_PATH + "single-marker-5-1-corners.json");
        // runSegmentTest(RES_PATH + "single-marker-5-2-contour.json", RES_PATH + "single-marker-5-2-corners.json");
        // runSegmentTest(RES_PATH + "single-marker-5-3-contour.json", RES_PATH + "single-marker-5-3-corners.json");
    }

    void runFitTest(String contourPath, String cornersPath) {
        double[][] contour = (double[][]) loadObject(this.getClass(), contourPath, double[][].class);
        //double[][] corners = (double[][]) loadObject(this.getClass(), cornersPath, double[][].class);
        SegmentedContour segCtr = new ContourSegmenter().segment(makePolygon(contour), contour.length * polygonalApproxAccuracyRate);
        assertEquals(4, segCtr.getSegmentCount());

        QuadHomographyFit fit = new QuadHomographyFit(segCtr);
        double[][] A = fit.getTransformationMatrix();
        assertNotNull(A);
        PrintPrecision.set(8);
        System.out.println("A quadFit = \n" + Matrix.toString(A));
        System.out.println("A quadFit error = " + fit.getError());

        // -------------------------------------------

        ProjectiveFit2d fitP = new ProjectiveFit2d(segCtr.getCorners().toArray(new Pnt2d[0]), UnitSquare.toArray(new Pnt2d[0]));
        System.out.println("A projective = \n" + Matrix.toString(fitP.getTransformationMatrix()));
        System.out.println("A projective error = " + fitP.getError());

    }

    /*
    A  quadFit =
    {{-0.00515487, -0.00027217, 1.46992790},
    {-0.00015717, 0.00509512, -0.12120253},
    {0.00001732, 0.00002034, 1.00000000}}
    A projective error = 0.04938862118838941

    A projective =
    {{-0.00523378, -0.00023790, 1.48353823},
    {-0.00013493, 0.00518130, -0.12775133},
    {0.00004991, 0.00005863, 1.00000000}}
    */




    @Test
    void getErrorTest() {
    }
}