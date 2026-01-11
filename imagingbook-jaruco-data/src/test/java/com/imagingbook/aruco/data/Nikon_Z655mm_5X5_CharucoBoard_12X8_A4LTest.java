package com.imagingbook.aruco.data;

import imagingbook.testutils.ResourceTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Nikon_Z655mm_5X5_CharucoBoard_12X8_A4LTest {

    // @Test
    // void valuesTest() {
    //     for (var name : Z6_Yashica_55mm_DICT5x5_CharucoBoard_12x8.values()) {
    //         System.out.println("1 " + name);
    //         System.out.println("2 " + name.getFileName());
    //         System.out.println("3 " + name.getRelativePath());
    //         System.out.println("4 " + name.getURL());
    //         System.out.println();
    //     }
    // }

    @Test
    public void fullImageResourceTest() {
        int n = ResourceTestUtils.testImageResource(Z6_Yashica_55mm_DICT5x5_CharucoBoard_12x8.class);
        assertEquals(n, Z6_Yashica_55mm_DICT5x5_CharucoBoard_12x8.values().length);
    }
}