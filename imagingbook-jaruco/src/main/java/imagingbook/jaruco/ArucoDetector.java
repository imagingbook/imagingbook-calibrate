package imagingbook.jaruco;


import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.ij.IjUtils;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.common.util.ParameterBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static imagingbook.jaruco.ArucoDictionary.toBitSet;
import static imagingbook.jaruco.ByteArrayUtils.toString01;
import static imagingbook.jaruco.ContourSimplifierClosed.getCircularity;
import imagingbook.jaruco.ArucoDictionary.LookupResult;
import imagingbook.jaruco.gui.ZoomableImagePlus;

public class ArucoDetector {

    public enum CornerRefineMethod {
        /** Tag and corners detection based on the ArUco approach */
        CORNER_REFINE_NONE,
        /** ArUco approach and refine the corners locations using corner subpixel accuracy */
        CORNER_REFINE_SUBPIX,
        /** ArUco approach and refine the corners locations using the contour-points line fitting */
        CORNER_REFINE_CONTOUR,
        /** Tag and corners detection based on the AprilTag 2 approach @cite wang2016iros */
        CORNER_REFINE_APRILTAG
    }

    public static class DetectorParameters implements ParameterBundle<ArucoDetector> {
        public int adaptiveThreshWinSizeMin = 3;
        public int adaptiveThreshWinSizeMax = 23;
        public int adaptiveThreshWinSizeStep = 10;
        public int adaptiveThreshConstant = 7;
        public double minMarkerPerimeterRate = 0.03;
        public double maxMarkerPerimeterRate = 4.;
        public double polygonalApproxAccuracyRate = 0.03;
        public double minCornerDistanceRate = 0.05;
        public int minDistanceToBorder = 3;
        public double minMarkerDistanceRate = 0.125;
        public CornerRefineMethod cornerRefinementMethod = CornerRefineMethod.CORNER_REFINE_NONE;
        public int cornerRefinementWinSize = 5;
        public double relativeCornerRefinmentWinSize = 0.3;
        public int cornerRefinementMaxIterations = 30;
        public double cornerRefinementMinAccuracy = 0.1;
        public int markerBorderBits = 1;
        public int perspectiveRemovePixelPerCell = 4;
        public double perspectiveRemoveIgnoredMarginPerCell = 0.13;
        public double maxErroneousBitsInBorderRate = 0.35;
        public double minOtsuStdDev = 5.0;
        public double errorCorrectionRate = 0.6;
        public double aprilTagQuadDecimate = 0.0;
        public double aprilTagQuadSigma = 0.0;
        public int aprilTagMinClusterPixels = 5;
        public int aprilTagMaxNmaxima = 10;
        public double aprilTagCriticalRad = 10 * Math.PI / 180;
        public double aprilTagMaxLineFitMse = 10.0;
        public int aprilTagMinWhiteBlackDiff = 5;
        public int aprilTagDeglitch = 0;
        public boolean detectInvertedMarker = false;
        public boolean useAruco3Detection = false;
        public int minSideLengthCanonicalImg = 32;
        public double minMarkerLengthRatioOriginalImg = 0.0;
    }

    public static class RefineParameters implements ParameterBundle<ArucoDetector> {
        public double minRepDistance = 10;
        public double errorCorrectionRate = 3;
        public boolean checkAllOrders = true;
    }

    /**
     * Represents the detection result for a single marker.
     */
    public static class MarkerDetection {
        final int markerId;
        final int rotation;
        final int hammingDist;
        final Pnt2d[] corners;
        final Pnt2d[] rejectedPoints;

        // Constructor (private).
        private MarkerDetection(int markerId, int rotation, int hDist, Pnt2d[] corners, Pnt2d[] rejectedPoints) {
            this.markerId = markerId;
            this.rotation = rotation;
            this.hammingDist = hDist;
            this.corners = corners;
            this.rejectedPoints = rejectedPoints;
        }

        /**
         * Builds a {@link MarkerDetection} from a given {@link LookupResult}
         * instance, adding the associated corner positions and rejected points.
         *
         * @param lookupR a {@link LookupResult} instance
         * @param corners corner positions for the detected marker
         * @param rejectedPoints
         * @return a new {@link MarkerDetection} instance
         */
        static MarkerDetection from(LookupResult lookupR, Pnt2d[] corners, Pnt2d[] rejectedPoints) {
            return new MarkerDetection(lookupR.markerIndex, lookupR.rotation, lookupR.hammingDistance,
                    corners, rejectedPoints);
        }

        @Override
        public String toString() {
            return String.format("%s [id=%d, corners=%s]",
                    getClass().getSimpleName(), markerId, Arrays.toString(corners));
        }
    }

    // -------------------------------------------------------------------------

    private final class DetectionContext {

    }

    private DetectionContext detContext = null;

    // -------------------------------------------------------------------------

    private final ArucoDictionary dictionary;
    private final DetectorParameters detectorParams;
    private final RefineParameters refineParams;

    /**
     * Basic constructor, using default parameter settings specified by
     * {@link DetectorParameters} and {@link RefineParameters}.
     *
     * @param dictionary a {@link ArucoDictionary} instance
     */
    public ArucoDetector(ArucoDictionary dictionary) {
        this(dictionary, new DetectorParameters(), new RefineParameters());
    }

    /**
     * Full constructor.
     * Default parameters are used if null is passed for any of the parameter
     * bundles.
     *
     * @param dictionary a {@link ArucoDictionary} instance
     * @param detectorParams a {@link DetectorParameters} parameter bundle (may be null)
     * @param refineParams a {@link RefineParameters} parameter bundle (may be null)
     */
    public ArucoDetector(ArucoDictionary dictionary,
                         DetectorParameters detectorParams,
                         RefineParameters refineParams) {
        this.dictionary = dictionary;
        this.detectorParams = (detectorParams != null) ?
                detectorParams : new DetectorParameters();
        this.refineParams = (refineParams != null) ?
                refineParams : new RefineParameters();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    int CURRENT_THRESHOLD = -1; // TODO: remove from here!

    /**
     * The core method. Tries to locate and identify markers in the given image.
     * @param ip the input image
     * @return
     */
    public List<MarkerDetection> detectMarkers(ImageProcessor ip) {
        // STEP 1: convert input image to grayscale:
        ByteProcessor gray = ip.convertToByteProcessor();

        // STEP 2a: threshold image for region/contour extraction:
        CURRENT_THRESHOLD = Math.round(new OtsuThresholder().getThreshold(gray));
        gray.threshold(CURRENT_THRESHOLD);


        // STEP 2b: segment and find closed contours:
        ContourTracer ct = new RegionContourSegmentation(gray);
        // since white is considered foreground, outer contours
        // of black regions are actually INNER contours:
        List<? extends Contour> ics = ct.getInnerContours();

        // STEP 3: Simplify inner contours to polygons
        List<List<Pnt2d>> candidateBoxes = simplifyContours(ics);

        // STEP 4: Process each candidate box and collect the results
        List<MarkerDetection> markerDetections = new ArrayList<>();
        int k = 0;
        for (List<Pnt2d> candidateBox : candidateBoxes) {
            MarkerDetection dr = processOneCandidateBox(ip, candidateBox, k++);
            if (dr != null) {
                markerDetections.add(dr);
            }
        }
        return markerDetections;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------


    List<List<Pnt2d>> simplifyContours(List<? extends Contour> icsCln) {
        List<List<Pnt2d>> candidateBoxes = new ArrayList<>();
        for (Contour ic : icsCln) {
            // keep only contours with more than 50 points (TODO: parameter?)
            if (ic.getLength() < 50)
                continue;
            double tol = ic.getLength() * detectorParams.polygonalApproxAccuracyRate;
            List<Pnt2d> iscln = ContourSimplifierClosed.simplify(ic, tol);   // simplified polygon
            // iscln = ContourSimplifier.cleanupCollinear(is, tol, true);   // optional cleanup, not needed
            // check if this is a convex 4-corner polygon that is not too elongated:
            if (iscln.size() == 4 &&
                    ContourSimplifier.isConvex(iscln) &&
                    getCircularity(iscln) > 0.5) {
                // add to candidate marker boxes
                candidateBoxes.add(iscln);
            }
        }
        return candidateBoxes;
    }

    MarkerDetection processOneCandidateBox(ImageProcessor ip, List<Pnt2d> candidateBox, int k) {
        // System.out.println("processOneCandidateBox " + k);
        // STEP 4a - CORNER REFINEMENT should come here!

        // STEP 4b - extract a small rectified subimage
        int targetSize = 5 * (this.dictionary.getMarkerSize() + 2); // fields with 5x5 pixels (parameter!?)
        ByteProcessor markerIp = extractMarkerImage(ip, candidateBox, targetSize);
        new ZoomableImagePlus("Marker raw" + k, markerIp.duplicate()).show(20);

        // new OtsuThresholder().threshold(markerIp);
        // new ZoomableImagePlus("Marker b&w" + k, markerIp.duplicate()).show(20);

        // STEP 4c - sample marker fields to generate the 1D marker pattern
        BitSet sampleBits = extractMarkerBits(markerIp);

        // STEP 4d - Lookup marker pattern in dictionary
        double maxCorrectionRate = 1.0; // TODO: CHECK!!!
        LookupResult lookup = dictionary.lookup(sampleBits, maxCorrectionRate);

        if (lookup != null) {
            // System.out.println("DETECTED: " + result);
            int id = lookup.markerIndex;
            Pnt2d[] corners = candidateBox.toArray(new Pnt2d[4]);
            return MarkerDetection.from(lookup, corners, null);   // TODO: rejectedPoints?
        }
        else {
            return null;
        }
    }

    //static int MARKER_SIZE = 64;
    ByteProcessor extractMarkerImage(ImageProcessor origIp, List<Pnt2d> corners, int targetSize) {
        Pnt2d[] sourcePts = corners.toArray(new Pnt2d[0]);
        Pnt2d[] targetPts = {   // enlarge target square by 1/2 pixel
                Pnt2d.from(-0.5, -0.5),
                Pnt2d.from(-0.5, targetSize - 1 + 0.5),
                Pnt2d.from(targetSize - 1 + 0.5, targetSize - 1 + 0.5),
                Pnt2d.from(targetSize - 1 + 0.5, -0.5)};
        // calculate homography mapping (from target to source):
        ProjectiveMapping2D hom = ProjectiveMapping2D.fromPoints(targetPts, sourcePts);
        ByteProcessor targetIp = new ByteProcessor(targetSize, targetSize);
        // IJ.log("map = " + map.toString());
        new ImageMapper(hom).map(origIp, targetIp);
        return targetIp;
    }


    private BitSet extractMarkerBits(ByteProcessor markerIp) {
        // optionally wrap markerIp into an ImageAccessor to handle image borders (not strictly needed)
        // ScalarAccessor ia = ScalarAccessor.create(markerIp,
        //         OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        int w = markerIp.getWidth();
        int N = dictionary.getMarkerSize();
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitSet bits = new BitSet(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int y = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int x = (int) Math.round((1.5 + j) * d);
                // int g = markerIp.getPixel(x, y);    // todo: get 3x3 median value at x/y

                int g = get3x3Median(markerIp, x, y); // use threshold from initial thresholding?
                // System.out.printf("x=%d y=%d g=%d\n", x, y, g);
                if (g >= CURRENT_THRESHOLD) {
                    bits.set(k);
                }
                k++;
            }
        }
        // System.out.println("Extracted pattern = " + toString01(bits, 25));

        // return toBitSet("1011010000010000001010111");
        return bits;
    }

    int get3x3Median(ByteProcessor ip, int x, int y) {
        // ScalarAccessor ia = ScalarAccessor.create(ip, OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        // collect 3x3 values
        int[] vals = new int[9];
        int k = 0;
        for (int v = 0; v < 3; v++) {
            for (int u = 0; u < 3; u++) {
                vals[k] = ip.getPixel(x -1 + u, y - 1 + v);
                k++;
            }
        }
        Arrays.sort(vals);  // calculate median
        return vals[4];
    }


    // ------------------------------------------------------------------------

    // static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2705_singleA.jpg";
    static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2702_small.jpg";

    public static void main(String[] args) {
        ImagePlus im = IjUtils.openImage(IMG_PATH);
        im.show();
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();

        ArucoDetector detector = new ArucoDetector(dict);
        List<MarkerDetection> markerDetections = detector.detectMarkers(im.getProcessor());

        System.out.println("Markers found: " + markerDetections.size());
        for (MarkerDetection res : markerDetections) {
            System.out.println(res);
        }
    }

}