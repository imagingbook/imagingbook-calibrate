/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2023 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.calibration.zhang.util.MathUtil;
import imagingbook.common.color.sets.BasicAwtColor;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.core.plugin.JavaDocHelp;
import imagingbook.core.resource.ImageResource;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static imagingbook.common.ij.DialogUtils.formatText;

/**
 * This plugin performs interpolation of views, given a sequence of key views. Translations (3D camera positions) are
 * interpolated linearly. Pairs of rotations are interpolated by linear mixture of the corresponding quaternion
 * representations (see
 * http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-17-quaternions/#How_do_I_interpolate_between_2_quaternions__).
 *
 * @author WB
 * @version 2022/12/19
 */
public class View_Interpolation_Demo implements PlugIn, JavaDocHelp {

	private static ImageResource resource = CalibrationImage.CalibImageStack;
	private static int NumberOfInterpolatedFrames = 10;
	private static double PeakHeightZ = -1.5;
	private static BasicAwtColor BackGroundColor = BasicAwtColor.White;
	private static BasicAwtColor LineColor = BasicAwtColor.Black;
	private static double LineWidth = 1.0;


	public void run(String arg0) {
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!"); 
			return;
		}

		if (!runDialog()) {
			return;
		}

		Camera cam = ZhangData.getCameraIntrinsics();
		Pnt2d[] modelPoints = ZhangData.getModelPoints();

		final int w = testIm.getWidth();
		final int h = testIm.getHeight();
		final int M = testIm.getNSlices();

		ImageStack animStack = new ImageStack(w, h);
		ByteProcessor bgIp = new ByteProcessor(w, h);	// background image for all stack slices
		bgIp.setColor(BackGroundColor.getColor());
		bgIp.fill();

		ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
		ola.setStroke(new ColoredStroke(LineWidth, LineColor.getColor()));

		int sliceNo = 1;
		for (int A = 0; A < M; A++) {
			int B = (A + 1) % M;
			ViewTransform viewA = ZhangData.getViewTransform(A);	// view A
			ViewTransform viewB = ZhangData.getViewTransform(B);	// view B

			Rotation rA = viewA.getRotation();
			Rotation rB = viewB.getRotation();
			double[] tA = viewA.getTranslation();
			double[] tB = viewB.getTranslation();

			// interpolation step k for view pair (A,B)
			for (int k = 0; k < NumberOfInterpolatedFrames; k++) {
				double alpha = (double) k / NumberOfInterpolatedFrames;
				Rotation rk = MathUtil.Lerp(rA, rB, alpha);	// interpolate rotation
				double[] tk = MathUtil.Lerp(tA, tB, alpha);	// interpolate translation
				ViewTransform viewK = new ViewTransform(rk, tk);

				String sliceLabel = String.format("frame-%d-%d", A, k);
				animStack.addSlice(sliceLabel, bgIp);	// dummy image with white background
				ola.setStackPosition(sliceNo++);
				for (Shape s : makePyramids(cam, viewK, modelPoints)) {
					ola.addShape(s);
				}
			}
		}
		ImagePlus animIm = new ImagePlus("Animation", animStack);
		animIm.setOverlay(ola.getOverlay());
		animIm.show();
	}

	// ----------------------------------------------------------------------

	List<Shape> makePyramids(Camera cam, ViewTransform view, Pnt2d[] modelPoints) {
		List<Shape> shapes = new ArrayList<>();
		for (int i = 0; i < modelPoints.length; i += 4) {
			Pnt2d[] modelSq = new Pnt2d[4];
			Pnt2d[] imageSq = new Pnt2d[4];
			// 3D points p0,...,p3 define a model square in the Z=0 plane
			for (int j = 0; j < 4; j++) {
				modelSq[j] = modelPoints[i + j];
				imageSq[j] = MathUtil.toPnt2d(cam.project(view, modelSq[j]));
			}
			// make the 3D pyramid peak and project to 2D:
			double[] modelPeak3d = new double[3];
			modelPeak3d[0] = (modelSq[0].getX() + modelSq[2].getX()) / 2;	// X
			modelPeak3d[1] = (modelSq[0].getY() + modelSq[2].getY()) / 2;	// Y
			modelPeak3d[2] = PeakHeightZ;	// Z
			Pnt2d pk = MathUtil.toPnt2d(cam.project(view, modelPeak3d));
			// make and add the projected pyramid for this model quad:
			shapes.add(makePyramidShape(imageSq, pk));
		}
		return shapes;
	}

	private Shape makePyramidShape(Pnt2d[] pnts, Pnt2d pk) {    // takes 4 base points + 1 peak point
		Path2D path = new Path2D.Double();
		// draw the base quad:
		path.moveTo(pnts[0].getX(), pnts[0].getY());
		for (int j = 1; j < 4; j++) {
			path.lineTo(pnts[j].getX(), pnts[j].getY());
		}
		path.closePath();
		// draw lines to pyramid peak
		for (int j = 0; j < 4; j++) {
			path.moveTo(pnts[j].getX(), pnts[j].getY());
			path.lineTo(pk.getX(), pk.getY());
		}
		return path;
	}

	// ------------------------------------------------------------------------------------

	private boolean runDialog() {
		GenericDialog gd = new GenericDialog(this.getClass().getSimpleName());
		gd.addHelp(getJavaDocUrl());
		gd.setInsets(0, 0, 0);
		gd.addMessage(formatText(40,
				"This plugin performs interpolation of views, given a sequence of key views."));

		gd.addNumericField("Number of interpolated frames", NumberOfInterpolatedFrames);
		gd.addNumericField("Pyramid peak height (inches)", PeakHeightZ);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		NumberOfInterpolatedFrames = (int) gd.getNextNumber();
		PeakHeightZ = gd.getNextNumber();
		return true;
	}

}
