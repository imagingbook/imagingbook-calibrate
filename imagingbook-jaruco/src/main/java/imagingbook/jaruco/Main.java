package imagingbook.jaruco;

import ij.ImagePlus;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    // ------------------------------------------------------------------------

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2705_singleA.jpg";
    // "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    static void doBigImageTest() {
        String IMG_PATH = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";

        ImagePlus im = IjUtils.openImage(IMG_PATH);
        im.show();
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();

        ArucoDetector detector = new ArucoDetector(dict);
        List<ArucoDetector.MarkerDetection> markerDetections = detector.detectMarkers(im.getProcessor());

        System.out.println("Markers found: " + markerDetections.size());
        for (ArucoDetector.MarkerDetection res : markerDetections) {
            System.out.println(res);
        }
    }

    static void doSmallImageTest() {
        String[] paths = {
                SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-1.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-2.jpg",
                SAMPLE_IMAGE_DIR + "single-marker-5-3.jpg",
        };

        ImagePlus[] images = new ImagePlus[paths.length];
        for (int i = 0; i < paths.length; i++) {
            images[i] = IjUtils.openImage(paths[i]);
            images[i].show();
        }

        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();
        ArucoDetector detector = new ArucoDetector(dict);

        ColoredStroke stroke = new ColoredStroke(1.0, Color.blue);
        ColoredStroke stroke0 = new ColoredStroke(1.0 * 3, Color.red);

        for (int i = 0; i < paths.length; i++) {
            System.out.println("***** Processing image + " + i);
            ImagePlus im = images[i];
            List<ArucoDetector.MarkerDetection> markerDetections = detector.detectMarkers(im.getProcessor());
            System.out.println("Markers found: " + markerDetections.size());

            ShapeOverlayAdapter ola = new ShapeOverlayAdapter();

            {
                ArucoDetector.MarkerDetection res = markerDetections.get(0);
                System.out.println(res);
                // create shape overlay

                List<Pnt2d> corners = res.corners.polygon;
                Collections.rotate(corners, res.rotation);
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

                im.setOverlay(ola.getOverlay());
                im.setTitle(im.getTitle() + " rot=" + res.rotation);
                im.updateAndDraw();
            }
        }
    }

    static List<Pnt2d> rotateCorners(List<Pnt2d> corners, int steps) {
        int n = corners.size();
        List<Pnt2d> corners2 = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            corners2.add(corners.get((steps + i) % n));
        }
        return corners2;
    }


    static Path2D getPolygonPath(List<Pnt2d> contour, double xOffset, double yOffset) {
        Path2D path = new Path2D.Float();
        Pnt2d[] pnts = contour.toArray(new Pnt2d[0]);
        if (pnts.length > 1) {
            path.moveTo(pnts[0].getX() + xOffset, pnts[0].getY() + yOffset);
            for (int i = 1; i < pnts.length; i++) {
                path.lineTo(pnts[i].getX() + xOffset,  pnts[i].getY() + yOffset);
            }
            path.closePath();
        }
        else {	// special case: mark a single pixel region "X"
            double x = pnts[0].getX();
            double y = pnts[0].getY();
            path.moveTo(x + xOffset - 0.5, y + yOffset - 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset + 0.5);
            path.moveTo(x + xOffset - 0.5, y + yOffset + 0.5);
            path.lineTo(x + xOffset + 0.5, y + yOffset - 0.5);
        }
        return path;
    }

    public static void main(String[] args) {
        // doBigImageTest();
        doSmallImageTest();
    }
}
