/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize;

import imagingbook.common.util.bits.BitVector;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class SubsequenceMapTest {

    @Test
    public void ArrayIndexMapperTest1() {
        BitVector subset = BitVector.from("1110011110");
        SubsequenceMap mapper = new SubsequenceMap(subset);
        System.out.println(Arrays.toString(mapper.origSeqIndex));
        System.out.println(Arrays.toString(mapper.subSeqIndex));
        assertArrayEquals(new int[] {0, 1, 2, -1, -1, 3, 4, 5, 6, -1}, mapper.origSeqIndex);
        assertArrayEquals(new int[] {0, 1, 2, 5, 6, 7, 8}, mapper.subSeqIndex);

        assertEquals(-1, mapper.getSubsequencePos(3));
        assertEquals(5, mapper.getSubsequencePos(7));
        assertEquals(8, mapper.getOriginalPos(6));

        for (int p = 0; p < subset.length(); p++) {
            if (subset.getBit(p)) {
                assertEquals(p, mapper.getOriginalPos(mapper.getSubsequencePos(p)));
            }
        }
    }


}