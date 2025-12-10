/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco.obsolete;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Dictionaries are stored as a list of bytes in its four rotations
 * On each rotation, the marker is divided in bytes assuming a row-major order
 * This format allows a faster marker identification.
 * For a dictionary composed by M markers of NxN bits, the structure dimensions should be:
 * const char name[nMarkers][4rotations][nBytes], or more specifically:
 * const char name[M][4][ceil(NxN/8)]
 * The element [i][j][k] represents the k-th byte of the i-th marker in the dictionary
 * in its j-th rotation.
 * Each rotation implies a 90 degree rotation of the marker in anticlockwise direction.
 */
@Deprecated
public class DictionaryReader {

    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_4X4_1000_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_5X5_1000_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_6X6_1000_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_7X7_1000_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_APRILTAG_16h5_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_APRILTAG_25h9_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_APRILTAG_36h10_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_APRILTAG_36h11_BYTES.json";
    // static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_ARUCO_BYTES.json";
    static String filePath = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook_calibrate_plugins/aruco-dicts/DICT_ARUCO_MIP_36h12_BYTES.json";

    public static void main(String[] args) throws IOException {

        File jsonfile = new File(filePath);
        System.out.println("abs = " + jsonfile.getAbsolutePath());
        System.out.println("exists = " + jsonfile.exists());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        // This will correctly read the JSON and perform the two's complement conversion
        byte[][][] DICT_5X5_1000_BYTES = mapper.readValue(jsonfile, byte[][][].class);

        int M = DICT_5X5_1000_BYTES.length;
        int N = DICT_5X5_1000_BYTES[0].length;
        int P = DICT_5X5_1000_BYTES[0][0].length;
        System.out.printf("M=%d, N=%d, P=%d\n", M, N, P);

        for (int i = 0; i < P; i++) {
            int x = 0xFF & DICT_5X5_1000_BYTES[0][0][i];
            System.out.print(x + ", ");
        }
        System.out.println();


    }
}
