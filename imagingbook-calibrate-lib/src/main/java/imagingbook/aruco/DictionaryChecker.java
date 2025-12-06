/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

import static imagingbook.aruco.Dictionary.toStringUnsigned;

public class DictionaryChecker {

    /**
     * Check dictionaries if all rotated patterns match the canonical pattern.
     * @param args
     */
    public static void main(String[] args) {
        // for (PredefiedDictionary pd : PredefiedDictionary.values()) {
        {
            //PredefiedDictionary pd = PredefiedDictionary.DICT_4X4_1000;
            //PredefiedDictionary pd = PredefiedDictionary.DICT_5X5_1000;
            //PredefiedDictionary pd = PredefiedDictionary.DICT_6X6_1000;
            //PredefiedDictionary pd = PredefiedDictionary.DICT_7X7_1000;
            //PredefiedDictionary pd = PredefiedDictionary.DICT_APRILTAG_16h5;
            PredefiedDictionary pd = PredefiedDictionary.DICT_APRILTAG_25h9;
            // check all others!

            System.out.println("Dict = " + pd.name());
            Dictionary dict = pd.getDict();
            System.out.println("Size = " + dict.getMarkerSize());
            System.out.println("Marker bits = " + dict.getMarkerSize() * dict.getMarkerSize());
            System.out.println("  codes = " + dict.getNumberOfCodes());

            int[] errCnt = new int[4]; // one for each rotation r

            for (int code = 0; code < dict.getNumberOfCodes(); code++) {  // code < d.getNumberOfCodes()
                System.out.println("-------------- Code = " + code);

                byte[] br0 = dict.getCodeBytes(code, 0);
                ByteProcessor ip = dict.bytesToImage(br0); // canonical image

                for (int r = 0; r < 4; r++) {
                    byte[] bytes1 = dict.getCodeBytes(code, r);
                    System.out.printf("Code %d / Rotation %d\n", code, r);
                    System.out.println("br1= " + toStringUnsigned(bytes1));
                    String str = dict.markerAsString1D(bytes1);
                    //System.out.println("br1= " + d.markerAsString1D(br1));
                    // System.out.println();
                    // System.out.println(d.markerAsString2D(br));
                    // System.out.println();

                    // ip.invertLut();
                    //new ImagePlus("Rotation r = " + r, ip.resize(50)).show();
                    byte[] bytes2 = dict.imageToBytes(ip);
                    System.out.println("br2= " + toStringUnsigned(bytes2));
                    //System.out.println("br2= " + d.markerAsString1D(br2));
                    //System.out.println(d.markerAsString2D(br2));

                    if (!Arrays.equals(bytes1, bytes2)) {
                        errCnt[r]++;
                        System.out.println("**** error! ****");
                    }
                    ip = (ByteProcessor) ip.rotateLeft();
                }
            }
            System.out.println("\nErrors found: " + Arrays.toString(errCnt));
            System.out.println("Errors total: " + (errCnt[0] + errCnt[1] + errCnt[2] + errCnt[3]));
        }
    }
}
