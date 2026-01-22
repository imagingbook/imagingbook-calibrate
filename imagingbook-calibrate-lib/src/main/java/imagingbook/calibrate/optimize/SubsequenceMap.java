/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.common.util.bits.BitVector;

import java.util.List;

/**
 * Stores the association between an original sequence (array) and a derived subsequence.
 * It provides information (a) where to find an original element in the subsequence and
 * (b) where a subsequence element originated in the original sequence.
 * Note that this class stores no data items but only indexes to map between the two
 * sequences.
 */
public class SubsequenceMap {
    final int[] origSeqIndex;
    final int[] subSeqIndex;

    /**
     * Constructor.
     * @param subset a {@link BitVector} indicating which vector elements to keep
     */
    public SubsequenceMap(BitVector subset) {
        this.origSeqIndex = new int[subset.length()];
        this.subSeqIndex = new int[subset.cardinality()];
        for (int i = 0, j = 0; i < origSeqIndex.length; i++) {
            if (subset.getBit(i)) {     // keep this element in reduced vector
                origSeqIndex[i] = j;       // i -> j (position reducedIndex)
                subSeqIndex[j] = i;    // j -> i (position in fullIndex)
                j++;
            }
            else {
                origSeqIndex[i] = -1;      // mark skipped items in fullIndex -1
            }
        }
    }

    @Deprecated
    public SubsequenceMap(int size, List<Integer> skipped) {
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
    public SubsequenceMap(int[] skipArray) {
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
     * Returns the subsequence position of the item located at {@code origPos} in the original
     * sequence.
     * @param origPos position in original sequence
     * @return position in the subsequence if contained, -1 otherwise
     */
    int getSubsequencePos(int origPos) {
        return origSeqIndex[origPos];
    }

    /**
     * Returns the original position of the item located at  {@code subPos} in the subsequence.
     * @param subPos position in the subsequence
     * @return position in original sequence
     */
    int getOriginalPos(int subPos) {
        return subSeqIndex[subPos];
    }

    /**
     * Returns the original sequence length this {@link SubsequenceMap} is set up for.
     * @return the length of original vectors
     */
    public int getOriginalLength() {
        return origSeqIndex.length;
    }

    /**
     * Returns the subsequence length this {@link SubsequenceMap} is set up for.
     * @return the subsequence length
     */
    public int getSubsequenceLength() {
        return subSeqIndex.length;
    }
}
