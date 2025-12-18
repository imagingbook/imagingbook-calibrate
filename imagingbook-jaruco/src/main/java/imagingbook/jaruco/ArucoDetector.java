package imagingbook.jaruco;


import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.math.Matrix;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.common.util.ParameterBundle;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.ArucoDictionary.LookupResult;
import imagingbook.jaruco.gui.ZoomableImagePlus;
import imagingbook.jaruco.obsolete.MarkerDetectionResult_obsolete;
import imagingbook.jaruco.obsolete.MarkerOutline;
import imagingbook.jaruco.util.Polygons;

import java.util.ArrayList;
import java.util.Arrays;
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
    // -------------------------------------------------------------------------

    /**
     * The core method. Tries to locate and identify markers in the given image.
     * @param ip the input image
     * @return a (possibly empty) list of {@link MarkerDetectionResult_obsolete} instances
     */
    public List<MarkerDetectionResult_obsolete> detectMarkers(ImageProcessor ip) {
        MarkerOutline.resetUid();
        List<MarkerDetectionResult_obsolete> markerDetectionResultObsoletes = new ArrayList<>();

        // STEP 1: convert input image to grayscale:
        ByteProcessor gray = ip.convertToByteProcessor();

        int initThr = Math.round(new OtsuThresholder().getThreshold(gray));
        for (int thr = initThr; thr <= initThr; thr++) {

            // STEP 2: Threshold the input image and find candidate outlines
            List<MarkerOutline> contours = extractContours(gray, thr);

            // STEP 3: Simplify inner contours to 4-corner convex polygons
            List<MarkerOutline> candidateOutlines = extractMarkerQuads(contours);
            // System.out.println(candidateOutlines.get(0).toString());

            // STEP 4: Process each candidate box and collect the results
            for (MarkerOutline outline : candidateOutlines) {
                MarkerDetectionResult_obsolete dr = processOneOutline(ip, outline);
                if (dr != null) {
                    markerDetectionResultObsoletes.add(dr);
                }
            }
        }
        return markerDetectionResultObsoletes;
    }

    // ------------------ NEW VERSION !! --------------------------------------

    public List<MarkerDetectionResult> detectMarkers2(ImageProcessor ip) {
        MarkerOutline.resetUid();
        List<MarkerDetectionResult> markerDetectionResults = new ArrayList<>();

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

        System.out.println("ics = " + ics.size());
        System.out.println("ics[0] = " + ics.get(0).getLength());

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

            System.out.println("found corners = " + corners.size());

            // Estimate homography and locate corners
            MarkerLocator locator = new MarkerLocator(poly);
            ProjectiveMapping2D unitMapping = locator.getUnitMapping();

            // B: Extract the canonical marker image and read the marker's bitcode
            MarkerExtractor extractor = new MarkerExtractor(ip, dictionary.getMarkerSize());
            BitVector bitCode = extractor.getMarkerBits(unitMapping, thr);
            System.out.println("bitcode = " + bitCode);

            // C: Lookup the bitcode in the dictionary (all rotations)
            LookupResult lookup = dictionary.lookup(bitCode, maxCorrectionRate);
            System.out.println("lookup = " + lookup);

            if (lookup == null) {
               continue;
            }

            markerDetectionResults.add(new MarkerDetectionResult(lookup, poly));
            // take care of rotation! Extract exact patch corner positions
            // by projecting the unit square.
            System.out.println("markerDetectionResults(0) = " + markerDetectionResults.get(0));
        }

        return markerDetectionResults;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /**
     * Applies global thresholding to the input image, performs region
     * segmentation on the resulting binary image and returns the detected
     * (inner) contours as a list of raw {@link MarkerOutline} instances.
     * @param gray
     * @param threshold
     * @return
     */
    public static List<MarkerOutline> extractContours(ByteProcessor gray, int threshold) {
        // STEP 2a: threshold image for region/contour extraction:
        ByteProcessor binary = (ByteProcessor) gray.duplicate();
        binary.threshold(threshold);

        // STEP 2b: segment and find closed contours:
        ContourTracer ct = new RegionContourSegmentation(binary);
        // since white is considered foreground, outer contours
        // of black regions are actually INNER contours:
        List<? extends Contour> ics = ct.getInnerContours();
        List<MarkerOutline> outlines = new ArrayList<>(ics.size());
        for (Contour ctr : ics) {
            int uid = MarkerOutline.nextUid();
            // TODO: eliminate unworthy contours right here!
            outlines.add(new MarkerOutline(uid, threshold, ctr.getPointList()));
        }
        return outlines;
    }

    /**
     * Takes a list of raw contours ({@link MarkerOutline} instances) and tries
     * to segment each to a convex four-corner polygon (quad) with sufficient area to
     * boundary length ratio. Resulting corner vertices are checked to run
     * in counter-clockwise order and reversed if needed.
     * Surviving quads are returned as a list of ???.
     * @param outlines a list of rwa contours
     * @return
     */
    List<MarkerOutline> extractMarkerQuads(List<MarkerOutline> outlines) {
        List<MarkerOutline> quads = new ArrayList<>();
        ContourSegmenter contourSegmenter = new ContourSegmenter();
        for (MarkerOutline mol : outlines) {
            MarkerOutline quad = contourSegmenter.extractQuad(mol);
            if (quad != null) {
                quads.add(quad);

            }
        }
        return quads;
    }

    MarkerDetectionResult_obsolete processOneOutline(ImageProcessor ip, MarkerOutline markerOutline) {
        // System.out.println("processOneCandidateBox " + k);
        // STEP 4a - CORNER REFINEMENT should come here!

        // STEP 4b - extract a small rectified subimage
        int targetSize = 5 * (this.dictionary.getMarkerSize() + 2); // fields with 5x5 pixels (parameter!?)
        ByteProcessor markerIp = extractMarkerImage(ip, markerOutline, targetSize);
        new ZoomableImagePlus("Marker raw", markerIp.duplicate()).show(20);

        // STEP 4c - sample marker fields to generate the 1D marker pattern
        BitVector sampleBits = extractMarkerBits(markerIp, markerOutline.threshold);

        // STEP 4d - Lookup marker pattern in dictionary
        double maxCorrectionRate = 1.0; // TODO: CHECK!!!
        LookupResult lookup = dictionary.lookup(sampleBits, maxCorrectionRate);

        if (lookup != null) {
            // Collections.rotate(markerOutline.polygon, lookup.rotation);
            markerOutline.rotatePolygon(lookup.rotation());   // rotate vertices to canonical state
            // return new MarkerDetectionResult_obsolete(lookup, markerOutline, null);   // TODO: rejectedPoints?
            return new MarkerDetectionResult_obsolete(lookup.markerIndex(), lookup.rotation(),
                    lookup.hammingDistance(), markerOutline, null);
        }
        else {
            return null;
        }
    }

    /**
     * Extracts the outlined 4-corner patch from the original image into a
     * new square image of the specified size.
     * The vertices in {@code outline} must be in counter-clockwise order (i.e.
     * the result of {@link Polygons#convexity(List)} must be 1).
     *
     * @param origIp the source image to extract from
     * @param outline the polygon (plus additional data) outlining the marker patch
     * @param targetSize the size (width and height) of the square marker patch image
     * @return the extracted marker patch image
     */
    ByteProcessor extractMarkerImage(ImageProcessor origIp, MarkerOutline outline, int targetSize) {
        Pnt2d[] sourcePts = outline.polygon.toArray(new Pnt2d[0]);
        System.out.println("extractMarkerImage: convexity = " + Polygons.convexity(outline.polygon));
        Pnt2d[] targetPts = {   // enlarge target square by 1/2 pixel
                Pnt2d.from(-0.5, -0.5),                         // NOTE: square winds CW!
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

    /**
     * Takes a square marker patch image and extracts the bit pattern assuming
     * the marker structure specified by the current directory.
     * This is done by sampling the greyscale {@code markerPatch} image at the
     * associated N x N grid positions.
     * Each sample value is taken as the median of a 3x3 neighborhood around its
     * grid position. The size of the marker patch is assumed large enough
     * that each marker field has at least 5 x 5 pixels.
     * The median sample value for each field is then compared to
     * {@code threshold}, which is typically the threshold applied to obtain
     * the binary image for region and contour extraction.
     *
     * @param markerPatch
     * @param threshold
     * @return a {@link BitVector} holding the extracted bit sequence
     */
    BitVector extractMarkerBits(ByteProcessor markerPatch, int threshold) {
        // optionally wrap markerIp into an ImageAccessor to handle image borders (not strictly needed)
        // ScalarAccessor ia = ScalarAccessor.create(markerIp,
        //         OutOfBoundsStrategy.NearestBorder, InterpolationMethod.NearestNeighbor);
        int w = markerPatch.getWidth();
        int N = dictionary.getMarkerSize();
        double d = (double) w / (N + 2);    // NxN marker + 1 row/ 1 column around on each side
        BitVector bits = new BitVector(N * N);
        int k = 0;
        for (int i = 0; i < N; i++) {
            int y = (int) Math.round((1.5 + i) * d);
            for (int j = 0; j < N; j++) {
                int x = (int) Math.round((1.5 + j) * d);
                int g = get3x3Median(markerPatch, x, y);
                if (g >= threshold) {    // use threshold from initial thresholding
                    bits.setBit(k);
                }
                k++;
            }
        }
        return bits;
    }

    int get3x3Median(ByteProcessor ip, int x, int y) {
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

}