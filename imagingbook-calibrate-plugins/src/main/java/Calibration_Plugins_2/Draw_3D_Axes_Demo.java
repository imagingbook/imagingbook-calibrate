package Calibration_Plugins_2;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagingbook.calibration.zhang.data.CalibrationImage;
import imagingbook.calibration.zhang.data.ZhangData;
import imagingbook.calibration.zhang.Camera;
import imagingbook.calibration.zhang.ViewTransform;
import imagingbook.common.color.sets.BasicAwtColor;
import imagingbook.common.ij.overlay.ColoredStroke;
import imagingbook.common.ij.overlay.ShapeOverlayAdapter;
import imagingbook.common.math.Matrix;
import imagingbook.core.resource.ImageResource;

import java.awt.Shape;
import java.awt.geom.Line2D;

import static imagingbook.common.ij.DialogUtils.splitLines;


/**
 * This plugin draws the projected X/Y/Z coordinate axes for each of the given camera views.
 *
 * @author WB
 * @version 2022/12/19
 */
public class Draw_3D_Axes_Demo implements PlugIn {
	
	private static ImageResource resource = CalibrationImage.CalibImageStack;
	private static BasicAwtColor xColor = BasicAwtColor.Red;
	private static BasicAwtColor yColor = BasicAwtColor.Green;
	private static BasicAwtColor zColor = BasicAwtColor.Blue;
	private static double StrokeWidth = 2.0;
	private static double AxisLength = 5;
	private static boolean FlipYaxis = true;

	public void run(String arg0) {

		// create a 3D model:
		double[] p0 = {0.0, 0.0, 0.0}; // 3D origin
		double[] p1 = {AxisLength, 0.0, 0.0};	// x-axis
		double[] p2 = {0.0, AxisLength, 0.0};	// y-axis
		double[] p3 = {0.0, 0.0, AxisLength};	// z-axis
		if (FlipYaxis) {
			Matrix.multiplyD(-1, p2);
		}

		// open the test image (stack):
		ImagePlus testIm = resource.getImagePlus();
		if (testIm == null) {
			IJ.error("Could not open calibration images!");
			return;
		}
		testIm.show();

		if (!runDialog()) {
			return;
		}

		// get pre-calculated camera intrinsics and view parameters (typically by calibration):
		Camera camera = ZhangData.getCameraIntrinsics();
		ViewTransform[] views = ZhangData.getAllViewTransforms();
		final int M = views.length;

		// project and draw the model into the views:
		ShapeOverlayAdapter ola = new ShapeOverlayAdapter();
		ColoredStroke xStroke = new ColoredStroke(StrokeWidth, xColor.getColor());
		ColoredStroke yStroke = new ColoredStroke(StrokeWidth, yColor.getColor());
		ColoredStroke zStroke = new ColoredStroke(StrokeWidth, zColor.getColor());

		for (int i = 0; i < M; i++) {
			ola.setStackPosition(i + 1);	// stack slice number
			ola.addShape(get3DAxisProjection(camera, views[i], p0, p1), xStroke);
			ola.addShape(get3DAxisProjection(camera, views[i], p0, p2), yStroke);
			ola.addShape(get3DAxisProjection(camera, views[i], p0, p3), zStroke);
		}

		testIm.setOverlay(ola.getOverlay());
	}

	private Shape get3DAxisProjection(Camera cam, ViewTransform V, double[] P1, double[] P2) {
		double[] u1 = cam.project(V, P1);
		double[] u2 = cam.project(V, P2);
		return new Line2D.Double(u1[0], u1[1], u2[0], u2[1]);
	}

	// ------------------------------------------------------------------------------------

	private boolean runDialog() {
		GenericDialog gd = new GenericDialog(this.getClass().getSimpleName());
		gd.setInsets(0, 0, 0);
		gd.addMessage(splitLines(40,
				"This plugin displays the 5 sample view images and",
				"projects the 3D coordinate axis onto the origin of ",
				"the calibration model, based on the pre-calculated",
				"calibration data. No calibration is performed!"));

		gd.addNumericField("Stroke width", StrokeWidth);
		gd.addEnumChoice("X-coordinate color", xColor);
		gd.addEnumChoice("Y-coordinate color", yColor);
		gd.addEnumChoice("Z-coordinate color", zColor);
		gd.addNumericField("3D axis length (inches)", AxisLength, 1);
		gd.addCheckbox("Flip y-axis", FlipYaxis);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		StrokeWidth = gd.getNextNumber();
		xColor = gd.getNextEnumChoice(BasicAwtColor.class);
		yColor = gd.getNextEnumChoice(BasicAwtColor.class);
		zColor = gd.getNextEnumChoice(BasicAwtColor.class);
		AxisLength = gd.getNextNumber();
		FlipYaxis = gd.getNextBoolean();
		return true;
	}

}
