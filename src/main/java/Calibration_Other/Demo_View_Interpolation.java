package Calibration_Other;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.testdata.ZhangData;
import imagingbook.calibration.zhang.util.GridPainter;
import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.lib.ij.IjLogStream;
import imagingbook.lib.settings.PrintPrecision;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

/**
 * This plugin performs interpolation of views, given a sequence
 * of key views.
 * Translations (3D camera positions) are interpolated linearly.
 * Pairs of rotations are interpolated by linear mixture
 * of the corresponding quaternion representations (see
 * http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-17-quaternions/#How_do_I_interpolate_between_2_quaternions__).
 * @author WB
 *
 */
public class Demo_View_Interpolation implements PlugIn {
	
	static int NumberOfInterpolatedFrames = 10;
	static double PeakHeightZ = -0.5;
	
	static Color LineColor = Color.black;
	static Color BackGroundColor = Color.white;
	static boolean BeVerbose = false;
	
	static final String imgName = "CalibImageStack.tif";
	
	static {
		IjLogStream.redirectSystem();
		PrintPrecision.set(6);
	}
	
	@Override
	public void run(String arg0) {
		String path = ZhangData.getResourcePath(imgName);
		ImagePlus testIm = new Opener().openImage(path);
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}
		
		Camera cam = ZhangData.getCameraIntrinsics();
		Point2D[] modelPoints = ZhangData.getModelPoints();
		
		int w = testIm.getWidth();
		int h = testIm.getHeight();
		int M = testIm.getNSlices();
		
		ImageStack animation = new ImageStack(w, h);

		for (int a = 0; a < M; a++) {
			int b = (a + 1) % M;
			ViewTransform viewA = ZhangData.getViewTransform(a);	// view A
			ViewTransform viewB = ZhangData.getViewTransform(b);	// view B
			if (BeVerbose)
				System.out.println("**** doing views " + a +"/"+ ((a + 1) % M));

			Rotation rA = viewA.getRotation();
			Rotation rB = viewB.getRotation();
			double[] tA = viewA.getTranslation();
			double[] tB = viewB.getTranslation();
			
			// interpolation step k for view pair (a,b)
			for (int k = 0; k < NumberOfInterpolatedFrames; k++) {
				double alpha = (double) k / NumberOfInterpolatedFrames;
				
				// interpolate rotation
				Rotation rk = Lerp(rA, rB, alpha);
				
				// interpolate translation:
				double[] tk = Lerp(tA, tB, alpha);
				
				ViewTransform viewK = new ViewTransform(rk, tk);
				//viewK.print();
				
				// Create a new frame for the interpolated view
				// and project the target model:
				ImageProcessor frame = new ColorProcessor(w, h);
				if (BackGroundColor != null) {
					frame.setColor(BackGroundColor);
					frame.fill();
				}
				projectAndDrawPyramids(frame, cam, viewK, modelPoints);
				String title = String.format("frame-%d-%d", a, k);
				animation.addSlice(title, frame);
			}
		}
		
		new ImagePlus("Animation", animation).show();
	}
	
	Rotation Lerp(Rotation R0, Rotation R1, double alpha) {
		Quaternion qa = MathUtil.toQuaternion(R0);
		Quaternion qb = MathUtil.toQuaternion(R1);
		return MathUtil.toRotation(Lerp(qa, qb, alpha));
	}
	
	/**
	 * Linear quaternion interpolation (LERP)
	 * @param Q0
	 * @param Q1
	 * @param a
	 * @return
	 */
	Quaternion Lerp(Quaternion Q0, Quaternion Q1, double a) {
		return Quaternion.add(Q0.multiply(1 - a), Q1.multiply(a));
	}
	
	/**
	 * Linear translation interpolation
	 * @param t0
	 * @param t1
	 * @param a
	 * @return
	 */
	double[] Lerp(double[] t0, double[] t1, double a) {
		double[] t01 = t0.clone();
		for (int i = 0; i < t01.length; i++) {
			t01[i] = (1 - a) * t0[i] + a * t1[i];
		}
		return t01;
	}

	// ----------------------------------------------------------------------
			
	// draw pyramids instead of squares at Z = PeakHeightZ (inch)
	void projectAndDrawPyramids(ImageProcessor ip, Camera cam, ViewTransform view, Point2D[] modelPoints) {
		GridPainter painter = new GridPainter(ip);
		for (int i = 0; i < modelPoints.length; i += 4) {
			Point2D[] modelSq = new Point2D[4];
			Point2D[] imageSq = new Point2D[4];
			// 3D points p0,...,p3 define a model square in the Z=0 plane
			for (int j = 0; j < 4; j++) {
				modelSq[j] = modelPoints[i + j];
				imageSq[j] = MathUtil.toPoint2D(cam.project(view, modelSq[j]));
			}
			painter.lineCol = LineColor;
			painter.drawSquare(imageSq);
			
			// make the 3D pyramid peak:
			double[] modelPeak3d = new double[3];
			modelPeak3d[0] = (modelSq[0].getX() + modelSq[2].getX()) / 2;	// X
			modelPeak3d[1] = (modelSq[0].getY() + modelSq[2].getY()) / 2;	// Y
			modelPeak3d[2] = PeakHeightZ;	// Z
			
			// project and draw the pyramid peak:
			Point2D pk = MathUtil.toPoint2D(cam.project(view, modelPeak3d));
			painter.drawLine(imageSq[0], pk);
			painter.drawLine(imageSq[1], pk);
			painter.drawLine(imageSq[2], pk);
			painter.drawLine(imageSq[3], pk);
		}
	}
	

}
