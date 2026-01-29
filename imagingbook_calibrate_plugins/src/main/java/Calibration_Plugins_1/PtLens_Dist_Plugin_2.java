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
import imagingbook.calibrate.distortion.RadialDistortion;
import imagingbook.calibrate.distortion.RectificationMapping;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.calibrate.intrinsics.StandardCamera;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.common.geometry.mappings.Mapping2D;
import imagingbook.common.image.ImageMapper;
import imagingbook.common.image.OutOfBoundsStrategy;
import imagingbook.common.image.access.ImageAccessor;
import imagingbook.common.image.interpolation.InterpolationMethod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static imagingbook.common.ij.DialogUtils.formatText;
import static imagingbook.common.util.Timing.timeNanos;

public class PtLens_Dist_Plugin_2 implements PlugInFilter {

    ImagePlus im;

    double focalLength = 50;
    double sensorWidth = 36;
    double sensorHeight = 24;
    // PtLens distortion parameters;
    double a = -0.0030105199701668;
    double b = 0.00307881852077996;
    double c = -0.0107098456707285;

    double[] distParams = {a, b, c};

    boolean parallel = true;

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


        // ----------------------------------------------------------------------------------

        ImageProcessor source = im.getProcessor();
        ImageProcessor target = source.createProcessor(W, H);

        double WH = Math.min(W, H);
        IJ.log("WH = " + WH);
        final double scale = 2.0 / WH; //.min(W, H);
        IJ.log("scale = " + scale);
        final int uc = W / 2;
        final int vc = H / 2;
        IJ.log("uc = " + uc);
        IJ.log("vc = " + vc);

        ImageAccessor sourceAcc = ImageAccessor.create(source, OutOfBoundsStrategy.NearestBorder, InterpolationMethod.Bilinear, 0, 0);
        ImageAccessor targetAcc = ImageAccessor.create(target);
        RadialDistortion dist = new PtLensDistortion(distParams, scale);

        final int w = target.getWidth();
        final int h = target.getHeight();
        IJ.log("Starting mapping");
        long nanos = timeNanos(() -> {
            if (parallel)
                mapImagePar(sourceAcc, targetAcc, dist);
            else
                mapImage(sourceAcc, targetAcc, dist);
         });
        IJ.log("Took " + (nanos / 1000000.0) + " ms");

        new ImagePlus("Target", target).show();
    }

    // sequential version:
    void mapImage(ImageAccessor sourceAcc, ImageAccessor targetAcc, RadialDistortion dist) {
        final int w = targetAcc.getWidth();
        final int h = targetAcc.getHeight();
        final int uc = w / 2;
        final int vc = h / 2;

        for (int v = 0; v < h; v++) {
            int dv = v - vc;
            for (int u = 0; u < w; u++) {
                int du = u - uc;
                if (du == 0 && dv == 0) {
                    targetAcc.setPix(u, v, sourceAcc.getPix(uc, vc));
                }
                else {
                    double r = Math.hypot(du, dv);
                    double rr = dist.fRad(r);
                    double xx = uc + rr * du / r;
                    double yy = vc + rr * dv / r;
                    float[] val = sourceAcc.getPix(xx, yy);
                    targetAcc.setPix(u, v, val);
                }
            }
        }
    }

    // parallel version:
    void mapImagePar(final ImageAccessor sourceAcc, final ImageAccessor targetAcc, RadialDistortion dist) {
        final int w = targetAcc.getWidth();
        final int h = targetAcc.getHeight();
        final int uc = w / 2;
        final int vc = h / 2;

        int threads = Runtime.getRuntime().availableProcessors();
        IJ.log("threads = " + threads);
        int rowsPerThread = (h + threads - 1) / threads; // Ceiling division
        IJ.log("rowsPerThread = " + rowsPerThread);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int t = 0; t < threads; t++) {
                // ImageAccessor sourceAcc = ImageAccessor.create(source, OutOfBoundsStrategy.NearestBorder, InterpolationMethod.Bicubic, 0, 0);
                // ImageAccessor targetAcc = ImageAccessor.create(target);
                // RadialDistortion dist = new PtLensDistortion(distParams, scale);
                final int startRow = t * rowsPerThread;
                final int endRow = Math.min(startRow + rowsPerThread, h);
                if (startRow >= h)
                    break; // Safety check
                executor.submit(() -> {
                    for (int v = startRow; v < endRow; v++) {
                        int dv = v - vc;
                        for (int u = 0; u < w; u++) {
                            int du = u - uc;
                            if (du == 0 && dv == 0) {
                                targetAcc.setPix(u, v, sourceAcc.getPix(uc, vc));
                            } else {
                                double r = Math.hypot(du, dv);
                                double rr = dist.fRad(r);
                                double xx = uc + rr * du / r;
                                double yy = vc + rr * dv / r;
                                float[] val = sourceAcc.getPix(xx, yy);
                                targetAcc.setPix(u, v, val);
                            }
                        }
                    }
                });
            }
        } finally {
            // Initiates an orderly shutdown
            executor.shutdown();
            try {
                // Wait for all rows to finish processing (adjust timeout as needed)
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
        gd.addCheckbox("parallel", parallel);

        gd.showDialog();
        if (gd.wasCanceled())
            return false;

        focalLength = gd.getNextNumber();
        sensorWidth = gd.getNextNumber();
        sensorHeight = gd.getNextNumber();

        a = gd.getNextNumber();
        b = gd.getNextNumber();
        c = gd.getNextNumber();
        parallel = gd.getNextBoolean();
        distParams = new double[] {a, b, c};
        return true;
    }

}
