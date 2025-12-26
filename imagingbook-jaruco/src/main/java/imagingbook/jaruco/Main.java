package imagingbook.jaruco;

import ij.ImagePlus;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.PolyLine2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.common.util.Timing.timeNanos;

public class Main {


    // public static List<PolyLine2d> parabCurves = null;

    // ------------------------------------------------------------------------

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2705_singleA.jpg";
    // "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    private static final Font MarkerFont = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final Font CornerFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Color MarkerColor = Color.magenta;
    private static final Color CornerColor = Color.blue;

    static void doBigImageTest() {
        // String IMG_PATH = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
        String IMG_PATH = SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg";

        ImagePlus im = IjUtils.openImage(IMG_PATH);
        im.show();
        ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();

        ArucoDetector detector = new ArucoDetector(dict);
        List<ArucoDetector.DetectionResult> detectionResultObsoletes = detector.detectMarkers(im.getProcessor());

        System.out.println("Markers found: " + detectionResultObsoletes.size());
        for (ArucoDetector.DetectionResult res : detectionResultObsoletes) {
            System.out.println(res);
        }
    }

    static void doSmallImageTest() {
        String[] paths = {
                // SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg",
                // SAMPLE_IMAGE_DIR + "single-marker-5-1.jpg",
                // SAMPLE_IMAGE_DIR + "single-marker-5-2.jpg",
                // SAMPLE_IMAGE_DIR + "single-marker-5-3.jpg",
                // SAMPLE_IMAGE_DIR + "single-marker-5-0-distA.png",
                // SAMPLE_IMAGE_DIR + "single-marker-5-0-distB.png",
                SAMPLE_IMAGE_DIR + "all-markers-small.jpg",
        };

        ImagePlus[] images = new ImagePlus[paths.length];
        for (int i = 0; i < paths.length; i++) {
            images[i] = IjUtils.openImage(paths[i]);
            images[i].show();
        }

        ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();
        ArucoDetector detector = new ArucoDetector(dict);

        ColoredStroke cornerStroke = new ColoredStroke(1.0, Color.blue);
        ColoredStroke cornerStroke0 = new ColoredStroke(1.0 * 3, Color.red);

        // process each image -------------------------------------------------------------

        for (int i = 0; i < paths.length; i++) {
            System.out.println("***** Processing image + " + i);
            ImagePlus im = images[i];
            ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
            // parabCurves = null;

            // ------------------------------------------------------------
            List<ArucoDetector.DetectionResult> detectedMarkers = new ArrayList<>();
            long elapsed = timeNanos(() ->
                {detectedMarkers.addAll(detector.detectMarkers(im.getProcessor()));}
            );
            System.out.println("Elapsed time (ms): " + elapsed/1000000);
            System.out.println("Markers found: " + detectedMarkers.size());
            // ------------------------------------------------------------

            // process all detected markers
            for (ArucoDetector.DetectionResult marker : detectedMarkers) {
                Polygon2d corners = marker.corners();

                ola.setFont(CornerFont);
                ola.setTextColor(CornerColor);
                double rad = 2;
                int j = 0;
                for (Pnt2d p : corners) {
                    double x = p.getX() - rad;
                    double y = p.getY() - rad;
                    ola.addShape(new Ellipse2D.Double(x, y, 2 * rad, 2 * rad),
                            j == 0 ? cornerStroke0 : cornerStroke); // mark corner 0 red
                    ola.addText(x + 5, y + 5, "" + j);
                    j++;
                }

                // draw the marker's id number
                Pnt2d center = corners.getCentroid();
                ola.setFont(MarkerFont);
                ola.setTextColor(MarkerColor);
                // ola.addText(center.getX(), center.getY(), res.markerId() + "/" + res.rotation() + "/" + res.hammingDist());
                ola.addText(center.getX(), center.getY(),
                        marker.lookup().markerIndex() + "/" + marker.lookup().rotation() + "/" + marker.lookup().hammingDistance());

                // if parabolas exist, draw them:
                // if (parabCurves != null) {
                //     for (PolyLine2d par : parabCurves) {
                //        ola.addShape(par.getShape());
                //     }
                // }
            }
            im.setOverlay(ola.getOverlay());
            // im.setTitle(im.getTitle() + " rot=" + res.rotation);
            im.updateAndDraw();
        }
    }

    public static void main(String[] args) {
        doSmallImageTest();
        // doBigImageTest();
    }
}
