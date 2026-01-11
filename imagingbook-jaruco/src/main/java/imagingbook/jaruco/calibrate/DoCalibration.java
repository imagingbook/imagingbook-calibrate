package imagingbook.jaruco.calibrate;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import imagingbook.calibration.Calibration;
import imagingbook.calibration.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.boards.AbstractBoard;
import imagingbook.jaruco.boards.AbstractBoardDetector.PntPair;
import imagingbook.jaruco.boards.CharucoBoard;
import imagingbook.jaruco.data.DICT_5x5_CharucoBoard_12x8_A4L.ImageCornerSet;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class DoCalibration {

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";
    static String[] paths = {
            // "DSC_2691.jpg",
            // "DSC_2692.jpg",
            // "DSC_2693.jpg",
            // "DSC_2694.jpg",    // fails with ParabolicFit!!
            // "DSC_2696.jpg",
            // "DSC_2698.jpg",
            // "DSC_2699.jpg",
            // "DSC_2700.jpg",
            // "DSC_2701g.jpg",    // problems with marker detection!!
            // "DSC_2702.jpg",
            // "DSC_2704.jpg",
            // "DSC_2705.jpg",
            // "DSC_2706.jpg",
            // "DSC_2707.jpg",
            // "DSC_2708.jpg",
            // "DSC_2709g.jpg",
            // "DSC_2710g.jpg",
            // "DSC_2711g.jpg",
            // "DSC_2712g.jpg",
            // "DSC_2713g.jpg",
            // "DSC_2715g.jpg"
    };
    static int USE_ONLY_POINTS = 64;

    public static void main(String[] args) {
        ImageCornerSet[] cornerSets = {
                ImageCornerSet.DSC_2691,
                ImageCornerSet.DSC_2692,
                ImageCornerSet.DSC_2693,
                ImageCornerSet.DSC_2694
        };

        int[] SELECTED = {}; //{ 0, 21, 171, 190};

        CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        // Pnt2d[] modelPoints = Arrays.copyOfRange(getModelPoints(board), 0, USE_ONLY_POINTS);
        Pnt2d[] modelPoints = getModelPoints(board);
        System.out.println(" modelPoints = " + modelPoints.length);
        // showBoard(board, modelPoints);

        // set up calibration
        Calibration.Parameters params = new Calibration.Parameters();
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = true;
        Calibration calibration = new Calibration(params, 6048, 4024);

        for (ImageCornerSet cornerSet : cornerSets) {
            System.out.println("adding corner set " + cornerSet);
            // Pnt2d[] obsPoints = getObservedPoints(matches);
            Pnt2d[] obsPoints = cornerSet.getInstance().getCornerPoints();
            // showObserved(bp, obsPoints, path);

            calibration.addView(modelPoints, obsPoints);
        }
        // if (true) return;


        calibration.calibrate();
        Camera camFinal = calibration.getFinalCamera();
        if (camFinal == null) {
            System.out.println("Calibration failed");
        }
        else {
            System.out.println("Calibration complete, camera = " + camFinal);
        }


    }

    // --------------------------------------------------------------------------------------------

    static Pnt2d[] getModelPoints(AbstractBoard board) {
        List<Pnt2d> modelPoints = new ArrayList<>();
        for (int i = 0; i < board.getMarkerCount(); i++) {
            Polygon2d corners = board.getMarkerCorners(i);
            for (int j = 0; j < 4; j++) {
                modelPoints.add(corners.getPnt(j));
            }
        }
        return modelPoints.toArray(new Pnt2d[0]);
    }

    static Pnt2d[] getObservedPoints(List<PntPair> matches) {
        List<Pnt2d> observedPoints = new ArrayList<>();
        for (PntPair p : matches) {
            observedPoints.add(p.imagePnt());
        }

        return observedPoints.toArray(new Pnt2d[0]);
    }

    static Pnt2d[] selectPoints(Pnt2d[] points, int ... indexes) {
        if (indexes.length > 0) {
            List<Pnt2d> selPoints = new ArrayList<>();
            for (int i : indexes) {
                if (i < points.length) {
                    selPoints.add(points[i]);
                }
            }
            return selPoints.toArray(new Pnt2d[0]);
        }
        else
            return points;
    }

    static void showBoard(AbstractBoard board, Pnt2d[] modelPoints) {
        ByteProcessor boardIp = board.createImage(1200);
        double scale = (double) boardIp.getWidth() / board.getBoardWidth();
        ImagePlus boardIm = new ImagePlus(board.getName(), boardIp);
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        ola.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (boardIp.getWidth() * 0.01)));
        ola.setTextColor(Color.red);
        for (int i = 0; i < modelPoints.length; i++) {
            double x = modelPoints[i].getX() * scale;
            double y = modelPoints[i].getY() * scale;
            ola.addText(x, y, ""+ i);
        }
        boardIm.setOverlay(ola.getOverlay());
        boardIm.show();
    }

    static void showObserved(ByteProcessor ip, Pnt2d[] obsPoints, String title) {
        ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
        ola.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (ip.getWidth() * 0.01)));
        ola.setTextColor(Color.red);
        double scale = 1;
        for (int i = 0; i < obsPoints.length; i++) {
            double x = obsPoints[i].getX() * scale;
            double y = obsPoints[i].getY() * scale;
            // System.out.println(i + " -> " + obsPoints[i]);
            ola.addText(x, y, ""+ i);
        }
        ImagePlus im = new ImagePlus(title, ip);
        im.setOverlay(ola.getOverlay());
        im.show();

    }

}
