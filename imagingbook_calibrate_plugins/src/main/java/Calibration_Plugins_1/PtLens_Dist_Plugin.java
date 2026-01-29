/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package Calibration_Plugins_1;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.PtLensDistortion;
import imagingbook.calibrate.distortion.RectificationMapping;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.common.geometry.mappings.Mapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.image.interpolation.InterpolationMethod;

import static imagingbook.common.ij.DialogUtils.formatText;

public class PtLens_Dist_Plugin implements PlugInFilter {

    ImagePlus im;

    double focalLength = 50;
    double sensorWidth = 36;
    double sensorHeight = 24;
    // PtLens distortion parameters;
    double a = -0.0030105199701668;
    double b = 0.00307881852077996;
    double c = -0.0107098456707285;

    @Override
    public int setup(String s, ImagePlus im) {
        this.im = im;
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        int W = im.getWidth();
        int H = im.getHeight();

        if (!runDialog()) {
            return;
        }

        IJ.log("f = " + focalLength);

        double pw = sensorWidth / W;        // width of  pixel
        double ph = sensorHeight / H;        // width of  pixel
        double alpha = focalLength / pw;    // focal length in pixels
        double beta = focalLength / ph;

        IJ.log("pw = " + pw);
        IJ.log("ph = " + ph);
        IJ.log("alpha = " + alpha);
        IJ.log("beta = " + beta);

        double[] linParams = {alpha, beta, 0, 0.5 * W, 0.5 * H};
        double[] distParams = {a, b, c};

        // ----------------------------------------------------------------------------------
        Camera cam = new StandardCamera(linParams, null);
        double scale = PtLensDistortion.findScale(cam);
        IJ.log("scale = " + scale);
        DistortionModel dist = new PtLensDistortion(distParams, scale);
        cam.setDistortion(dist);
        IJ.log("cam = " +cam);
        // ----------------------------------------------------------------------------------
        /* what we want:

        Camera cam = new StandardCamera(linParams);

        */
        // ----------------------------------------------------------------------------------

        Mapping2D mapping = new RectificationMapping(cam);	// inverse, ie., maps target to source
        ImageProcessor source = im.getProcessor();
        ImageProcessor target = source.createProcessor(W, H);
        ImageMapper mapper = new ImageMapper(mapping, null, InterpolationMethod.Bicubic);

        IJ.log("Starting mapping");
        mapper.map(source, target);
        IJ.log("done");

        new ImagePlus("Target", target).show();
    }

    // -------------------------------------------------------------------------------------------

    private boolean runDialog() {
        GenericDialog gd = new GenericDialog(this.getClass().getSimpleName());
        // gd.addHelp(getJavaDocUrl());
        gd.setInsets(0, 0, 0);
        gd.addMessage(formatText(40, "Undistort image using PTLens model"));

        gd.addNumericField("Focal length (mm)", focalLength);
        gd.addNumericField("Sensor width (mm)", sensorWidth);
        gd.addNumericField("Sensor height (mm)", sensorHeight);

        gd.addMessage(formatText(40, "PTLens distortion params:"));
        gd.addNumericField("a", a);
        gd.addToSameRow();
        gd.addNumericField("b", b);
        gd.addToSameRow();
        gd.addNumericField("c", c);

        gd.showDialog();
        if (gd.wasCanceled())
            return false;

        focalLength = gd.getNextNumber();
        sensorWidth = gd.getNextNumber();
        sensorHeight = gd.getNextNumber();

        a = gd.getNextNumber();
        b = gd.getNextNumber();
        c = gd.getNextNumber();

        return true;
    }

}
