/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import imagingbook.common.math.Matrix;

import java.util.Arrays;
import java.util.List;

/**
 * Assembles and disassembles parameter vectors for the non-linear optimizer
 * (see {@link OverallOptimizer}).
 */
class ParameterVectorBuilder {

    private final Camera cam;
    private final int viewCount;
    private final int camLinParamCount;
    private final int camDistParamCount;
    private final int viewParamCount = ViewTransform.PARAMETER_COUNT;
    private final int totalParamCnt;

    ParameterVectorBuilder(Camera cam, int viewCount) {
        this.cam = cam;
        this.viewCount = viewCount;
        this.camLinParamCount = cam.getLinParameterCount();
        this.camDistParamCount = cam.getDistParameterCount();
        this.totalParamCnt = camLinParamCount + camDistParamCount + viewCount * viewParamCount;
    }

    // ------------------------------------------------------------------

    double[] getParameters(Camera cam, List<ViewTransform> views) {
        if (viewCount != views.size()) {
            throw new IllegalArgumentException("view count does not match list size: " + views.size());
        }
        double[] params = new double[totalParamCnt];
        double[] cp = cam.getParameters();
        System.arraycopy(cp, 0, params, 0, cp.length);
        // insert M view parameters:
        int start = cp.length;
        for (ViewTransform V : views) {
            double[] w = V.getParameters();
            System.arraycopy(w, 0, params, start, w.length);
            start = start + w.length;
        }
        return params;
    }

    int getCameraParamCount() {
        return camLinParamCount + camDistParamCount;
    }

    double[] getLinearCameraParameters(double[] params) {
        return Arrays.copyOfRange(params, 0, camLinParamCount);
    }

    double[] getDistortionParameters(double[] params) {
        return Arrays.copyOfRange(params, camLinParamCount, camLinParamCount + camDistParamCount);
    }

    double[] getCameraParameters(double[] parameters) {
        return Matrix.join(getLinearCameraParameters(parameters),
                getDistortionParameters(parameters));
    }

    double[] getViewParameters(double[] parameters, int k) {
        int startPos = getViewParameterPos(k, 0);
        return Arrays.copyOfRange(parameters, startPos, startPos + viewParamCount);
    }

    int getViewParameterPos(int k, int i) {
        return camLinParamCount + camDistParamCount + k * viewParamCount + i;
    }
}
