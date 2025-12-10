package imagingbook.jaruco;

import imagingbook.common.util.bits.BitVector;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.ArucoDictionary.toBitVector;
import static imagingbook.jaruco.RotationUtils.makeRotationPermutation;
import static org.junit.jupiter.api.Assertions.*;

class ArucoDictionaryTest {

    @Test
    void lookupTest1() {
        ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_1000.getInstance();
        BitVector pattern0 = toBitVector("1011010000010000001010111");
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
        patterns[1] =  RotationUtils.permute(patterns[0], rotperm);
        patterns[2] =  RotationUtils.permute(patterns[1], rotperm);
        patterns[3] =  RotationUtils.permute(patterns[2], rotperm);

        for (int r=0; r<4; r++) {
            ArucoDictionary.LookupResult result = dict.lookup(toBitVector(patterns[r]), 2);
            assertNotNull(result);
            // System.out.println(result);
            assertEquals(5, result.markerIndex);
            assertEquals(0, result.hammingDistance);
        }
    }
}