package imagingbook.jaruco;

import ij.ImagePlus;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.util.Polygons;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.util.List;

import static imagingbook.jaruco.util.Polygons.getPolygonPath;

public class Main {

    // ------------------------------------------------------------------------

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2705_singleA.jpg";
    // "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    private static final Font MarkerFont = new Font(Font.SANS_SERIF, Font.BOLD, 32);
    private static final Color MarkerColor = Color.magenta;

    static void doBigImageTest() {
        String IMG_PATH = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
        // String IMG_PATH = SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg";

        ImagePlus im = IjUtils.openImage(IMG_PATH);
        im.show();
        ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();

        ArucoDetector detector = new ArucoDetector(dict);
        List<ArucoDetector.MarkerDetectionResult> markerDetectionResultObsoletes = detector.detectMarkers2(im.getProcessor());

        System.out.println("Markers found: " + markerDetectionResultObsoletes.size());
        for (ArucoDetector.MarkerDetectionResult res : markerDetectionResultObsoletes) {
            System.out.println(res);
        }
    }

    static void doSmallImageTest() {
        String[] paths = {
                SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-1.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-2.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-3.jpg",
                // SAMPLE_IMAGE_DIR + "all-markers-small.jpg",
        };

        ImagePlus[] images = new ImagePlus[paths.length];
        for (int i = 0; i < paths.length; i++) {
            images[i] = IjUtils.openImage(paths[i]);
            images[i].show();
        }

        ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();
        ArucoDetector detector = new ArucoDetector(dict);

        ColoredStroke stroke = new ColoredStroke(1.0, Color.blue);
        ColoredStroke stroke0 = new ColoredStroke(1.0 * 3, Color.red);

        for (int i = 0; i < paths.length; i++) {
            System.out.println("***** Processing image + " + i);
            ImagePlus im = images[i];

            List<ArucoDetector.MarkerDetectionResult> markerDetectionResultObsoletes = detector.detectMarkers2(im.getProcessor());
            System.out.println("Markers found: " + markerDetectionResultObsoletes.size());

            ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

            for (ArucoDetector.MarkerDetectionResult res : markerDetectionResultObsoletes) {
                // ArucoDetector.MarkerDetectionResult_obsolete res = markerDetectionResultObsoletes.get(0);
                System.out.println(res);
                // create shape overlay

                List<Pnt2d> corners = res.corners();
                // Collections.rotate(corners, res.rotation);
                //List<Pnt2d> corners = rotateCorners(corners, res.rotation);

                ola.addShape(getPolygonPath(corners, 0, 0), stroke);

                double rad = 2;
                int j = 0;
                for (Pnt2d p : corners) {
                    double x = p.getX() - rad;
                    double y = p.getY() - rad;
                    ola.addShape(new Ellipse2D.Double(x, y, 2 * rad, 2 * rad), j == 0 ? stroke0 : stroke);
                    j++;
                }

                // draw the marker's id number
                Pnt2d center = Polygons.getCentroid(corners);
                ola.setFont(MarkerFont);
                ola.setTextColor(MarkerColor);
                ola.addText(center.getX(), center.getY(), Integer.toString(res.markerId()));
            }

            im.setOverlay(ola.getOverlay());
            // im.setTitle(im.getTitle() + " rot=" + res.rotation);
            im.updateAndDraw();
        }
    }


    public static void main(String[] args) {
        // doSmallImageTest();
        doBigImageTest();
    }
}
