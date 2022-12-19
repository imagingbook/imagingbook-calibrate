/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Obsolete;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.LogStream;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.common.math.PrintPrecision;
import imagingbook.core.resource.ImageResource;

import java.awt.Color;
import java.awt.geom.Point2D;


/**
 * This plugin projects opens an image stack containing the 5 Zhang test images and projects the model points into each
 * view, using the (known) camera and view parameters. All data are part of Zhang's demo data set that comes with the
 * EasyCalib program. NO CALIBRATION is performed here!
 *
 * @author W. Burger
 * @version 2022/04/14
 */
public class Demo_Zhang_Projection implements PlugIn {
	
	static ImageResource resource = CalibrationImage.CalibImageStack;
	
	static Color BackGroundColor = Color.white;
	static Color LineColor = Color.magenta;
	static boolean BeVerbose = false;
	
	static {
		LogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	

	public void run(String arg0) {
		// open the test image (stack):
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();
		
		// get the 3D model points:
		Point2D[] modelPoints = ZhangData.getModelPoints();
		
		// get the camera intrinsics (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();
		
		// get all view parameters (extrinsics):
		ViewTransform[] views = ZhangData.getAllViewTransforms();
		int M = views.length;

		// project and draw the model into the views:
		ImageStack stack = testIm.getStack();
		for (int i = 0; i < M; i++) {
			// project the model points:
			Point2D[] projectedPoints = camera.project(views[i], modelPoints);
			ImageProcessor ip = stack.getProcessor(i + 1);
			// draw the squares:
			GridPainter painter = new GridPainter(ip);
			painter.lineCol = LineColor;
			painter.drawSquares(projectedPoints);
		}
		testIm.updateAndDraw();
	}

}
