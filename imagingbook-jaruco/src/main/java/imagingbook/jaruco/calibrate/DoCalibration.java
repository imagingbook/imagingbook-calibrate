package imagingbook.jaruco.calibrate;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import imagingbook.calibration.Calibrator;
import imagingbook.calibration.Camera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.jaruco.boards.AbstractBoard;
import imagingbook.jaruco.boards.AbstractBoardDetector;
import imagingbook.jaruco.boards.AbstractBoardDetector.PntPair;
import imagingbook.jaruco.boards.CharucoBoard;
import imagingbook.jaruco.boards.CharucoBoardDetector;

import java.util.ArrayList;
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

    public static void main(String[] args) {
        String[] paths = {
                "DSC_2691g.jpg",
                "DSC_2692g.jpg",
                "DSC_2693g.jpg"
        };

        CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        Pnt2d[] modelPoints = getModelPoints(board);

        // set up calibrator
        Calibrator.Parameters params = new Calibrator.Parameters();
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = true;
        Calibrator calibrator = new Calibrator(params, modelPoints);


        for (String path : paths) {
            System.out.println("loading image " + path);
            ImagePlus im = IjUtils.openImage(SAMPLE_IMAGE_DIR + path);
            // im.show();
            ByteProcessor bp = im.getProcessor().convertToByteProcessor();
            CharucoBoardDetector gbd = new CharucoBoardDetector(board, bp);
            System.out.println("   all board markers found: " + gbd.allBoardMarkersFound());

            List<PntPair> matches =  gbd.getAllMarkerPointMatches();
            Pnt2d[] obsPoints = getObservedPoints(matches);
            calibrator.addView(obsPoints);
        }

        Camera camFinal = calibrator.calibrate();
        if (camFinal == null) {
            System.out.println("Calibration failed");
        }
        else {
            System.out.println("Calibration complete, camera = " + camFinal);
        }

        // ByteProcessor bp = im.getProcessor().convertToByteProcessor();
        //
        // CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        // ImageProcessor ip = board.createImage(1200);
        // new ImagePlus("Board " + board.getName(), ip).show();
        //
        // CharucoBoardDetector gbd = new CharucoBoardDetector(board, bp);
        // System.out.println("markers detected: " + gbd.getDetectedMarkerCount());
        //
        // List<PntPair> matches =  gbd.getAllMarkerPointMatches();
        // // System.out.println("marker point matches: " + matches.size());
        // // for (PntPair pntPair : matches) {
        // //     System.out.println(pntPair);
        // // }
        //
        // List<Integer> ids = gbd.getDetectedMarkerIds();
        // for (Integer id : ids) {
        //     System.out.println("   id: " + id);
        // }
        // System.out.println("all board markers found: " + gbd.allBoardMarkersFound());

    }

}
