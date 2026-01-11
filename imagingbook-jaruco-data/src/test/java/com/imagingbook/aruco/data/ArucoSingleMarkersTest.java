package com.imagingbook.aruco.data;

import imagingbook.testutils.ResourceTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArucoSingleMarkersTest {

    @Test
    public void fullImageResourceTest() {
        int n = ResourceTestUtils.testImageResource(ArucoSingleMarkers.class);
        assertEquals(n, ArucoSingleMarkers.values().length);
    }

}