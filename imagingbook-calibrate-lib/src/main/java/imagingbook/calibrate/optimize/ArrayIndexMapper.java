/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import java.util.ArrayList;
import java.util.List;

class ArrayIndexMapper {
    final int[] fullIndex;
    final int[] reducedIndex;

    public ArrayIndexMapper(int size, List<Integer> skipped) {
        this.fullIndex = makeFullIndex(size, skipped);
        this.reducedIndex = makeReducedIndex(fullIndex);

        for (int i = 0, j = 0; i < fullIndex.length; i++) {
            if (fullIndex[i] != -1) {
                fullIndex[i] = j;       // i -> j (position reducedIndex)
                reducedIndex[j] = i;    // j -> i (position in fullIndex)
                j++;
            }
        }
    }

    public ArrayIndexMapper(int[] skipArray) {
        this(skipArray.length, collectSkipped(skipArray));
    }

    /**
     * Returns the reduced position of parameter located at  {@code p} in the original (full)
     * vector.
     * @param p position in original vector
     * @return position in reduced vector or -1 if {@code p} is skipped
     */
    int getReducedPos(int p) {
        return fullIndex[p];
    }

    /**
     * Returns the original (full) position of parameter located at  {@code q} in the reduced
     * vector.
     * @param q position in reduced vector
     * @return position in full vector
     */
    int getFullPos(int q) {
        return reducedIndex[q];
    }


    // double[] getReducedParams(double[] fullParams) {
    //     double[] rp = new double[this.getReducedLength()];
    //     // fill rp:
    //     for (int q = 0; q < rp.length; q++) {
    //         int p = this.getFullPos(q);
    //         rp[q] = fullParams[p];
    //     }
    //     return rp;
    // }
    //
    // double[] getfullParams(double[] fullParams, double[] rp) {
    //     double[] fp = fullParams.clone();
    //     // insert from reduced parameters:
    //     for (int q = 0; q < rp.length; q++) {
    //         int j = this.getFullPos(q);
    //         fp[j] = rp[q];
    //     }
    //     return fp;
    // }

    // -----------------------------------------------------------------------------------



    static List<Integer> collectSkipped(int[] skippedArray) {
        List<Integer> skipped = new ArrayList<>();
        for (int i = 0; i < skippedArray.length; i++) {
            if (skippedArray[i] == -1) {
                skipped.add(i);
            }
        }
        return skipped;
    }

    private static int[] makeFullIndex(int size, List<Integer> skipped) {
        int[] idx = new int[size];
        for (int c : skipped) {
            idx[c] = -1;
        }
        return idx;
    }

    private static int[] makeReducedIndex(int[] fullIdx) {
        int n = 0;
        for (int c : fullIdx) { // count non-skipped
            if (c != -1) {
                n++;
            }
        }
        return new int[n];
    }

    public int getFullLength() {
        return fullIndex.length;
    }

    public int getReducedLength() {
        return reducedIndex.length;
    }
}
