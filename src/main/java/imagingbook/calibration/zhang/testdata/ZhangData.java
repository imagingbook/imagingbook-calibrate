package imagingbook.calibration.zhang.testdata;

import java.awt.geom.Point2D;
import java.net.URL;

import org.apache.commons.math3.linear.MatrixUtils;

import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;


/**
 * Supplies all data for Zhang's demo calibration test suite.
 * @author WB
 *
 */
public class ZhangData {
	
	public static final int ImageWidth = 640;
	public static final int ImageHeight = 480;
	public static final int NumberOfViews = ZhangObservedPoints.pointData.length;
	
	public static Point2D[] getModelPoints() {
		return ZhangStandardModel.getPoints();
	}
	
	public static Point2D[] getObservedPoints(int viewNr) {
		return ZhangObservedPoints.getPoints(viewNr);
	}
	
	public static Point2D[][] getAllObservedPoints() {
		Point2D obsPoints[][] = new Point2D[NumberOfViews][];
		for (int i = 0; i < NumberOfViews; i++) {
			int viewNr = i + 1;
			obsPoints[i] = ZhangData.getObservedPoints(viewNr);
		}
		return obsPoints;
	}
	
	public static ViewTransform[] getAllViewTransforms() {
		ViewTransform[] viewtransforms = new ViewTransform[NumberOfViews];
		for (int i = 0; i < NumberOfViews; i++) {
			viewtransforms[i] = getViewTransform(i);
		}
//		return new ViewTransform[] 
//			{getViewTransform(1), getViewTransform(2), getViewTransform(3), getViewTransform(4), getViewTransform(5)};
		return viewtransforms;
	}
	
	public static ViewTransform getViewTransform(int viewNr) {
		double[][] RT = ZhangCameraViews.getViewMatrix(viewNr);
		return (RT == null) ? null : new ViewTransform(MatrixUtils.createRealMatrix(RT));
	}
	
	public static Camera getCameraIntrinsics() {
		// http://research.microsoft.com/en-us/um/people/zhang/calib/Calibration/Calib.txt
		return new Camera (
				832.5,   832.53, 0.204494, 	// alpha, beta, gamma, (!)
				303.959, 206.585,			// c_x, c_y
				-0.228601, 0.190353);		// k1, k2
	}
		
	public static int extractViewNumber(String imgShortTitle) {
//		String imgShortTitle = im.getShortTitle();
		String numba = imgShortTitle.substring(imgShortTitle.length() - 1, imgShortTitle.length());
		return Integer.decode(numba) - 1;
	}
	
	/**
	 * Returns the path to the test image for the given view number
	 * @param vn view number (0,...,4)
	 * @return
	 */
	public static String getViewImagePath(int vn) {
		String imgName = "CalibIm" + (vn + 1) + ".gif";
		String path = getResourcePath(imgName);
//		File imgPath = new File(imgDir + imgName);
//		return imgPath.getAbsolutePath();
		return path;
	}
	
	
	private static final String resourcePath = "resources/";
	
	//TODO: replicated from imagingbook.lib.util.FileUtils.java
	public static String getResourcePath(String name) {
		URL url = ZhangData.class.getResource(resourcePath + name);
		if (url == null) {
			return null;
		}
		else {
			return url.getPath();
		}
	}
	
	// -----------------------------------------------------
	
	public static void main(String[] args) {
		System.out.println("testing access to resources:");
		String path = "resources/testFile1.txt";
		URL location = ZhangData.class.getResource(path);
		
		if (location == null) {
			System.out.println("Resource not found: " + path);
		}
		else {
			System.out.println("Class found: " + location.toString());
		}
	}
	

}
