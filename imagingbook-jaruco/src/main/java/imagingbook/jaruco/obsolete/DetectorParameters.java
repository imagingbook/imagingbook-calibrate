package imagingbook.jaruco.obsolete;

import imagingbook.common.util.ParameterBundle;
import imagingbook.jaruco.ArucoMarkerDetector;

@Deprecated
public class DetectorParameters implements ParameterBundle<ArucoMarkerDetector> {
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


    public static int PYRAMID_LEVELS = 5;

}
