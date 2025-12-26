package imagingbook.jaruco;

import imagingbook.common.util.ParameterBundle;

@Deprecated
public class RefineParameters implements ParameterBundle<ArucoDetector> {
    public double minRepDistance = 10;
    public double errorCorrectionRate = 3;
    public boolean checkAllOrders = true;
}
