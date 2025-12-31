package imagingbook.jaruco.boards;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.ij.IjUtils;
import imagingbook.jaruco.marker.ArucoMarkerDetector;
import imagingbook.jaruco.marker.ArucoMarkerDetector.DetectionResult;
import imagingbook.jaruco.dict.ArucoDictionary;

import java.util.ArrayList;
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
public class GridBoardDetector {

    /**
     * A pair of corresponding points.
     * @param imagePnt point located in the image
     * @param modelPnt corresponding point on the board
     */
    public record PntPair(Pnt2d imagePnt, Pnt2d modelPnt) { }


    private final GridBoard board;
    // private final  ArucoDictionary dictionary;
    // private final ArucoMarkerDetector markerDetector;
    private final ByteProcessor ip;
    private final List<DetectionResult> detResults;

    public GridBoardDetector(GridBoard board, ByteProcessor ip) {
        this.board = board;
        this.ip = ip;

        // detect markers immediately (parameters needed):
        ArucoMarkerDetector markerDetector = new ArucoMarkerDetector(board.getDictionary());
        this.detResults = markerDetector.detectMarkers(ip);
    }

    // ----------------------------------------------------------------------------------

    public int getDetectedMarkerCount() {
        return detResults.size();
    }

    public List<Integer> getDetectedMarkerIds() {
        List<Integer> ids = new ArrayList<>();
        for (DetectionResult detectionResult : detResults) {
            ids.add(detectionResult.lookup().markerId());
        }
        return ids;
    }

    public List<PntPair> getAllMarkerPointMatches() {
        List<PntPair> matches = new ArrayList<>();
        for (DetectionResult detResult : detResults) {
            int markerId = detResult.lookup().markerId();
            Polygon2d imageCorners = detResult.corners();
            Polygon2d boardCorners = board.getMarkerCorners(markerId);
            for (int i = 0; i < 4; i++) {
                matches.add(new PntPair(imageCorners.getPnt(i), boardCorners.getPnt(i)));
            }
        }
        return matches;
    }

    // ----------------------------------------------------------------------------------

    public boolean checkBoard() {
        // check if all marker corners are in CCW order:
        for (DetectionResult detResult : detResults) {
            Polygon2d imageCorners = detResult.corners();
            if (imageCorners.getSignedArea() <= 0) {
                throw new RuntimeException("negative area for id = " + detResult.lookup().markerId());
            }
        }

        return true;
    }

    // ----------------------------------------------------------------------------------

    static String SAMPLE_IMAGE_DIR = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-jaruco/src/main/resources/imagingbook/jaruco/sample-images/";

    public static void main(String[] args) {
        // String path = SAMPLE_IMAGE_DIR + "single-marker-5-0.jpg";
        String path = SAMPLE_IMAGE_DIR + "all-markers-small.jpg";
        ImagePlus im = IjUtils.openImage(path);
        im.show();

        ByteProcessor bp = im.getProcessor().convertToByteProcessor();

        GridBoard board = GridBoardPredefined.DICT_5X5_GridBoard_18x12_A3L.getInstance();
        ImageProcessor ip = board.createImage(1200);
        new ImagePlus("Board " + board.getName(), ip).show();

        GridBoardDetector gbd = new GridBoardDetector(board, bp);
        System.out.println("markers detected: " + gbd.getDetectedMarkerCount());

        gbd.checkBoard();

        List<PntPair> matches =  gbd.getAllMarkerPointMatches();
        // System.out.println("marker point matches: " + matches.size());
        // for (PntPair pntPair : matches) {
        //     System.out.println(pntPair);
        // }

        // List<Integer> ids = gbd.getDetectedMarkerIds();
        // for (Integer id : ids) {
        //     System.out.println("   id: " + id);
        // }

    }


}
