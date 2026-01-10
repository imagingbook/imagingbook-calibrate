package imagingbook.jaruco.calibrate;

import ij.IJ;
import imagingbook.calibration.Calibration;
import imagingbook.calibration.Camera;
import imagingbook.calibration.ViewTransform;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;

public class DoZhangExample {

    static boolean ListCameraIntrinsics = true;
    static boolean ListCameraViews = true;

    public static void main(String[] args) {

        Pnt2d[] modelPoints = ZhangData.getModelPoints();
        Camera camReference = ZhangData.getCamera();
        Pnt2d[][] obsPoints = ZhangData.getAllObservedPoints();
        int M = obsPoints.length;

        // Set up the calibrator ------------------------------------------

        Calibration.Parameters params = new Calibration.Parameters();
        params.normalizePoints = true;
        params.useNumericJacobian = true;
        params.debug = false;

        Calibration zcalib = new Calibration(params, modelPoints, 640, 480);
        for (int i = 0; i < M; i++) {
            zcalib.addView(modelPoints, obsPoints[i]);
        }

        // Perform calibration ------------------------------------------

        Camera camFinal = zcalib.calibrate();
        if (camFinal == null) {
            System.out.println("Calibration failed");
            return;
        }
        ViewTransform[] finalViews = zcalib.getFinalViews();

        // Show results ------------------------------------------

        if (ListCameraIntrinsics) {
            IJ.log("\n**** Intrinsic camera parameters (common to all views): ****");
            IJ.log("Final estimate:\n   " + camFinal.toString());
            IJ.log("Reference (from EasyCalib):\n   " + camReference.toString());
        }

        if (ListCameraViews) {
            IJ.log("\n**** Camera view parameters (3D rotation and translation): ****");
            for (int i = 0; i < M; i++) {
                IJ.log("View " + i + ":\n" + finalViews[i].toString());
            }
            IJ.log(String.format("\nSquared projection error: %.3f\n",
                    zcalib.getProjectionError(camFinal, finalViews, obsPoints)));
        }
    }
}
