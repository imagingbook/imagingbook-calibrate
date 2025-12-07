/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

/**
 * Dictionary specs from
 * "/opencv/modules/objdetect/src/aruco/aruco_dictionary.cpp",
 * "/opencv/modules/objdetect/include/opencv2/objdetect/aruco_dictionary.hpp"
 *
 *  JSON byte data derived from
 * "/opencv/modules/objdetect/src/aruco/predefined_dictionaries.hpp"
 * "/opencv/modules/objdetect/src/aruco/apriltag/predefined_dictionaries_apriltag.hpp"
 */

public enum ArucoPredefiedDictionary {
    DICT_ARUCO_ORIGINAL,

    DICT_4X4_50,
    DICT_4X4_100,
    DICT_4X4_250,
    DICT_4X4_1000,

    DICT_5X5_50,
    DICT_5X5_100,
    DICT_5X5_250,
    DICT_5X5_1000,

    DICT_6X6_50,
    DICT_6X6_100,
    DICT_6X6_250,
    DICT_6X6_1000,

    DICT_7X7_50,
    DICT_7X7_100,
    DICT_7X7_250,
    DICT_7X7_1000,

    DICT_APRILTAG_16h5,
    DICT_APRILTAG_25h9,
    DICT_APRILTAG_36h10,
    DICT_APRILTAG_36h11;

    static final String RELATIVE_DIR = "dict-gz/";
    static final String FILE_EXTENSION = ".json.gz";

    private ArucoDictionary dict = null;    // singleton instance, only loaded once

    public boolean isLoaded() {
        return (dict != null);
    }

    // lazy evaluation: data don't get loaded unless needed:
    public ArucoDictionary getDictionary() {
        if (!isLoaded()) {   // dictionary not yet initialized
            String resourcePath = RELATIVE_DIR + this.name() + FILE_EXTENSION;
            // System.out.println("Loading dictionary from " + resourcePath);
            dict = ArucoDictionary.fromResource(this.getClass(), resourcePath);
        }
        return dict;
    }

    // --------------------------------------------------

    public static void main(String[] args) {
        // open/load all predefined dictionaries:
        for (ArucoPredefiedDictionary dictname : ArucoPredefiedDictionary.values()) {
            ArucoDictionary dict = dictname.getDictionary();
            System.out.println(dictname + ": " + dict);

            // not loaded twice, same instance:
            ArucoDictionary dict2 = dictname.getDictionary();
            assert(dict == dict2);
        }
        System.out.println();

    }

}
