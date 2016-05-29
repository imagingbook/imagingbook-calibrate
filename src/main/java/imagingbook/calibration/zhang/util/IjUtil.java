package imagingbook.calibration.zhang.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.lib.math.Matrix;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.apache.commons.math3.linear.RealMatrix;


/**
 * This class defines ImageJ-related utility methods used for camera
 * calibration.
 * @author WB
 *
 */
public class IjUtil {
	
	/**
	 * Creates a float-type image of the supplied matrix.
	 * Matrix rows/columns correspond to image rows/columns.
	 * Typical usage: makeImage("my matrix", M).show()
	 * @param title the image title
	 * @param M the matrix to be displayed
	 * @return the resulting image
	 */
	public static ImagePlus makeImage(String title, RealMatrix M) {
		float[][] fdata = Matrix.duplicateToFloat(M.transpose().getData());
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
	

}
