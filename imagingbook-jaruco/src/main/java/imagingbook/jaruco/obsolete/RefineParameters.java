package imagingbook.jaruco.obsolete;

import imagingbook.common.util.ParameterBundle;
import imagingbook.jaruco.marker.ArucoMarkerDetector;

@Deprecated
public class RefineParameters implements ParameterBundle<ArucoMarkerDetector> {
    public double minRepDistance = 10;
    public double errorCorrectionRate = 3;
    public boolean checkAllOrders = true;
}
