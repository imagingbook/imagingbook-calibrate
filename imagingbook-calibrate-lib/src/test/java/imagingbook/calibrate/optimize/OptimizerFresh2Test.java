/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import org.junit.Test;

import java.util.Arrays;

// import static imagingbook.calibrate.optimize.OptimizerFresh2.countEffParameters;
// import static imagingbook.calibrate.optimize.OptimizerFresh2.makeParamIndex;
import static org.junit.Assert.*;

public class OptimizerFresh2Test {

    @Test
    public void ArrayIndexMapperTest1() {
        int[] skipArray = { 0 , 0 , 0 , -1 , -1 , 0 , 0 , 0 , 0 , -1 };
        ArrayIndexMapper mapper = new ArrayIndexMapper(10, Arrays.asList(3, 4, 9));
        // System.out.println(Arrays.toString(mapper.fullIndex));
        // System.out.println(Arrays.toString(mapper.reducedIndex));
        assertArrayEquals(new int[] {0, 1, 2, -1, -1, 3, 4, 5, 6, -1}, mapper.fullIndex);
        assertArrayEquals(new int[] {0, 1, 2, 5, 6, 7, 8}, mapper.reducedIndex);

        assertEquals(-1, mapper.getReducedPos(3));
        assertEquals(5, mapper.getReducedPos(7));
        assertEquals(8, mapper.getFullPos(6));

        for (int p = 0; p < skipArray.length; p++) {
            if (skipArray[p] != -1) {
                assertEquals(p, mapper.getFullPos(mapper.getReducedPos(p)));
            }
        }
    }

    @Test
    public void ArrayIndexMapperTest2() {
        int[] skipArray = { 0 , 0 , 0 , -1 , -1 , 0 , 0 , 0 , 0 , -1 };
        ArrayIndexMapper mapper = new ArrayIndexMapper(skipArray);
        // System.out.println(Arrays.toString(mapper.fullIndex));
        // System.out.println(Arrays.toString(mapper.reducedIndex));
        assertArrayEquals(new int[] {0, 1, 2, -1, -1, 3, 4, 5, 6, -1}, mapper.fullIndex);
        assertArrayEquals(new int[] {0, 1, 2, 5, 6, 7, 8}, mapper.reducedIndex);

        assertEquals(-1, mapper.getReducedPos(3));
        assertEquals(5, mapper.getReducedPos(7));
        assertEquals(8, mapper.getFullPos(6));

        for (int p = 0; p < skipArray.length; p++) {
            if (skipArray[p] != -1) {
                assertEquals(p, mapper.getFullPos(mapper.getReducedPos(p)));
            }
        }
    }



    // @Test
    // public void makeParamIndexTest() {
    //     // int[] skipArray = { false, false, false, true, true, false, false, false, false, true };
    //     int[] skipArray = { 0 , 0 , 0 , -1 , -1 , 0 , 0 , 0 , 0 , -1 };
    //     int n = countEffParameters(skipArray);
    //     int[] index = makeParamIndex(skipArray);
    //     // check length of index array
    //     assertEquals(n, index.length);
    //
    //     // System.out.println(Arrays.toString(skipArray));
    //     // System.out.println(Arrays.toString(index));
    //
    //     // check if all index q's are non-skipped
    //     for (int q = 0; q < index.length; q++) {
    //         int p = index[q];
    //         // p contained in index only if skipArray[p] is false
    //         assertFalse(skipArray[p] < 0);
    //     }
    //
    //     // check if all non-skipped p's are contained in index:
    //     for (int p = 0; p < index.length; p++) {
    //         if (skipArray[p] < 0) {
    //             assertFalse(contains(index, p));
    //         }
    //         else {
    //             assertTrue(contains(index, p));
    //         }
    //     }
    //
    //     // check if index is sorted:
    //     assertTrue(isStrictlySorted(index));
    // }

    private static boolean contains(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return true;
            }
        }
        return false;
    }
    static boolean isStrictlySorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] >= a[i]) {
                return false;
            }
        }
        return true;
    }

    // @Test
    // public void countEffParametersTest() {
    //     boolean[] sa7 = { false, false, false, true, true, false, false, false, false, true };
    //     assertEquals(7, countEffParameters(sa7));
    //
    //     boolean[] sa8 = { false, false, false, false, false, false, false , false };
    //     assertEquals(8, countEffParameters(sa8));
    //
    //     boolean[] sa3 = { true, true, false, false, false, true, true, true };
    //     assertEquals(3, countEffParameters(sa3));
    //
    //     boolean[] sa1 = { true, true, false };
    //     assertEquals(1, countEffParameters(sa1));
    //
    //     boolean[] sa0 = { true, true, true };
    //     assertEquals(0, countEffParameters(sa0));
    // }

    // @Test
    // public void getReducedFullParamsTest() {
    //     double[] fullParams = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    //     // boolean[] skipArray = { true, false, false, true, true, false, false, false, false };
    //     int[] skipArray = { -1,  0,  0,  -1,  -1,  0,  0,  0,  0 };
    //     int[] index = makeParamIndex(skipArray);
    //
    //     // System.out.println(Arrays.toString(index));
    //     assertArrayEquals(new int[] {1, 2, 5, 6, 7, 8}, index);
    //
    //     double[] redParams = getReducedParams(fullParams, index);
    //     // System.out.println(Arrays.toString(redParams));
    //     assertArrayEquals(new double[] {2.0, 3.0, 6.0, 7.0, 8.0, 9.0}, redParams, 1e-6);
    //
    //     double[] fullParams2 = getfullParams(fullParams, redParams, index);
    //     assertArrayEquals(fullParams, fullParams2, 1e-6);
    //
    //     double[] redParams3 = Matrix.multiply(-1.0, redParams);
    //     double[] fullParams3 = getfullParams(fullParams, redParams3, index);
    //     // System.out.println(Arrays.toString(fullParams3));
    //     assertArrayEquals(new double[] {1.0, -2.0, -3.0, 4.0, 5.0, -6.0, -7.0, -8.0, -9.0}, fullParams3, 1e-6);
    // }

}