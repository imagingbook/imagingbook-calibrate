package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.ij.IjUtils;
import imagingbook.jaruco.marker.ArucoMarkerDetector;

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
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

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

    }


}
