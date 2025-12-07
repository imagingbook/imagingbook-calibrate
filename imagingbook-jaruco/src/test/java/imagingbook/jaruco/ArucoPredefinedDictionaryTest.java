package imagingbook.jaruco;

import org.junit.jupiter.api.Test;  // JUnit5 !!

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import static imagingbook.jaruco.ByteArrayUtils.countValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ArucoPredefinedDictionaryTest {

    @Test   // JUnit5
    public void getDictionaryTest1() {
        // open/load all predefined dictionaries:
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            // assertFalse(dictname.isLoaded());    // this is not guaranteed

            ArucoDictionary dict = dictname.getDictionary();
            assertNotNull(dict,"could not load dictionary: " + dictname);

            // not loaded twice, same instance:
            ArucoDictionary dict2 = dictname.getDictionary();
            assertSame(dict2, dict);
        }
    }

    @Test   // JUnit5
    public void dictionaryIntegrityTest1() {
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            final ArucoDictionary dict = dictname.getDictionary();
            int M = dict.getNumberOfCodes();
            int N = dict.getMarkerSize();
            int NxN = N * N;
            // check if all marker patterns exist and have the proper length
            for (int id = 0; id < M; id++) {
                for (int r = 0; r < 4; r++) {
                    byte[] pattern = dict.getMarkerPattern(id, r);
                    final int finalId = id, finalR = r;
                    assertEquals(NxN, pattern.length,
                            () -> String.format("%s: wrong pattern length for marker id=%d, r=%d", dictname, finalId, finalR));
                }
            }
        }
    }

    @Test   // JUnit5
    public void dictionaryIntegrityTest2() {
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            final ArucoDictionary dict = dictname.getDictionary();
            int M = dict.getNumberOfCodes();
            int N = dict.getMarkerSize();
            int NxN = N * N;
            // check if all rotated marker patterns have the same number of 0/1
            for (int id = 0; id < M; id++) {
                // reference pattern:
                byte[] canonical = dict.getMarkerPattern(id, 0);
                int zeros = countValues(canonical, (byte) 0);
                int ones = countValues(canonical, (byte) 1);
                assertEquals(NxN, zeros + ones);
                // check rotated versions:
                for (int r = 1; r < 4; r++) {
                    byte[] pattern = dict.getMarkerPattern(id, r);
                    assertEquals(zeros, countValues(pattern, (byte) 0));
                    assertEquals(ones, countValues(pattern, (byte) 1));
                }
            }
        }
    }


    @Test   // JUnit5
    public void dictionaryIntegrityTest3() {    // checking BitSets
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            final ArucoDictionary dict = dictname.getDictionary();
            int M = dict.getNumberOfCodes();
            int N = dict.getMarkerSize();
            int NxN = N * N;
            // check if all rotated marker patterns have the same number of 1s
            for (int id = 0; id < M; id++) {
                // reference pattern:
                BitSet canonical = dict.getBitSet(id, 0);
                int ones = canonical.cardinality();
                // check rotated versions:
                for (int r = 1; r < 4; r++) {
                    BitSet pattern = dict.getBitSet(id, r);
                    assertEquals(ones, pattern.cardinality());
                }
            }
        }
    }


    @Test   // JUnit5
    public void dictionaryCheckDuplicates() {    // checking duplicate pattern entries
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            if (dictname == ArucoPredefinedDictionary.DICT_ARUCO_ORIGINAL)
                continue;
            final ArucoDictionary dict = dictname.getDictionary();
            int M = dict.getNumberOfCodes();
            int N = dict.getMarkerSize();
            int NxN = N * N;
            Set<BitSet> allMarkers = new HashSet<>();
            // we incrementally add all marker patterns to a set and check if not already contained:
            for (int id = 0; id < M; id++) {
            // for (int id = M-1; id >= 0; id--) {
                BitSet canonical = dict.getBitSet(id, 0);
                assertFalse(allMarkers.contains(canonical),  "problem canonical id=" + id + " in dictionary " + dictname);
                allMarkers.add(canonical);
                for (int r = 1; r < 4; r++) {
                    BitSet pattern = dict.getBitSet(id, r);
                    assertFalse(allMarkers.contains(pattern),  "problem rotated id=" + id + " r=" + r + " in dictionary " + dictname);
                    allMarkers.add(pattern);
                }
            }
            assertEquals(M * 4, allMarkers.size(), "overall size problem in dictionary " + dictname);
        }
    }

    // @Test   // JUnit5
    // public void dictionaryCheckDuplicatesFull() {    // checking duplicate pattern entries exhaustively!
    //     for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
    //         System.out.println("checking dict " + dictname);
    //         final ArucoDictionary dict = dictname.getDictionary();
    //         int M = dict.getNumberOfCodes();
    //         int N = dict.getMarkerSize();
    //         int NxN = N * N;
    //
    //         for (int id = 0; id < M; id++) {
    //             for (int r = 0; r < 4; r++) {
    //                 BitSet reference = dict.getBitSet(id, r);
    //
    //                 // check against all other id's in this dictionary:
    //                 for  (int i = 0; i < M; i++) {
    //                     if (i == id) continue;
    //                     for (int s = 0; s < 4; s++) {
    //                         BitSet pattern = dict.getBitSet(i, s);
    //                         assertNotEquals(reference, pattern, "problem rotated id=" + id + " r=" + r + " in dictionary " + dictname);
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }

    @Test   // JUnit5
    public void dictionaryCheckRotatedPatterns() {    // check if rotated markers are unique
        for (ArucoPredefinedDictionary dictname : ArucoPredefinedDictionary.values()) {
            if (dictname == ArucoPredefinedDictionary.DICT_ARUCO_ORIGINAL)
                continue;
            // System.out.println("checking dict " + dictname);
            final ArucoDictionary dict = dictname.getDictionary();
            int M = dict.getNumberOfCodes();
            int N = dict.getMarkerSize();
            int NxN = N * N;

            for (int id = 0; id < M; id++) {
                Set<BitSet> allMarkers = new HashSet<>();
                allMarkers.add(dict.getBitSet(id, 0));
                for (int r = 1; r < 4; r++) {
                    BitSet pattern = dict.getBitSet(id, r);
                    int finalId = id;
                    int finalR = r;
                    assertFalse(allMarkers.contains(pattern), // "problem rotated id=" + id + " r=" + r + " in dictionary " + dictname);
                            () -> String.format("%s: duplicate rotate pattern for id=%d, r=%d", dictname, finalId, finalR));
                }
            }
        }
    }



}