/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.util.RotationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static imagingbook.jaruco.util.RotationUtils.makeRotationPermutation;

/**
 * Dictionaries are stored as a list of bytes in its four rotations
 * On each rotation, the marker is divided in bytes assuming a row-major order
 * This format allows a faster marker identification.
 * For a dictionary composed by M markers of NxN bits, the structure dimensions should be:
 * const char name[nMarkers][4rotations][nBytes], or more specifically:
 * const char name[M][4][ceil(NxN/8)]
 * The element [i][j][k] represents the k-th byte of the i-th marker in the dictionary
 * in its j-th rotation.
 * Each rotation implies a 90 degree rotation of the marker in anticlockwise direction.
 */

public class ArucoDictionary {
    static final int R = 4;                     // number of marker rotations
    private final int M;                        // number of marker codes
    private final int N;                        // number of bits per dimension
    private final int markerBitCount;           // number of bits for whole marker
    private final int maxCorrectionBits;        // max. number of correction bits
    private final BitVector[][] bitdata;        // marker bit patterns, bitdata[m][r] is a 0/1 bit-pattern for marker m, rotation r
    private final int[] rotperm;                // permutation vector for 2D matrix rotation of size NxN

    /**
     * Constructor
     * @param M number of code id's
     * @param N marker size
     * @param maxCorrectionBits number of correctable error bits
     * specifying the marker's canonical (unrotated) pattern
     */
    private ArucoDictionary(int M, int N, int maxCorrectionBits) {
        this.M = M;
        this.N = N;
        this.markerBitCount = N * N;
        this.maxCorrectionBits = maxCorrectionBits;
        this.bitdata = new BitVector[M][]; // not yet initialized, to be filled later
        this.rotperm = makeRotationPermutation(N);
    }

    // private ArucoDictionary(int M, int N, int maxCorrectionBits, String[] markerStrings) {
    //     this.M = M;
    //     this.N = N;
    //     this.maxCorrectionBits = maxCorrectionBits;
    //     this.bitdata = makeBitVectors(markerStrings);  // create canonical and rotated bit patterns
    //     this.rotperm = makeRotationPermutation(N);
    // }

    /**
     * Adds the bit vectors (canonical plus 3 rotated versions) for the specified
     * marker. Throws an exception if this marker was already initialized.
     * Converts the 0/1 marker string to an array of four {@link BitVector}
     * instances, the canonical [0] plus 3 rotated versions.
     * Note: M, N are assumed to be initialized!
     * @param id the marker id
     * @param markerString the marker's bit pattern as a 0/1 string
     */
    private void addMarkerPattern(int id, String markerString) {
        if (id < 0 || id > M) {
            throw new IndexOutOfBoundsException("out-of-range marker dictionary id: " + id);
        }
        if (bitdata[id] != null) {
            throw new IllegalStateException("bit data already set for marker id " + id);
        }
        if (markerString.length() != markerBitCount) {
            throw new RuntimeException("wrong string length for marker id=" + id);
        }
        BitVector[] bitVectors = new BitVector[4];
        char[] markerPattern = markerString.toCharArray();
        bitVectors[0] = toBitVector(markerPattern);   // r=0: canonical (unrotated)
        // make rotated patterns for r = 1, 2, 3
        for (int r = 1; r < 4; r++) {
            markerPattern = RotationUtils.permute(markerPattern, rotperm);  // perform 2D rotation
            bitVectors[r] = toBitVector(markerPattern);
        }
        this.bitdata[id] = bitVectors;
    }

    @Override
    public String toString() {
        return String.format("%s [M=%d, N=%d, maxCorrectionBits=%d]",
                getClass().getSimpleName(), M, N, maxCorrectionBits);
    }

    // /**
    //  * Converts a 0/1 marker string to an array of four {@link BitVector}
    //  * instances, the canonical [0] plus 3 rotated versions.
    //  * Note: M, N are assumed to be initialized!
    //  *
    //  * @param markerString the 0/1 pattern string for a single marker
    //  * @return an 2D array of {@link BitVector} instances
    //  */
    // private BitVector[] makeBitVectors(String markerString) {
    //     BitVector[] bitVectors = new BitVector[4];
    //     // process all marker ids:
    //
    //     char[] markerPattern = markerString.toCharArray();
    //     bitVectors[0] = toBitVector(markerPattern);   // r=0: canonical (unrotated)
    //     // make rotated patterns for r = 1, 2, 3
    //     for (int r = 1; r < 4; r++) {
    //         markerPattern = RotationUtils.permute(markerPattern, rotperm);  // perform 2D rotation
    //         bitVectors[r] = toBitVector(markerPattern);
    //     }
    //
    //     return bitVectors;
    // }

    /**
     * Converts the 0/1 marker string array to an array of {@link BitVector,
     * pre-calculating rotated versions too.
     * Note: M, N are assumed to be initialized!
     *
     * @param markerStrings an array of 0/1 marker strings
     * @return an 2D array of {@link BitVector} instances, one item for each
     * marker id and four rotations: {@code BitVectors[id][rot]}
     */
    private BitVector[][] makeBitVectors(String[] markerStrings) {
        if (markerStrings.length != this.M) {
            throw new IllegalArgumentException("wrong length of markerString array: "
                    + markerStrings.length);
        }
        int[] rotperm = makeRotationPermutation(N); // permutation vector for 2D matrix rotation
        BitVector[][] allBitVectors = new BitVector[M][4];
        // process all marker ids:
        for (int id = 0; id < M; id++) {
            char[] markerPattern = markerStrings[id].toCharArray();
            allBitVectors[id][0] = toBitVector(markerPattern);   // r=0: canonical (unrotated)
            // make rotated patterns for r = 1, 2, 3
            for (int r = 1; r < 4; r++) {
                markerPattern = RotationUtils.permute(markerPattern, rotperm);  // perform 2D rotation
                allBitVectors[id][r] = toBitVector(markerPattern);
            }
        }
        return allBitVectors;
    }

    /**
     * Converts a 0/1 char array to a {@link BitVector}.
     * @param char01 the input char array
     * @return the corresponding {@link BitVector}
     */
    static BitVector toBitVector(char[] char01) {
        BitVector bs = new BitVector(char01.length);
        for (int i = 0; i < char01.length; i++) {
            if (char01[i] == '1') bs.setBit(i);    // '0' is unchecked/ignored
        }
        return bs;
    }

    static BitVector toBitVector(String str01) {
        return toBitVector(str01.toCharArray());
    }

    // ----------------------------------------------------------------------

    /**
     * Factory method. Create a ArUco dictionary from the contents of a
     * (gnu-zipped) JASON file. An exception is thrown if the resource is
     * not found at the specified location.
     *
     * @param clazz the class at the root of the relative path
     * @param relPath the name of the resource file (relative path to class)
     * @return the newly created dictionary
     */
    public static ArucoDictionary fromResource(Class<?> clazz, String relPath) {
        String classPath = clazz.getResource("").toExternalForm(); // safe for JARs

        ArucoDictionary dict;
        try (InputStream is = clazz.getResourceAsStream(relPath)) {
            assert is != null;
            InputStream gzipStream = new GZIPInputStream(is);
            // read and configure this dictionary:
            dict = readDictFromJsonStream(gzipStream);
        } catch (IOException e) {
            throw new MissingResourceException(
                    "unable to read resource " + classPath + relPath.length(),
                    clazz.getSimpleName(), relPath);
        }
        return dict;
    }

    /**
     * Reads a JSON-encoded {@link ArucoDictionary} from a stream.
     * @param is the input stream
     * @return the newly created dictionary
     */
    static ArucoDictionary readDictFromJsonStream(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final int nmarkers = root.get("nmarkers").asInt();
        final int markersize = root.get("markersize").asInt();
        final int maxCorrectionBits = root.get("maxCorrectionBits").asInt();
        final int markerBitCount = markersize * markersize;

        // create the initial (unfinished) dictionary
        ArucoDictionary dict = new ArucoDictionary(nmarkers, markersize, maxCorrectionBits);

        // collect all markers from the JSON tree and fill bit patterns in dict
        Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (field.startsWith("marker_")) {
                int id = Integer.parseInt(field.substring(7));
                String markerpattern = root.get(field).asText();
                dict.addMarkerPattern(id, markerpattern);
            }
        }
        dict.checkIntegrity();
        return dict;
    }

    // ---------------------------------------------------------------

    /**
     * Verifies that this dictionary is set up correctly.
     * Exceptions are thrown for any irregularities encountered.
     */
    public void checkIntegrity() {
        if (bitdata.length != M) {
            throw new IllegalStateException("wrong length of bitdata: " + bitdata.length);
        }
        for (int i = 0; i < M; i++) {
            if (bitdata[i] == null || bitdata[i].length != 4) {
                throw new IllegalStateException("bitdata[] missing or corrupted for marker id=" + i);
            }
            for (int r = 0; r < 4; r++) {
                if (bitdata[i][r] == null) {
                    throw new IllegalStateException("bitdata null for marker id=" + i + ", r=" + r);
                }
                if (bitdata[i][r].length() != markerBitCount) {
                    throw new IllegalStateException("wrong bitdata length for marker id=" + i + ", r=" + r);
                }
            }
        }
    }

    // ---------------------------------------------------------------

    /**
     * Returns the number of different marker codes in this dictionary.
     * @return the number of marker codes.
     */
    public int getNumberOfCodes() {
        return this.M;
    }

    /**
     * Returns N, the number of black and white  in each dimension of the marker.
     * The whole marker is of size N x N.
     * @return the size of the marker in x/y
     */
    public int getMarkerSize() {
        return this.N;
    }

    public int getMaxCorrectionBits() {
        return this.maxCorrectionBits;
    }

    // ----------------------------------------------------------------------

    public BitVector getBits(int id, int rot) {
        return this.bitdata[id][rot];
    }

    // -------------------------------------------------------------

    /**
     * Scans the dictionary for the index of the best-fitting marker and
     * if successful, builds and returns a {@link LookupResult} instance.
     * Otherwise, null is returned.
     * Different to the OpenCV implementation we potentially scan the entire
     * dictionary, continuing even if an "acceptable" match was found.
     * The search stops, however, if a perfect (i.e., zero-distance) match
     * is encountered, since no more improvement is possible after that.
     * Scanning the dictionary exhaustively is not a great effort since bitwise
     * matching (Hamming distance calculation) is very efficient.
     *
     * @param candidate the bit pattern extracted from the candidate region
     * @return a {@link LookupResult} instance or null if unsuccessful
     */
    public LookupResult lookup(BitVector candidate, double maxCorrectionRate) {
        Objects.requireNonNull(candidate, "candidate bits must not be null");
        int maxCorrectionRecalc = (int) (maxCorrectionBits * maxCorrectionRate);

        int card = candidate.cardinality();
        // candidate is blank, all bits either zero or one:
        if (card == 0 || card == markerBitCount) {
            return null;
        }

        int minIdx = -1;
        int minRot = -1;
        int minDist = Integer.MAX_VALUE;

        // Scan the dictionary for a pattern match at least until a zero-distance
        // pattern is found:
        outer:
        for (int m = 0; m < M; m++) {       // all M marker id's
            for (int r = 0; r < 4; r++) {   // all 4 rotations
                int dist = candidate.hammingDistance(bitdata[m][r]);
                if(dist < minDist) {
                    minDist = dist;
                    minIdx = m;
                    minRot = r;
                }
                if (minDist == 0) { // perfect match, time to quit ...
                    break outer;
                }
            }
        }

        if (minIdx >= 0 &&  minDist <= maxCorrectionRecalc) {
            return new LookupResult(minIdx, minRot, minDist);
        }
        else {
            return null;
        }
    }

    // -------------------------------------------------------------------------

    public record LookupResult(int markerIndex, int rotation, int hammingDistance) {}

    // public static class LookupResult {
    //     final int markerIndex;
    //     final int rotation;
    //     final int hammingDistance;
    //
    //     LookupResult(int markerIndex, int rotation, int hammingDistance) {
    //         this.markerIndex = markerIndex;
    //         this.rotation = rotation;
    //         this.hammingDistance = hammingDistance;
    //     }
    //
    //     @Override
    //     public String toString() {
    //         return String.format("%s [id=%d, r=%d, dist=%d]",
    //                 getClass().getSimpleName(), markerIndex, rotation, hammingDistance);
    //     }
    // }

}
