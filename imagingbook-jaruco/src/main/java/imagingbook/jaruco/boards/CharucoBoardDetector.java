package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.process.ByteProcessor;
import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial3TermDistortion;
import imagingbook.calibrate.hugin.StraightnessDistortionEstimator1;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.math.PrintPrecision;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectedMarker;

import java.util.List;

/**
 * Performs detection of the specified {@link GridBoard} in an image
 * and holds the result for evaluation.
 * TODO: hold the result in another object?
 * Typical usage:
 * <pre>
 *     GridBoard board = new GridBoard();
 *     GridBoardDetector detector = new GridBoardDetector(board);
 *     ByteProcessor ip = ... // input image
 *     Result result = detector.detectBoard(ip);
 *
 *     List&lt;PntMatch&gt; allMatches = result.getAllPointMatches();
 *     List&lt;LineMatch&gt; lineMatches = result.getAllLineMatches();
 *     List&lt;List&lt;PntMatch&gt;&gt; markerMatches = .....
 *     List&lt;List&lt;PntMatch&gt;&gt; checkerMatches = .....
 *     List&lt;PntMatchGroup&gt; anyGroup = ....
 * </pre>
 * {@code PntMatch} is a pair of {@code (imagePnt, modelPnt)},
 * a {@code PntMatchGroup} is simply a set of {@code PntMatch} instances.
 * Is it important to group point matches? Certainly for straight lines.
 * how about for marker boxes?
 * All this should be as generic as possible.
 */
public class CharucoBoardDetector extends AbstractBoardDetector {

    public CharucoBoardDetector(CharucoBoard board, ByteProcessor ip) {
        super(board, ip);
    }

    // ----------------------------------------------------------------------------------

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    public static void main(String[] args) {
        // String path = SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg";
        // String path = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
        String path = SAMPLE_IMAGE_DIR + "DSC_2691g.jpg";
        ImagePlus im = IjUtils.openImage(path);
        im.show();

        ByteProcessor bp = im.getProcessor().convertToByteProcessor();

        CharucoBoard board = CharucoBoard.Predefined.DICT_5x5_CharucoBoard_12x8_A4L.getInstance();
        // new ImagePlus("Board " + board.getName(), board.createImage(1200)).show();

        CharucoBoardDetector gbd = new CharucoBoardDetector(board, bp);
        System.out.println("markers detected: " + gbd.getDetectedMarkerCount());

        List<PntPair> matches =  gbd.getAllMarkerPointMatches();
        // System.out.println("marker point matches: " + matches.size());
        // for (PntPair pntPair : matches) {
        //     System.out.println(pntPair);
        // }

        List<Integer> ids = gbd.getDetectedMarkerIds();
        for (Integer id : ids) {
            System.out.println("   id: " + id);
        }
        System.out.println("all board markers found: " + gbd.allBoardMarkersFound());

        // collect collinear corner points -----------------------------------

        List<DetectedMarker> detectedMarkers = gbd.getDetectedMarkers();
        CollinearPointsExtractor cbe = new CollinearPointsExtractor(board);
        List<List<Pnt2d>> collinearPointSets = cbe.getCollinearPointSets(detectedMarkers);
        Overlay oly = CollinearPointsExtractor.getOverlay(collinearPointSets);
        im.setOverlay(oly);
        im.updateAndDraw();

        // try plumb line calibration --------------------------------------------------

        int w = im.getWidth();
        int h = im.getHeight();
        double f = w * (55.0 / 36.0);
        System.out.println("f = " + f + " pixels");
        double[] A = {520, 520, 0, 0.5 * w, 0.5 * h};
        DistortionModel initDist = new Radial3TermDistortion(new double[] {0, 0, 0});
        Camera initCam = new StandardCamera(A, initDist);
        StraightnessDistortionEstimator1 estimator = new StraightnessDistortionEstimator1(initCam, collinearPointSets);
        Camera newCam = estimator.estimateDistortion();
        PrintPrecision.set(6);
        System.out.println("result = " + newCam.getDistortion());

    }


}
