package imagingbook.jaruco;


import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.common.util.ParameterBundle;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.ArucoDictionary.LookupResult;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArucoDetector {

    static int PYRAMID_LEVELS = 5;



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

    // -------------------------------------------------------------------------

    private static int minContourLength = 50;
    private static double minCircularity = 0.5;
    private static int markerImageSize = 64;
    private static double maxCorrectionRate = 1.0; // TODO: CHECK!!!


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

    /**
     * The core method. Tries to locate and identify markers in the given image.
     * @param ip the input image
     * @return a (possibly empty) list of {@link DetectionResult} instances
     */
    public List<DetectionResult> detectMarkers(ImageProcessor ip) {
        List<DetectionResult> markerDetectionResults = new ArrayList<>();

        // STEP 1: convert input image to grayscale:
        ByteProcessor gray = ip.convertToByteProcessor();

        // STEP 2: Threshold the input image and find candidate outlines
        int thr = Math.round(new OtsuThresholder().getThreshold(gray));
        gray.threshold(thr);

        // STEP 3: Find candidate contours
        ContourTracer ct = new RegionContourSegmentation(gray);
        // since white is considered foreground, outer contours
        // of black regions are actually INNER contours:
        List<? extends Contour> ics = ct.getInnerContours();             // inner corners run CCW?

        // process all contours
        for (Contour contour : ics) {
            List<Pnt2d> pts = contour.getPointList();
            if (pts.size() < minContourLength) {                          // parameter!
                continue;
            }
            // A. Segment contour and extract quad
            SegmentedContour poly = new ContourSegmenter().segment(pts);
            List<Pnt2d> corners = poly.getCorners();
            if (corners.size() != 4 ||                                     // pack into a local method
                Polygons.circularity(corners) < minCircularity ||           // parameter!
                Polygons.convexity(corners) != -1) {
                continue;
            }

            // Estimate homography and locate corners
            // MarkerLocator locator = new SimpleMarkerLocator();
            MarkerLocator locator = new LeastSquaresMarkerLocator();
            List<Pnt2d> refinedCorners = locator.getCorners(poly);

            // B: Extract the canonical marker image and read the marker's bitcode
            MarkerScanner extractor = new MarkerScanner(ip, dictionary);
            BitVector bitCode = extractor.getMarkerData(corners, thr);

            // C: Lookup the bitcode in the dictionary (all rotations)
            LookupResult lookup = dictionary.lookup(bitCode, maxCorrectionRate);
            if (lookup == null) {
               continue;
            }
            // Rotate corners to canonical to align with ArUco pattern printouts
            // (corner 0 is the top-left corner of the marker)
            Collections.rotate(refinedCorners, lookup.rotation());
            markerDetectionResults.add(new DetectionResult(lookup, refinedCorners));
        }

        return markerDetectionResults;
    }

    // -------------------------------------------------------------------------

    /**
     * Represents the result of a single marker detection.
     */
     public record DetectionResult(
             int markerId,
             int rotation,
             int hammingDist,
             List<Pnt2d> corners)
    {

         DetectionResult(LookupResult lookup, List<Pnt2d> corners) {
             this(lookup.markerIndex(), lookup.rotation(), lookup.hammingDistance(), corners);
         }
     }
}