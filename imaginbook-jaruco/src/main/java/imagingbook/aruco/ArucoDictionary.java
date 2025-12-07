/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.aruco;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ij.process.ByteProcessor;
import imagingbook.common.util.bits.BitVector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

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
    private final byte[][][] bytedata;               // marker bit patterns, bytedata[m][r] is a 0/1 byte[] for one marker instance

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
        this.bytedata = makeByteData(markerStrings);
    }

    private byte[][][] makeByteData(String[] markerStrings) {
        if (markerStrings.length != this.M) {
            throw new IllegalArgumentException("wrong length of markerString[]: "
                    + markerStrings.length);
        }
        int NxN = N * N;
        byte[][][] bytes = new byte[M][4][];
        for (int id = 0; id < M; id++) {
            char[] chars = markerStrings[id].toCharArray();
            // copy content of chars to bytes (canonical pattern for r = 0)
            byte[] canonical = new byte[NxN];
            for (int k = 0; k < NxN; k++) {
                char c = chars[k];
                canonical[k] = switch(c) {
                    case '0' -> 0;
                    case '1' -> 1;
                    default -> {throw new RuntimeException("wrong element in 0/1 string: " + c);}
                };
            }
            bytes[id][0] = canonical;
            byte[][] pattern2d = toMatrix(canonical, N);
            // make rotated patterns for r = 1, 2, 3
            for (int r = 1; r < 4; r++) {
                rotateLeft(pattern2d);
                bytes[id][r] = flatten(pattern2d);
            }
        }
        return bytes;
    }

    // ----------------------------------------------------------------------

    byte[] getMarkerPattern(int id, int rot) {
        return this.bytedata[id][rot];
    }

    // ----------------------------------------------------------------------

    static byte[] toByteArray(String str) {
        char[] chars = str.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = switch(chars[i]) {
                case '0' -> 0;
                case '1' -> 1;
                default -> {throw new RuntimeException("wrong element in 0/1 string: " + chars[i]);}
            };
        }
        return bytes;
    }

    /**
     * Creates a 2D square array of the specified size from a 1D array.
     * @param src 1D array of sufficient size
     * @param n size of new 2D array (n x n)
     * @return the 2D array
     */
    static byte[][] toMatrix(byte[] src, int n) {
        if (src.length != n * n)
            throw new IllegalArgumentException("Length must be N*N but is " + (n * n));
        byte[][] m = new byte[n][n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = src[k++];
            }
        }
        return m;
    }

    /**
     * Flattens a 2D array to a 1D array in row-major order.
     * @param m a 2D array
     * @return the 1D array
     */
    static byte[] flatten(byte[][] m) {
        int n = m.length;
        byte[] out = new byte[n * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                out[k++] = m[i][j];
            }
        }
        return out;
    }

    /**
     * Rotates the given square array left (in-place).
     * @param m a 2D square array
     */
    static void rotateLeft(byte[][] m) {
        int n = m.length;
        // Transpose (swap m[i][j] with m[j][i])
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                byte tmp = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = tmp;
            }
        }
        // Reverse each column (swap rows)
        for (int col = 0; col < n; col++) {
            int top = 0;
            int bottom = n - 1;
            while (top < bottom) {
                byte tmp = m[top][col];
                m[top][col] = m[bottom][col];
                m[bottom][col] = tmp;
                top++;
                bottom--;
            }
        }
    }

    static String toString(byte[][] m) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(Arrays.toString(m[i]));
            if (i != m.length-1) sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

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

        int nmarkers = root.get("nmarkers").asInt();
        int markersize = root.get("markersize").asInt();
        int maxCorrectionBits = root.get("maxCorrectionBits").asInt();

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
                markerStrings[id] = root.get(field).asText();
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

    public static String toStringUnsigned(byte[] bytes) {
        int[] tmp = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            tmp[i] = 0xFF & bytes[i];
        }
        return Arrays.toString(tmp);
    }

    public static String toStringBinary(byte[] bytes) {
        BitVector bv = BitVector.from(bytes);
        return bv.toString();
    }

    // -------------------------------------------------------------

    public String markerAsString1D(byte[] markerBytes) {
        // create a string of NxN zero/ones:
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < markerBytes.length; i++) {
            String s = String.format("%8s", Integer.toBinaryString(markerBytes[i] & 0xFF));
            sb.append(s.replace(' ', '0'));
        }
        return sb.substring(0, N * N);
    }

    public String markerAsString2D(byte[] markerBytes) {
        String s1d = markerAsString1D(markerBytes);
        StringBuilder sb = new StringBuilder();
        int start = 0;
        for (int i = 0; i < N; i++) {
            sb.append(s1d, start, start + N);
            sb.append("\n");
            start = start + N;
        }

        return sb.toString();
    }

    public String markerAsString2D(String s1d) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        for (int i = 0; i < N; i++) {
            sb.append(s1d, start, start + N);
            sb.append("\n");
            start = start + N;
        }

        return sb.toString();
    }

    public ByteProcessor bytesToImage(byte[] bytes) {
        String str01 = markerAsString1D(bytes);
        char[] ch01 = str01.toCharArray();
        byte[] b0255 = new byte[ch01.length];
        for (int i = 0; i < ch01.length; i++) {
            b0255[i] = (ch01[i] == '0') ? 0 : (byte)0xFF;
        }
        ByteProcessor bp = new ByteProcessor(N, N, Arrays.copyOf(b0255, N*N));
        return bp;
    }

    public byte[] imageToBytes(ByteProcessor bp) {
        byte[] b0255 = (byte[]) bp.getPixels();
        char[] chars = new char[8];
        int n = (bp.getHeight() * bp.getWidth() + 7) / 8;
        byte[] bytes = new byte[n];
        for (int k = 0, start = 0; start < b0255.length; k++, start+=8) {
            Arrays.fill(chars, '0');
            for (int i = 0; i < 8; i++) {
                if (start + i >= b0255.length) break;
                chars[i] = (b0255[start + i] == 0) ? '0' : '1';
            }
            String str = String.valueOf(chars);
            // System.out.println("str = " + str);
            int intValue = Integer.parseInt(String.valueOf(chars), 2);
            // System.out.println("intVal = " + intValue);
            bytes[k] = (byte) (0xFF & intValue);
        }
        return bytes;
    }

    // --------------------------------------------------------------

    static void checkPatterRotation() {
        String s = "1010001011011001010111100";
        byte[] b = toByteArray("1010001011011001010111100");
        System.out.println("string = " + s);
        System.out.println("bytes = " + Arrays.toString(b));
        byte[][] marker = toMatrix(b, 5);
        System.out.println("marker = \n" + toString(marker));

        byte[] back1d = flatten(marker);
        System.out.println("bytes = " + Arrays.toString(back1d));
        System.out.println("is same = " + Arrays.equals(b, back1d));
    }

    static void showDictionaryMarkersRotated() {
        ArucoDictionary dict = ArucoPredefiedDictionary.DICT_5X5_50.getDictionary();
        for (int r = 0; r < 4; r++) {
            byte[] bytes = dict.getMarkerPattern(2, r);
            byte[][] marker = toMatrix(bytes, 5);
            System.out.println(r + ":\n" + toString(marker));
        }
    }

    public static void main(String[] args) {
        // checkPatterRotation();
        showDictionaryMarkersRotated();
    }

}
