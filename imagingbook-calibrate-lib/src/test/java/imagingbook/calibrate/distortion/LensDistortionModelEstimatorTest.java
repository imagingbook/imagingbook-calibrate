/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.distortion;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.zhang.data.ZhangData;
import imagingbook.common.geometry.basic.Pnt2d;

import java.util.ArrayList;
import java.util.List;

public class LensDistortionModelEstimatorTest {

    static Pnt2d[] modelPts = ZhangData.getModelPoints();
    static Pnt2d[][] obsPts = ZhangData.getAllObservedPoints();
    static ViewTransform[] views = ZhangData.getAllViewTransforms();   // cheating a bit, using final views from Zhang
    // initial estimate obtained from Calibration
    static double[] camIntrinsics = {877.16, 876.80, 0.1751, 301.04, 220.41};

    static List<Pnt2d[]> modelPtsList = new ArrayList<>();
    static List<Pnt2d[]> obsPtsList = new ArrayList<>();
    static {
        for (int i = 0; i < obsPts.length; i++) {
            modelPtsList.add(modelPts);
            obsPtsList.add(obsPts[i]);
        }
    }

    // @Test
    // public void getEstimateRadial2TermTest() {
    //     StandardCamera cam1 = new StandardCamera(camIntrinsics, Radial2TermDistortion.INSTANCE);
    //     DistortionModel dist = DistortionModel.from(cam1, views, modelPtsList, obsPtsList);
    //     assertNotNull(dist);
    //     // System.out.println(Arrays.toString(dist.getParameters()));
    //     assertArrayEquals(new double[] {-1.6129, 6.5133}, dist.getParameters(), 1e-3);
    // }

    // @Test
    // public void getEstimateRadial3TermTest() {
    //     StandardCamera cam1 = new StandardCamera(camIntrinsics, Radial3TermDistortion.INSTANCE);
    //     DistortionModel dist = DistortionModel.from(cam1, views, modelPtsList, obsPtsList);
    //     assertNotNull(dist);
    //     // System.out.println(Arrays.toString(dist.getParameters()));
    //     assertArrayEquals(new double[] {-2.1701, 18.4892, -57.64063}, dist.getParameters(), 1e-3);
    // }

    // @Test
    // public void getDistortion() {
    // }
    //
    // @Test
    // public void getError() {
    // }
    //
    // @Test
    // public void testGetError() {
    // }
}