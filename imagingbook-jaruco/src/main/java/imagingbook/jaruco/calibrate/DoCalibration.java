package imagingbook.jaruco.calibrate;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import imagingbook.calibration.Calibrator;
import imagingbook.calibration.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.jaruco.boards.AbstractBoard;
import imagingbook.jaruco.boards.AbstractBoardDetector.PntPair;
import imagingbook.jaruco.boards.CharucoBoard;
import imagingbook.jaruco.boards.CharucoBoardDetector;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoCalibration {

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";


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

    static int USE_ONLY_POINTS = 64;


    public static void main(String[] args) {
        String[] paths = {
                "DSC_2691g.jpg",
                "DSC_2692g.jpg",
                // "DSC_2693g.jpg",
                // "DSC_2694g.jpg",    // fails with ParabolicFit!!
                // "DSC_2696g.jpg",
                // "DSC_2698g.jpg",
                // "DSC_2699g.jpg",
                // "DSC_2700g.jpg",
                                    // "DSC_2701g.jpg",    // problems with marker detection!!
                // "DSC_2702g.jpg",
                // "DSC_2704g.jpg",
                // "DSC_2705g.jpg",
                // "DSC_2706g.jpg",
                // "DSC_2707g.jpg",
                // "DSC_2708g.jpg",
                // "DSC_2709g.jpg",
                // "DSC_2710g.jpg",
                // "DSC_2711g.jpg",
                // "DSC_2712g.jpg",
                // "DSC_2713g.jpg",
                // "DSC_2715g.jpg"
        };

        int[] SELECTED = {}; //{ 0, 21, 171, 190};

        CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        // Pnt2d[] modelPoints = Arrays.copyOfRange(getModelPoints(board), 0, USE_ONLY_POINTS);
        Pnt2d[] modelPoints = getModelPoints(board);
        modelPoints = selectPoints(modelPoints, SELECTED);    // , 0, 21, 171, 190
        System.out.println(" modelPoints = " + modelPoints.length);
        // showBoard(board, modelPoints);

        // set up calibrator
        Calibrator.Parameters params = new Calibrator.Parameters();
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = true;
        Calibrator calibrator = new Calibrator(params, modelPoints);

        // if (true) return;

        for (String path : paths) {
            // System.out.println("loading image " + path);
            ImagePlus im = IjUtils.openImage(SAMPLE_IMAGE_DIR + path);
            // im.show();
            ByteProcessor bp = im.getProcessor().convertToByteProcessor();
            CharucoBoardDetector gbd = new CharucoBoardDetector(board, bp);
            // System.out.println("   all board markers found: " + gbd.allBoardMarkersFound());

            List<PntPair> matches =  gbd.getAllMarkerPointMatches();
            // Pnt2d[] obsPoints = Arrays.copyOfRange(getObservedPoints(matches),  0, USE_ONLY_POINTS);
            Pnt2d[] obsPoints = getObservedPoints(matches);
            obsPoints = selectPoints(obsPoints, SELECTED);    // , 0, 21, 171, 190
            System.out.println(path + ": obsPoints = " + obsPoints.length);

            // showObserved(bp, obsPoints, path);

            calibrator.addView(obsPoints);

            // check estimated homographies (why is bottom row always {0,0,1}?):
            // ProjectiveMapping2D checkmap = ProjectiveMapping2D.fromPoints(modelPoints, obsPoints);
            // System.out.println("checkmap " + path + ": \n" + checkmap);
            // List<Pnt2d> checkPoints = new ArrayList<>();
            // for (int i = 0; i < modelPoints.length; i++) {
            //     checkPoints.add(checkmap.applyTo(modelPoints[i]));
            // }
            // showObserved(bp, checkPoints.toArray(new Pnt2d[0]), path);


        }
        // if (true) return;


        Camera camFinal = calibrator.calibrate();
        if (camFinal == null) {
            System.out.println("Calibration failed");
        }
        else {
            System.out.println("Calibration complete, camera = " + camFinal);
        }


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
