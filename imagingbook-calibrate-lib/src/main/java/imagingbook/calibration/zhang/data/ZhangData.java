/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.data;

import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.common.geometry.basic.Pnt2d;
import org.apache.commons.math3.linear.MatrixUtils;


/**
 * Supplies all numeric data for Zhang's demo calibration test suite.
 *
 * @author WB
 */
public abstract class ZhangData {
	
	public static final int ImageWidth = 640;
	public static final int ImageHeight = 480;
	public static final int NumberOfViews = ObservedPoints.pointData.length;
	
	public static Pnt2d[] getModelPoints() {
		return StandardModel.getPoints();
	}
	
	public static Pnt2d[] getObservedPoints(int viewNr) {
		return ObservedPoints.getPoints(viewNr);
	}
	
	public static Pnt2d[][] getAllObservedPoints() {
		Pnt2d obsPoints[][] = new Pnt2d[NumberOfViews][];
		for (int i = 0; i < NumberOfViews; i++) {
			int viewNr = i + 1;
			obsPoints[i] = getObservedPoints(viewNr);
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
		double[][] RT = CameraViews.getViewMatrix(viewNr);
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
		String num = imgShortTitle.substring(imgShortTitle.length() - 1, imgShortTitle.length());
		return Integer.decode(num) - 1;
	}

}
