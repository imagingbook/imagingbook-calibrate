package imagingbook.jaruco;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static imagingbook.jaruco.ArucoDictionary.normHamming;
import static imagingbook.jaruco.ArucoDictionary.toBitSet;
import static imagingbook.jaruco.Rotations.makeRotationPermutation;
import static org.junit.jupiter.api.Assertions.*;

class ArucoDictionaryTest {

    @Test
    void lookupTest1() {
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();
        BitSet pattern0 = toBitSet("1011010000010000001010111");
        ArucoDictionary.LookupResult result = dict.lookup(pattern0, 2);
        // System.out.println(result);
        assertNotNull(result);
        assertEquals(5, result.markerIndex);
        assertEquals(2, result.rotation);
        assertEquals(0, result.hammingDistance);
    }

    @Test
    void lookupTest2() {
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();
        int[] rotperm = makeRotationPermutation(dict.getMarkerSize());
        char[][] patterns = new char[4][];
        patterns[0] = "1011010000010000001010111".toCharArray();
        patterns[1] =  Rotations.permute(patterns[0], rotperm);
        patterns[2] =  Rotations.permute(patterns[1], rotperm);
        patterns[3] =  Rotations.permute(patterns[2], rotperm);

        for (int r=0; r<4; r++) {
            ArucoDictionary.LookupResult result = dict.lookup(toBitSet(patterns[r]), 2);
            assertNotNull(result);
            // System.out.println(result);
            assertEquals(5, result.markerIndex);
            assertEquals(0, result.hammingDistance);
        }
    }

    @Test
    void normHammingTest0() {
        BitSet a = toBitSet("1011010000010000001010111");
        BitSet b = (BitSet) a.clone();
        assertEquals(0, normHamming(a, b));
        assertEquals(0, normHamming(b, a));
    }

    @Test
    void normHammingTest1() {
        BitSet a = toBitSet("1011010000010000001010111");
        BitSet b = toBitSet("1011010000010000001010110");
        assertEquals(1, normHamming(a, b));
        assertEquals(1, normHamming(b, a));
    }

    @Test
    void normHammingTest2() {
        BitSet a = toBitSet("0011010000010000001010111");
        BitSet b = toBitSet("1011010000010000001010110");
        assertEquals(2, normHamming(a, b));
        assertEquals(2, normHamming(b, a));
    }

    @Test
    void normHammingTest3() {
        BitSet a = toBitSet("0011010000010000001010111");
        BitSet b = toBitSet("0000000000000000000000000");
        assertEquals(9, normHamming(a, b));
        assertEquals(9, normHamming(b, a));
    }
}