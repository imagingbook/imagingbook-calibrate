/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static imagingbook.jaruco.Rotations.makeRotationPermutation;

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
    private final int maxCorrectionBits;        // max. number of correction bits
    // private final byte[][][] bytedata;          // marker bit patterns, bytedata[m][r] is a 0/1 byte[] for one marker instance
    private final BitSet[][] bitsets;           // compact replacement for bytedata
    private final BitSet scratch;

    /**
     * Constructor
     * @param M number of code id's
     * @param N marker size
     * @param maxCorrectionBits
     * @param markerStrings array of 0/1 strings, one for each marker id,
     * specifiying its canonical (unrotated) pattern
     */
    private ArucoDictionary(int M, int N, int maxCorrectionBits, String[] markerStrings) {
        this.M = M;
        this.N = N;
        this.maxCorrectionBits = maxCorrectionBits;
        // this.bytedata = makeByteData(markerStrings);    // TODO: remove!
        this.bitsets = makeBitSets(markerStrings);
        this.scratch = new BitSet(N*N); // scratch bitset for hamming distance calculation
    }

    @Override
    public String toString() {
        return String.format("%s [M=%d, N=%d, maxCorrectionBits=%d]", getClass().getSimpleName(), M, N, maxCorrectionBits);
    }

    // @Deprecated
    // private byte[][][] makeByteData(String[] markerStrings) {
    //     if (markerStrings.length != this.M) {
    //         throw new IllegalArgumentException("wrong length of markerString[]: "
    //                 + markerStrings.length);
    //     }
    //     int NxN = N * N;
    //     byte[][][] bytes = new byte[M][4][];
    //     for (int id = 0; id < M; id++) {
    //         char[] chars = markerStrings[id].toCharArray();
    //         // copy content of chars to bytes (canonical pattern for r = 0)
    //         byte[] canonical = new byte[NxN];
    //         for (int k = 0; k < NxN; k++) {
    //             char c = chars[k];
    //             canonical[k] = switch(c) {
    //                 case '0' -> 0;
    //                 case '1' -> 1;
    //                 default -> {throw new RuntimeException("wrong element in 0/1 string: " + c);}
    //             };
    //         }
    //         bytes[id][0] = canonical;
    //         byte[][] pattern2d = toMatrix(canonical, N);
    //         // make rotated patterns for r = 1, 2, 3
    //         for (int r = 1; r < 4; r++) {
    //             rotateLeft(pattern2d);
    //             bytes[id][r] = flatten(pattern2d);
    //         }
    //     }
    //     return bytes;
    // }

    // TODO: remove intermediate byte[]s
    // private BitSet[][] makeBitSets(String[] markerStrings) {
    //     if (markerStrings.length != this.M) {
    //         throw new IllegalArgumentException("wrong length of markerString[]: "
    //                 + markerStrings.length);
    //     }
    //     int NxN = N * N;
    //     BitSet[][] bitsets = new BitSet[M][4];
    //     for (int id = 0; id < M; id++) {
    //         char[] chars = markerStrings[id].toCharArray();
    //         // copy content of chars to bytes (canonical pattern for r = 0)
    //         byte[] canonical = new byte[NxN];
    //         for (int k = 0; k < NxN; k++) {
    //             char c = chars[k];
    //             canonical[k] = switch(c) {
    //                 case '0' -> 0;
    //                 case '1' -> 1;
    //                 default -> {throw new RuntimeException("wrong element in 0/1 string: " + c);}
    //             };
    //         }
    //         bitsets[id][0] = ByteArrayUtils.toBitSet(canonical);
    //         byte[][] pattern2d = toMatrix(canonical, N);
    //         // make rotated patterns for r = 1, 2, 3
    //         for (int r = 1; r < 4; r++) {
    //             rotateLeft(pattern2d);
    //             bitsets[id][r] = ByteArrayUtils.toBitSet(flatten(pattern2d));
    //         }
    //     }
    //     return bitsets;
    // }

    // M, N are assumed to be initialized!

    /**
     * Converts the 0/1 marker string array to an array of {@link BitSet},
     * pre-calculating rotated versions too.
     *
     * @param markerStrings an array of 0/1 marker strings
     * @return an 2D array of {@link BitSet} instances, one item for each
     * marker id and four rotations: {@code bitsets[id][rot]}
     */
    private BitSet[][] makeBitSets(String[] markerStrings) {
        if (markerStrings.length != this.M) {
            throw new IllegalArgumentException("wrong length of markerString array: "
                    + markerStrings.length);
        }
        int[] rotperm = makeRotationPermutation(N); // permutation vector for 2D matrix rotation
        BitSet[][] allbitsets = new BitSet[M][4];
        // process all marker ids:
        for (int id = 0; id < M; id++) {
            char[] markerPattern = markerStrings[id].toCharArray();
            allbitsets[id][0] = toBitSet(markerPattern);   // r=0: canonical (unrotated)
            // make rotated patterns for r = 1, 2, 3
            for (int r = 1; r < 4; r++) {
                markerPattern = Rotations.permute(markerPattern, rotperm);  // perform 2D rotation
                allbitsets[id][r] = toBitSet(markerPattern);
            }
        }
        return allbitsets;
    }

    /**
     * Converts a 0/1 char array to a {@link BitSet}.
     * @param char01 the input char array
     * @return the corresponding {@link BitSet}
     */
    static BitSet toBitSet(char[] char01) {
        BitSet bs = new BitSet(char01.length);
        for (int i = 0; i < char01.length; i++) {
            if (char01[i] == '1') bs.set(i);    // '0' is unchecked/ignored
        }
        return bs;
    }

    static BitSet toBitSet(String str01) {
        return toBitSet(str01.toCharArray());
    }

    // ----------------------------------------------------------------------

    /**
     * Factory method. Create a ArUco dictionary from the contents of a
     * (gnu-zipped) JASON file. An exception is thrown if the resource is
     * not found at the specified location.
     *
     * @param clazz the class at the root of the relative path
     * @param path the name of the resource file (relative path to class)
     * @return the newly created dictionary
     */
    public static ArucoDictionary fromResource(Class<?> clazz, String path) {
        ArucoDictionary dict;
        try (InputStream is = clazz.getResourceAsStream(path)) {
            assert is != null;
            InputStream gzipStream = new GZIPInputStream(is);
            // read and configure this dictionary:
            dict = getDictFromJsonStream(gzipStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dict;
    }

    static ArucoDictionary getDictFromJsonStream(InputStream is) {
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

        // array of string to hold the 0/1 patters, one string for each marker id
        String[] markerStrings = new String[nmarkers];

        // collect all markers from the JSON tree
        Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (field.startsWith("marker_")) {
                int id = Integer.parseInt(field.substring(7));
                // check marker id for range
                if (id < 0 || id >= nmarkers) {
                    throw new IndexOutOfBoundsException("out-of-range marker dictionary id: " + id);
                }
                // check marker id for duplicate entries
                if (markerStrings[id] != null) {
                    throw new RuntimeException("duplicate marker dictionary id: " + id);
                }

                String markerStr = root.get(field).asText();
                if (markerStr.length() != markerBitCount) {
                    throw new RuntimeException("wrong string length for marker id=" + id);
                }
                markerStrings[id] = markerStr;
            }
        }

        return new ArucoDictionary(nmarkers, markersize, maxCorrectionBits, markerStrings);
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

    // public byte[] getMarkerPattern(int id, int rot) {
    //     return this.bytedata[id][rot];
    // }

    public BitSet getBitSet(int id, int rot) {
        return this.bitsets[id][rot];
    }

    // -------------------------------------------------------------
    // bool Dictionary::identify(const Mat &onlyBits, int &idx, int &rotation, double maxCorrectionRate) const {
    //     CV_Assert(onlyBits.rows == markerSize && onlyBits.cols == markerSize);

    /**
     * Scans the dictionary for the index of the best-fitting marker and
     * if successful, builds and returns a {@link LookupResult} instance.
     * Otherwise null is returned.
     *
     * @param candidateBits the bit pattern extracted from the candidate region
     * @return a {@link LookupResult} instance or null if unsuccessful
     */
    public LookupResult lookup(BitSet candidateBits, double maxCorrectionRate) {
        Objects.requireNonNull(candidateBits, "candidateBits must not be null");
        int maxCorrectionRecalculed = (int) (maxCorrectionBits * maxCorrectionRate);
        int idx = -1; // by default, not found
        int rotation = -1;
        int currentMinDistance = 0;

        for (int m = 0; m < this.M; m++) {
            currentMinDistance = N * N + 1;     // check why initialized here!
            int currentRotation = -1;

            for (int r = 0; r < 4; r++) {
                BitSet ref = getBitSet(m, r);
                // System.out.printf("id=%d r=%d: a=%s b=%s\n", m, r, ref, candidateBits);
                int currentHamming = normHamming(getBitSet(m, r), candidateBits);
                if(currentHamming < currentMinDistance) {
                    currentMinDistance = currentHamming;
                    currentRotation = r;
                }
            }

            // if maxCorrection is fulfilled, return this one
            if (currentMinDistance <= maxCorrectionRecalculed) {
                idx = m;
                rotation = currentRotation;
                break;
            }
        }

        return (idx >= 0) ?
                new LookupResult(idx, rotation, currentMinDistance) : null;
    }

    // private int normHamming(BitSet a, BitSet b) {
    //     BitSet x = (BitSet) a.clone();
    //     x.xor(b);
    //     return x.cardinality();   // number of bits set to 1
    // }

    // private int normHamming(BitSet a, BitSet b) {
    //     scratch.clear();
    //     scratch.or(a);   // scratch := a
    //     scratch.xor(b);  // scratch := a xor b
    //     return scratch.cardinality();
    // }

    static int normHamming(BitSet a, BitSet b) {
        //System.out.printf("a=%s b=%s\n", toString01(a), toString01(b));
        BitSet x = (BitSet) a.clone(); // cast needed because clone() returns Object
        x.xor(b);
        return x.cardinality();
    }

    public static class LookupResult {
        final int markerIndex;
        final int rotation;
        final int hammingDistance;

        LookupResult(int markerIndex, int rotation, int hammingDistance) {
            this.markerIndex = markerIndex;
            this.rotation = rotation;
            this.hammingDistance = hammingDistance;
        }

        @Override
        public String toString() {
            return String.format("%s [id=%d, r=%d, dist=%d]",
                    getClass().getSimpleName(), markerIndex, rotation, hammingDistance);
        }
    }

    // /**
    //  * Converts a byte array with 0/1 values (only) to a string with 0/1
    //  * characters.
    //  *
    //  * @param bytes01
    //  * @return
    //  */
    // String toString01(byte[] bytes01) {
    //     // create a string of NxN zero/ones:
    //     StringBuilder sb = new StringBuilder();
    //     for (int i = 0; i < bytes01.length; i++) {
    //         String s = String.format("%8s", Integer.toBinaryString(bytes01[i] & 0xFF));
    //         sb.append(s.replace(' ', '0'));
    //     }
    //     return sb.substring(0, N * N);
    // }



    // public String markerAsString2D(byte[] markerBytes) {
    //     String s1d = toString01(markerBytes);
    //     StringBuilder sb = new StringBuilder();
    //     int start = 0;
    //     for (int i = 0; i < N; i++) {
    //         sb.append(s1d, start, start + N);
    //         sb.append("\n");
    //         start = start + N;
    //     }
    //
    //     return sb.toString();
    // }

    // public String markerAsString2D(String s1d) {
    //     StringBuilder sb = new StringBuilder();
    //     int start = 0;
    //     for (int i = 0; i < N; i++) {
    //         sb.append(s1d, start, start + N);
    //         sb.append("\n");
    //         start = start + N;
    //     }
    //
    //     return sb.toString();
    // }
    //
    //
    // public ByteProcessor bytesToImage(byte[] bytes) {
    //     String str01 = toString01(bytes);
    //     char[] ch01 = str01.toCharArray();
    //     byte[] b0255 = new byte[ch01.length];
    //     for (int i = 0; i < ch01.length; i++) {
    //         b0255[i] = (ch01[i] == '0') ? 0 : (byte)0xFF;
    //     }
    //     ByteProcessor bp = new ByteProcessor(N, N, Arrays.copyOf(b0255, N*N));
    //     return bp;
    // }
    //
    // public byte[] imageToBytes(ByteProcessor bp) {
    //     byte[] b0255 = (byte[]) bp.getPixels();
    //     char[] chars = new char[8];
    //     int n = (bp.getHeight() * bp.getWidth() + 7) / 8;
    //     byte[] bytes = new byte[n];
    //     for (int k = 0, start = 0; start < b0255.length; k++, start+=8) {
    //         Arrays.fill(chars, '0');
    //         for (int i = 0; i < 8; i++) {
    //             if (start + i >= b0255.length) break;
    //             chars[i] = (b0255[start + i] == 0) ? '0' : '1';
    //         }
    //         String str = String.valueOf(chars);
    //         // System.out.println("str = " + str);
    //         int intValue = Integer.parseInt(String.valueOf(chars), 2);
    //         // System.out.println("intVal = " + intValue);
    //         bytes[k] = (byte) (0xFF & intValue);
    //     }
    //     return bytes;
    // }

    // --------------------------------------------------------------

    // static void checkPatterRotation() {
    //     String s = "1010001011011001010111100";
    //     byte[] b = toByteArray("1010001011011001010111100");
    //     System.out.println("string = " + s);
    //     System.out.println("bytes = " + Arrays.toString(b));
    //     byte[][] marker = toMatrix(b, 5);
    //     System.out.println("marker = \n" + toString(marker));
    //
    //     byte[] back1d = flatten(marker);
    //     System.out.println("bytes = " + Arrays.toString(back1d));
    //     System.out.println("is same = " + Arrays.equals(b, back1d));
    // }

    // static void showDictionaryMarkersRotated() {
    //     ArucoDictionary dict = ArucoPredefinedDictionary.DICT_5X5_50.getDictionary();
    //     for (int r = 0; r < 4; r++) {
    //         byte[] bytes = dict.getMarkerPattern(2, r);
    //         byte[][] marker = toMatrix(bytes, 5);
    //         System.out.println(r + ":\n" + ByteArrayUtils.toString(marker));
    //     }
    // }

    public static void main(String[] args) {
        // checkPatterRotation();
        // showDictionaryMarkersRotated();
    }

}
