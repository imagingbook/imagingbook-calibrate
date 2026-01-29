/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import org.apache.commons.math4.legacy.linear.RealMatrix;

@Deprecated
public class CameraFactory {

    // private final Camera.Type type;
    private final int imgWidth, imgHeight;

    public CameraFactory(int imgWidth, int imgHeight) {
        // this.type = type;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }

    public Camera createCamera(Camera.Type camType, RealMatrix[] homographies) {
        Camera initCam = switch (camType) {
            case SimpleCamera -> SimpleCamera.fromHomographies(homographies, imgWidth, imgHeight);
            case StandardCamera ->  StandardCamera.fromHomographies(homographies, imgWidth, imgHeight);
        };
        return initCam;
    }
}
