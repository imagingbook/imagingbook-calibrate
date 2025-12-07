/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Dictionary specs from
 * "/opencv/modules/objdetect/src/aruco/aruco_dictionary.cpp",
 * "/opencv/modules/objdetect/include/opencv2/objdetect/aruco_dictionary.hpp"
 *
 *  JSON byte data derived from
 * "/opencv/modules/objdetect/src/aruco/predefined_dictionaries.hpp"
 * "/opencv/modules/objdetect/src/aruco/apriltag/predefined_dictionaries_apriltag.hpp"
 */
@Deprecated
public enum PredefiedDictionary {
    DICT_ARUCO_ORIGINAL("DICT_ARUCO_BYTES", 1024, 5, 0),

    DICT_4X4_50("DICT_4X4_1000_BYTES", 50, 4, 0),
    DICT_4X4_100("DICT_4X4_1000_BYTES", 100, 4, 1),
    DICT_4X4_250("DICT_4X4_1000_BYTES", 250, 4, 1),
    DICT_4X4_1000("DICT_4X4_1000_BYTES", 1000, 4, 0),

    DICT_5X5_50("DICT_5X5_1000_BYTES", 50, 5, 3),
    DICT_5X5_100("DICT_5X5_1000_BYTES", 100, 5, 3),
    DICT_5X5_250("DICT_5X5_1000_BYTES", 250, 5, 2),
    DICT_5X5_1000("DICT_5X5_1000_BYTES", 1000, 5, 2),

    DICT_6X6_50("DICT_6X6_1000_BYTES", 50, 6, 6),
    DICT_6X6_100("DICT_6X6_1000_BYTES", 100, 6, 5),
    DICT_6X6_250("DICT_6X6_1000_BYTES", 250, 6, 5),
    DICT_6X6_1000("DICT_6X6_1000_BYTES", 1000, 6, 4),

    DICT_7X7_50("DICT_7X7_1000_BYTES", 50, 7, 9),
    DICT_7X7_100("DICT_7X7_1000_BYTES", 100, 7, 8),
    DICT_7X7_250("DICT_7X7_1000_BYTES", 250, 7, 8),
    DICT_7X7_1000("DICT_7X7_1000_BYTES", 1000, 7, 6),

    DICT_APRILTAG_16h5("DICT_APRILTAG_16h5_BYTES", 30, 4, 0),
    DICT_APRILTAG_25h9("DICT_APRILTAG_25h9_BYTES", 35, 5, 0),
    DICT_APRILTAG_36h10("DICT_APRILTAG_36h10_BYTES", 2320, 6, 0),
    DICT_APRILTAG_36h11("DICT_APRILTAG_36h11_BYTES", 587,  6, 0),

    DICT_ARUCO_MIP_36h12("DICT_ARUCO_MIP_36h12_BYTES", 250, 6, 12);

    static final String FILE_EXTENSION = ".json";

    private final String resourceName;
    private final Dictionary dict;

    /**
     * Constructor.
     * @param resourceName the name of the associated JSON file (without extension)
     * @param M the number of dictionary codes
     * @param N the number of marker bit-fields in X/Y (marker size is N x N)
     * @param maxCorrectionBits the max. number of correction bits
     */
    PredefiedDictionary(String resourceName, int M, int N, int maxCorrectionBits) {
        this.resourceName = resourceName;
        // we create a non-initialized directors so we don't have to remember its parameters:
        this.dict = new Dictionary(null, M, N, maxCorrectionBits);  // dict only partially initialized
    }

    // lazy evaluation: data don't get loaded unless needed:
    public Dictionary getDict() {
        if (dict.getByteData() == null) {   // data not yet initialized
            dict.setData(getDataFromResource(this.getClass(), resourceName + FILE_EXTENSION));
        }
        return dict;
    }

    /**
     * Reads dictionary byte data from a JSON resource file.
     * @param clazz the class at the root of the relative path
     * @param filename the name of the file (relative path to class)
     * @return the resulting 3-dimensional byte array (unsigned)
     */
    byte[][][] getDataFromResource(Class<?> clazz, String filename) {
        System.out.println("loading dictionary data from " + filename);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalStateException("Dictionary resource not found: " + filename);
        }
        try (is) {
            return mapper.readValue(is, byte[][][].class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
