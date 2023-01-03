/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.util;

import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.FloatProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.common.math.Matrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.awt.Color;


/**
 * This class defines ImageJ-related utility methods used for camera calibration.
 *
 * @author WB
 */
public class IjUtil {

	/**
	 * Creates a float-type image of the supplied matrix. Matrix rows/columns correspond to image rows/columns. Typical
	 * usage: makeImage("my matrix", M).show()
	 *
	 * @param title the image title
	 * @param M the matrix to be displayed
	 * @return the resulting image
	 */
	public static ImagePlus makeImage(String title, RealMatrix M) {
		float[][] fdata = Matrix.toFloat(M.transpose().getData());
		ImagePlus im = new ImagePlus(title, new FloatProcessor(fdata));
		im.show();
		return im;
	}
	
	// -------------------------------------------------------------------
	
	
	public static PlotWindow plotLensDistortionFunction(Camera cam, double rmax) {
		int n = 100;
//		double[] K = cam.getK();
		double[] xVals = new double[n];
		double[] yVals = new double[n];
		for (int i = 0; i < n; i++) {
			double r = i * (rmax / n);
			//double d = cam.distFun2(r * r); //getRadialDistortion(r, K);
			double d = cam.D(r);
			xVals[i] = r;
			yVals[i] = d;
		}
		Plot plot = new Plot("d(r)", "x", "f(x)");
		plot.setColor(Color.BLUE);
		plot.setLimits(0, 1.25, -0.1, 0.1);
		plot.addPoints(xVals, yVals, Plot.LINE);
		plot.draw();
		return plot.show();
	}
	
	// ---------------------------------------------------------------
	
//	public void showJacobian(MultivariateMatrixFunction jacobianFun, RealVector point) {
//		double[][] J = jacobianFun.value(point.toArray());
//		FloatProcessor fp = new FloatProcessor(J[0].length, J.length);
//		for (int i = 0; i < J.length; i++) {
//			for (int j = 0; j < J[i].length; j++) {
//				fp.setf(j, i, (float) J[i][j]); 
//			}
//		}
//		(new ImagePlus("Jacobian", fp)).show();
//	}
}
