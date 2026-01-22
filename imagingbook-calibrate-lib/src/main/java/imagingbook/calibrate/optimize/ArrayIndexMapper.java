/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.common.util.bits.BitVector;

import java.util.ArrayList;
import java.util.List;

class ArrayIndexMapper {
    final int[] fullIndex;
    final int[] reducedIndex;

    /**
     * Constructor.
     * @param subset a {@link BitVector} indicating which vector elements to keep
     */
    public ArrayIndexMapper(BitVector subset) {
        this.fullIndex = new int[subset.length()];
        this.reducedIndex = new int[subset.cardinality()];
        for (int i = 0, j = 0; i < fullIndex.length; i++) {
            if (subset.getBit(i)) {     // keep this element in reduced vector
                fullIndex[i] = j;       // i -> j (position reducedIndex)
                reducedIndex[j] = i;    // j -> i (position in fullIndex)
                j++;
            }
            else {
                fullIndex[i] = -1;      // mark skipped items in fullIndex -1
            }
        }
    }

    @Deprecated
    public ArrayIndexMapper(int size, List<Integer> skipped) {
        this(makeBitVector(size, skipped));
    }

    @Deprecated
    static BitVector makeBitVector(int size, List<Integer> skipped) {
        System.out.println("skipped = " + skipped);
        BitVector subset = new BitVector(size);
        subset.setAll();
        for (int i : skipped) {
            subset.unsetBit(i);
        }
        System.out.println("subset = " + subset);
        return subset;
    }

    @Deprecated
    public ArrayIndexMapper(int[] skipArray) {
        this(collectSkipped(skipArray));
    }

    @Deprecated
    static BitVector collectSkipped(int[] skippedArray) {
        BitVector subset = new BitVector(skippedArray.length);
        for (int i = 0; i < skippedArray.length; i++) {
            if (skippedArray[i] != -1) {
                subset.setBit(i);
            }
        }
        return subset;
    }

    // ------------------------------------------------------------------------------------------

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
