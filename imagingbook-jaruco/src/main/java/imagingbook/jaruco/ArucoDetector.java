package imagingbook.jaruco;


import ij.IJ;
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
    public static class DetectionResult {
        final int markerId;
        final int rotation;
        final int hammingDist;
        final Pnt2d[] corners;
        final Pnt2d[] rejectedPoints;

        public DetectionResult(int markerId, int rotation, int hDist, Pnt2d[] corners, Pnt2d[] rejectedPoints) {
            this.markerId = markerId;
            this.rotation = rotation;
            this.hammingDist = hDist;
            this.corners = corners;
            this.rejectedPoints = rejectedPoints;
        }

        /**
         * Factory method, builds a {@link DetectionResult} from a given
         * {@link LookupResult} instance, adding the associated corner positions
         * and rejected points.
         *
         * @param lookupR a {@link LookupResult} instance
         * @param corners corner positions for the detected marker
         * @param rejectedPoints
         * @return a new {@link DetectionResult} instance
         */
        static DetectionResult from(LookupResult lookupR, Pnt2d[] corners, Pnt2d[] rejectedPoints) {
            return new DetectionResult(lookupR.markerIndex, lookupR.rotation, lookupR.hammingDistance,
                    corners, rejectedPoints);
        }

        @Override
        public String toString() {
            return String.format("%s [id=%d, corners=%s]",
                    getClass().getSimpleName(), markerId, Arrays.toString(corners));
        }
    }

    // -------------------------------------------------------------------------

    private final ArucoDictionary dictionary;
    private final DetectorParameters detectorParams;
    private final RefineParameters refineParams;

    static int REDUCED_SIZE = 32;

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

    public List<DetectionResult> detectMarkers(ImagePlus im) {
        // STEP 1: convert input image to grayscale:
        ImageProcessor ip = im.getProcessor();
        ByteProcessor gray = ip.convertToByteProcessor();

        // STEP 2a: threshold image:
        new OtsuThresholder().threshold(gray);

        // STEP 2b: segment and find closed contours:
        ContourTracer ct = new RegionContourSegmentation(gray);
        // since white is considered foreground, outer contours
        // of black regions are actually INNER contours:
        List<? extends Contour> ics = ct.getInnerContours();

        // STEP 3: Simplify inner contours to polygons
        List<List<Pnt2d>> candidateBoxes = simplifyContours(ics);

        // STEP 4: Process each candidate box and collect the results
        List<DetectionResult> detectionResults = new ArrayList<>();
        int k = 0;
        for (List<Pnt2d> candidateBox : candidateBoxes) {
            DetectionResult dr = processOneCandidateBox(ip, candidateBox, k++);
            if (dr != null) {
                detectionResults.add(dr);
            }
        }
        return detectionResults;
    }

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

    DetectionResult processOneCandidateBox(ImageProcessor ip, List<Pnt2d> candidateBox, int k) {
        // STEP 4a - CORNER REFINEMENT should come here!

        // STEP 4b - extract a 64 x 64 rectified subimage
        ByteProcessor markerIp = extractMarkerImage(ip, candidateBox, REDUCED_SIZE); // parameter!
        new OtsuThresholder().threshold(markerIp);
        new ImagePlus("Marker " + k, markerIp).show();

        // STEP 4c - sample marker fields to generate the 1D marker pattern
        BitSet sampleBits = extractMarkerBits(markerIp);

        // STEP 4d - Lookup marker pattern in dictionary
        double maxCorrectionRate = 1.0; // TODO: CHECK!!!
        LookupResult lookup = dictionary.lookup(sampleBits, maxCorrectionRate);

        if (lookup != null) {
            // System.out.println("DETECTED: " + result);
            int id = lookup.markerIndex;
            Pnt2d[] corners = candidateBox.toArray(new Pnt2d[4]);
            return DetectionResult.from(lookup, corners, null);   // TODO: rejectedPoints?
        }
        else {
            return null;
        }
    }

    //static int MARKER_SIZE = 64;
    ByteProcessor extractMarkerImage(ImageProcessor origIp, List<Pnt2d> corners, int targetSize) {
        Pnt2d[] sourcePts = corners.toArray(new Pnt2d[0]);
        Pnt2d[] targetPts = {
                Pnt2d.from(0, 0),
                Pnt2d.from(0, targetSize - 1),
                Pnt2d.from(targetSize - 1, targetSize - 1),
                Pnt2d.from(targetSize - 1, 0)};
        // calculate homography mapping (from target to source):
        ProjectiveMapping2D hom = ProjectiveMapping2D.fromPoints(targetPts, sourcePts);
        ByteProcessor targetIp = new ByteProcessor(targetSize, targetSize);
        // IJ.log("map = " + map.toString());
        new ImageMapper(hom).map(origIp, targetIp);
        return targetIp;
    }

    // TODO next
    private BitSet extractMarkerBits(ByteProcessor markerIp) {
        int w = markerIp.getWidth();
        int N = dictionary.getMarkerSize();
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitSet bits = new BitSet(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int y = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int x = (int) Math.round((1.5 + j) * d);
                int g = markerIp.getPixel(x, y);    // todo: get 3x3 median value at x/y
                // System.out.printf("x=%d y=%d g=%d\n", x, y, g);
                if (g >= 128) {
                    bits.set(k);
                }
                k++;
            }
        }
        // System.out.println("Extracted pattern = " + toString01(bits));

        // return toBitSet("1011010000010000001010111");
        return bits;
    }


    // ------------------------------------------------------------------------

    static String IMG_PATH = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-images/DSC_2705_singleA.jpg";

    public static void main(String[] args) {
        ImagePlus im = IjUtils.openImage(IMG_PATH);
        im.show();
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();

        ArucoDetector detector = new ArucoDetector(dict);
        List<DetectionResult> detectionResults = detector.detectMarkers(im);
        for (DetectionResult res : detectionResults) {
            System.out.println(res);
        }
    }

}