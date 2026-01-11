package com.imagingbook.aruco.data;

import imagingbook.core.resource.ImageResource;

/**
 * Calibration images taken with a Nikon Z6 and a Yashica ML 55mm f/4 Macro lens using
 * a Charuco model board (size 12x8, square=22mm, marker=16mm, A4L, DICT_5x5_1000).
 */
public enum Z6_Yashica_55mm_DICT5x5_CharucoBoard_12x8 implements ImageResource {
    DSC_2691g,
    DSC_2692g,
    DSC_2693g,
    DSC_2694g,
    DSC_2696g,
    DSC_2698g,
    DSC_2699g,
    DSC_2700g,
    DSC_2701g,
    DSC_2702g,
    DSC_2704g,
    DSC_2705g,
    DSC_2706g,
    DSC_2707g,
    DSC_2708g,
    DSC_2709g,
    DSC_2710g,
    DSC_2711g,
    DSC_2712g,
    DSC_2713g,
    DSC_2715g;

    // all files are jpg's
    public String getDefaultExtension() {
        return "jpg";
    }
}


