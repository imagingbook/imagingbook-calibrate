package imagingbook.jaruco.calibrate;

import ij.IJ;
import imagingbook.calibrate.Calibration;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;

import static imagingbook.calibrate.distortion.DistortionModelType.Radial2Term;
import static imagingbook.calibrate.distortion.DistortionModelType.RadialLateral;

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
        params.distortionModelType = Radial2Term; //Radial3Term;
        params.debug = false;

        Calibration calibration = new Calibration(params, 640, 480);
        for (int k = 0; k < M; k++) {
            // modelPoints are the same for every view:
            calibration.addView(modelPoints, obsPoints[k]);
        }

        // Perform calibration ------------------------------------------

        calibration.calibrate();
        Camera camFinal = calibration.getFinalCamera();
        if (camFinal == null) {
            System.out.println("Calibration failed");
            return;
        }

        // Show results ------------------------------------------

        if (ListCameraIntrinsics) {
            IJ.log("\n**** Intrinsic camera parameters (common to all views): ****");
            IJ.log("Initial camera estimate:\n   " + calibration.getInitialCamera().toString());
            IJ.log("Final camera estimate:\n   " + camFinal.toString());
            IJ.log("Reference (from EasyCalib):\n   " + camReference.toString());
        }

        if (ListCameraViews) {
            IJ.log("\n**** Camera view parameters (3D rotation and translation): ****");
            for (int k = 0; k < M; k++) {
                ViewTransform view = calibration.getFinalViewTransform(k);
                IJ.log("View " + k + ":\n" + view.toString());
                IJ.log(String.format("RMS error: %.3f\n", calibration.getRmsReprojectionError(k)));
            }


        }

        IJ.log(String.format("Total RMS error: %.3f\n", calibration.getRmsReprojectionError()));
        // IJ.log(String.format("Check RMS error: %.3f\n", calibration.getRmsReprojectionErrorCheck()));
    }
}
