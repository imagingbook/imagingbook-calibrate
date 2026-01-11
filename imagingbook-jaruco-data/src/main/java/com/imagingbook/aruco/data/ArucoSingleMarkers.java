package com.imagingbook.aruco.data;

import imagingbook.core.resource.ImageResource;

/**
 * Photographs of a single ArUco marker (No 5) from the DICT_5x5_1000 dictionary in four rotated versions
 * plus two artificially distorted versions (barres and pincushion).
 */
public enum ArucoSingleMarkers implements ImageResource {
    marker_5_0_jpg,
    marker_5_1_jpg,
    marker_5_2_jpg,
    marker_5_3_jpg,
    marker_5_0_distortedA_png,
    marker_5_0_distortedB_png;
}
