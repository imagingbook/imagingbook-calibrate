package imagingbook.jaruco.marker;


import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Polygon2d;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.common.util.ParameterBundle;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.dict.ArucoDictionary;
import imagingbook.jaruco.dict.ArucoDictionary.DictionaryLookupResult;

import java.util.ArrayList;
import java.util.List;

import static imagingbook.jaruco.marker.MarkerLocator.Type.ParabolicFit;

/**
 * Performs Aruco marker detection.
 */
public class ArucoMarkerDetector {

    /**
     * A bundle of parameters for {@link ArucoMarkerDetector}.
     */
    public static class Parameters implements ParameterBundle<ArucoMarkerDetector> {
        /** Minimum number of contour points to be considered a marker candidate. */
        public int minContourLength = 50;
        /** Minimum circularity of contour to be considered a marker candidate. */
        public double minCircularity = 0.5;
        /** Relative straightness tolerance for polygon segmentation (used by {@link ContourSegmenter}) */
        public double polygonalApproxAccuracyRate = 0.03;
        /** Fraction of the dictionary's correctable bit errors to actually use (used by {@link ArucoDictionary} */
        public double maxCorrectionRate = 1.0;
        /** Type of {@link MarkerLocator} to use. */
        public MarkerLocator.Type locatorType = ParabolicFit;
        /** Fraction of quad side length used for fitting (used by {@link StraightMarkerLocator}) */
        public double cornerSupportFraction = 0.05;
    }

    private final ArucoDictionary dictionary;
    private final Parameters params;

    // -------------------------------------------------------------------------

    /**
     * Constructor.
     * @param dictionary a {@link ArucoDictionary} instance
     */
    public ArucoMarkerDetector(ArucoDictionary dictionary) {
        this(dictionary, new Parameters());
    }

    public ArucoMarkerDetector(ArucoDictionary dictionary, Parameters params) {
        this.params = params;
        this.dictionary = dictionary;
    }

    // -------------------------------------------------------------------------

    /**
     * The core method. Tries to locate and identify markers in the given image.
     * @param ip the input image
     * @return a (possibly empty) list of {@link DetectionResult} instances
     */
    public List<DetectionResult> detectMarkers(ImageProcessor ip) {
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

        List<DetectionResult> detections = new ArrayList<>();

        // process all contours and collect results in detections
        for (Contour candidate : ics) {
            processOneCandidate(candidate, ip, thr, detections);
        }

        // multi-threaded version:
        // ics.parallelStream().forEach(candidate ->
        //         processOneCandidate(candidate, ip, thr, detections));

        return detections;
    }

    void processOneCandidate(Contour contour, ImageProcessor ip, int thr, List<DetectionResult> detections) {
        Polygon2d poly = contour.getPolygon();
        // List<Pnt2d> pts = contour.getPointList();
        if (poly.length() < params.minContourLength) {                          // parameter!
            return;
        }
        // A. Segment contour and extract quad
        ContourSegmenter segmenter = new ContourSegmenter(params.polygonalApproxAccuracyRate);
        SegmentedPolygon segPoly = segmenter.segment(poly);
        Polygon2d corners = segPoly.getCornerPolygon();
        if (corners.length() != 4 ||                                     // pack into a local method
                corners.getCircularity() < params.minCircularity ||           // parameter!
                corners.getConvexity() != -1) {
            return;
        }

        // B. Estimate homography and locate corners
        MarkerLocator locator = MarkerLocator.createFrom(params);
        Polygon2d initialCorners = locator.getMarkerCorners(segPoly);

        // C: Extract the canonical marker image and read the marker's bitcode
        MarkerScanner extractor = new MarkerScanner(ip, dictionary);
        BitVector bitCode = extractor.getMarkerData(initialCorners, thr);

        // D: Lookup the bitcode in the dictionary (all rotations)
        DictionaryLookupResult lookup = dictionary.lookup(bitCode, params.maxCorrectionRate);
        if (lookup == null) {
            return;
        }

        // E. Rotate corners to canonical to align with ArUco pattern printouts
        // (corner 0 is the top-left corner of the marker)
        // Corners are in CW order (in image coordinate system)
        Polygon2d finalCorners = initialCorners.rotate(-lookup.rotation()); // correct but CCW
          // TODO: bring corners to CW order (clumsy!!)
        // finalCorners = new Polygon2d(finalCorners.getPnt(0), finalCorners.getPnt(3), finalCorners.getPnt(2), finalCorners.getPnt(1));

        // Bring finalCorners to CW order (using special reverse()!)
        DetectionResult result = new DetectionResult(lookup, finalCorners.reverse());
        // Merge everything into the result.
        detections.add(result);
    }

    // -------------------------------------------------------------------------

    /**
     * Represents the result of a single marker detection.
     */
     public record DetectionResult(
            DictionaryLookupResult lookup,
            Polygon2d corners) { }
}