package imagingbook.jaruco;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.linear.ProjectiveMapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.regions.Contour;
import imagingbook.common.regions.ContourTracer;
import imagingbook.common.regions.RegionContourSegmentation;
import imagingbook.common.threshold.global.OtsuThresholder;
import imagingbook.common.util.ParameterBundle;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static imagingbook.jaruco.ContourSimplifierClosed.getCircularity;
import imagingbook.jaruco.ArucoDictionary.LookupResult;

public class ArucoDetector {

    public enum CornerRefineMethod {
        CORNER_REFINE_NONE,
        /// < Tag and corners detection based on the ArUco approach
        CORNER_REFINE_SUBPIX,
        /// < ArUco approach and refine the corners locations using corner
        /// subpixel accuracy
        CORNER_REFINE_CONTOUR,
        /// < ArUco approach and refine the corners locations using the
        /// contour-points line fitting
        CORNER_REFINE_APRILTAG, ///< Tag and corners detection based on the AprilTag 2 approach @cite wang2016iros
    }

    ;

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
        int markerId;
        Pnt2d[] corners;
        Pnt2d[] rejectedPoints;

        public DetectionResult(int markerId, Pnt2d[] corners, Pnt2d[] rejectedPoints) {
            this.markerId = markerId;
            this.corners = corners;
            this.rejectedPoints = rejectedPoints;
        }
    }

    // -------------------------------------------------------------------------

    private final ArucoDictionary dictionary;
    private final DetectorParameters detectorParams;
    private final RefineParameters refineParams;

    public ArucoDetector(ArucoDictionary dictionary) {
        this(dictionary, new DetectorParameters(), new RefineParameters());
    }

    public ArucoDetector(ArucoDictionary dictionary,
                         DetectorParameters detectorParams,
                         RefineParameters refineParams) {
        this.dictionary = dictionary;
        this.detectorParams = detectorParams;
        this.refineParams = refineParams;
    }

    // -------------------------------------------------------------------------

    public List<DetectionResult> detectMarkers(ImagePlus im) {

        // STEP 1: convert input image to grayscale:

        ImageProcessor ip = im.getProcessor();
        ByteProcessor gray = ip.convertToByteProcessor();

        // STEP 2: threshold image and find closed contours:
        new OtsuThresholder().threshold(gray);
        // ImagePlus ig = new ImagePlus(im.getShortTitle() + "-gray", gray);
        ContourTracer ct = new RegionContourSegmentation(gray);
        List<? extends Contour> ocs = ct.getOuterContours();
        List<? extends Contour> ics = ct.getInnerContours();
        IJ.log("outer contours: " + ocs.size());
        IJ.log("inner contours: " + ics.size());

        // Keep only contours with more than 50 points (parameter?)
        List<? extends Contour> ocsCln =
                ocs.stream().filter(ctr -> ctr.getLength() > 50).toList();
        List<? extends Contour> icsCln =
                ics.stream().filter(ctr -> ctr.getLength() > 50).toList();
        IJ.log("outer contours cleaned: " + ocsCln.size());
        IJ.log("inner contours cleaned: " + icsCln.size());

        // STEP 3: Simplify inner contours to polygons

        // List<List<Pnt2d>> ocsSmpl = new ArrayList<>();
        List<List<Pnt2d>> candidateBoxes = new ArrayList<>();

        // only keep inner contours with exactly 4 vertices:
        int k = 0;
       // double accuracyRate = detectorParams.polygonalApproxAccuracyRate;
        for (Contour ic : icsCln) {
            double tol = ic.getLength() * detectorParams.polygonalApproxAccuracyRate;
            // IJ.log("tolerance = " + tol);
            List<Pnt2d> iscln = ContourSimplifierClosed.simplify(ic, tol);   // simplified polygon

            // iscln = ContourSimplifier.cleanupCollinear(is, tol, true);   // optional cleanup, not needed

            IJ.log("iscln: size = " + iscln.size());
            // check if this is a convex 4-corner polygon that is not too elongated:
            if (iscln.size() == 4 &&
                    ContourSimplifier.isConvex(iscln) &&
                    getCircularity(iscln) > 0.5) {
                // print(ic.getPointList(), "inner orig" + k);
                // add to candidate marker regions
                candidateBoxes.add(iscln);
                // print(iscln, "inner simple" + k);
                k++;
            }
        }

        // STEP 4: Process each candidate box:
        k = 0;
        for (List<Pnt2d> candidate : candidateBoxes) {

            // STEP 4a - CORNER REFINEMENT should come here!

            // STEP 4b - extract a 64 x 64 rectified subimage
            ByteProcessor markerIp = extractMarkerImage(ip, candidate, 64); // parameter!
            new OtsuThresholder().threshold(markerIp);
            new ImagePlus("Marker " + k, markerIp).show();

            // STEP 4c - sample marker fields to generate the 1D marker pattern
            BitSet sampleBits = extractMarkerBits(markerIp);

            // STEP 4d - Lookup marker pattern in dictionary
            double maxCorrectionRate = 1.0; // TODO: CHECK!!!
            LookupResult result = dictionary.lookup(sampleBits, maxCorrectionRate);

            if (result != null) {
                // process lookup result
                // result.markerIndex, result.rotation, result.hammingDistance
            }

            k++;
        }

        return null;
    }

    private BitSet extractMarkerBits(ByteProcessor markerIp) {
        return null;
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

}