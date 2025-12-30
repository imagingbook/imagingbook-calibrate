/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco.obsolete;

import ij.process.ByteProcessor;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

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
@Deprecated
public class Dictionary {

    static final int R = 4;                     // number of marker rotations

    private final int M;                        // number of marker codes
    private final int N;                        // number of bits per dimension
    private final int P;                        // length of the byte vector for a single marker (bytelength)
    private final int maxCorrectionBits;        // max. number of correction bits
    private byte[][][] bytedata;               // marker bit patterns encoded as bytes

    public Dictionary(byte[][][] bytes, int M, int N, int maxCorrectionBits) {
        this.bytedata = bytes;
        this.N = N;
        this.M = M;
        this.maxCorrectionBits = maxCorrectionBits;
        this.P = (N * N + 7) / 8;          // = ceil(NxN/8)
    }

    /**
     * Fill the byte data of this dictionary from the supplied array,
     * thereby truncting the array to the specified size.
     * An exception is thrown if any of the array dimensions is too small.
     * @param dictdata the source array (unsigned bytes)
     */
    void setData(byte[][][] dictdata) {
        if (dictdata.length < M)
            throw new IllegalArgumentException("wrong dictdata dimension 0, must be at least " + M);
        if (dictdata[0].length < R)
            throw new IllegalArgumentException("wrong dictdata dimension 1, must be at least " + R);
        if (dictdata[0][0].length < P)
            throw new IllegalArgumentException("wrong dictdata dimension 2, must be at least " + P);

            // System.out.printf("d0=%d d1=%d d2=%d\n", dictdata.length, dictdata[0].length, dictdata[0][0].length);
        this.bytedata = new byte[M][R][P];
        // System.out.printf("s0=%d s1=%d s2=%d\n", bytesList.length, bytesList[0].length, bytesList[0][0].length);
        try {
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < R; j++) {
                    for (int k = 0; k < P; k++) {
                        this.bytedata[i][j][k] = dictdata[i][j][k];
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("wrong dimensions in dictionary data");
        }
    }

    /**
     * Returns a reference to the dictionary's byte data (null if  yet
     * initialized).
     * @return  dictionary's byte data or null
     */
    byte[][][] getByteData() {
        return this.bytedata;
    }

    /**
     * Returns the byte data for a single marker code at the specified
     * rotation. An exception is thrown if any of the parameters is
     * out of bounds.
     * The byte array is exactly as long as to hold the underlying bit string
     * of length N x N, one bit for each black and white marker field.
     *
     * @param code the marker code
     * @param rot the rotation index (0,...,3)
     * @return
     */
    public byte[] getCodeBytes(int code, int rot) {
        if (code < 0 || code >= M) {
            throw new IllegalArgumentException("dictionary code out of bounds: " + code);
        }
        if (rot < 0 || rot >= R) {
            throw new IllegalArgumentException("rotation index out of bounds: " + rot);
        }
        return bytedata[code][rot];
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
        return bv.asString();
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

    // -------------------------------------------------------------

        static String reverse(String original) {
            return new StringBuilder(original).reverse().toString();
        }


    // Bit order in bytes is MSB first (big-endian)
    static void showMarkerBytesMsbFirst(int markerId) {
        // for (PredefiedDictionary pd : PredefiedDictionary.values()) {
        {PredefiedDictionary pd = PredefiedDictionary.DICT_5X5_1000;
            System.out.println("Dict = " + pd.name());
            Dictionary dict  = pd.getDict();
            System.out.println("Size = " + dict.getMarkerSize());
            System.out.println("ArucoMarker bits = " + dict.getMarkerSize() * dict.getMarkerSize());
            System.out.println("  codes = " + dict.getNumberOfCodes());

            int N = dict.getMarkerSize();
            // int markerId = 0;

            for (int r = 0; r < R; r++) {
                byte[] br = dict.getCodeBytes(markerId, r);
                // System.out.println("Rotation r = " + r + ": " + Arrays.toString(br));
                System.out.printf("ID=%d, r=%d bytes=%s\n", markerId, r, toStringUnsigned(br));

                System.out.print("MSB bytes(orig):  ");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < br.length; i++) {
                    String s = String.format("%8s", Integer.toBinaryString(br[i] & 0xFF)).replace(' ', '0');
                    sb.append(s);
                }
                String bitsAll = sb.toString();
                System.out.println(bitsAll);

                String bitsTrunc = bitsAll.substring(0, N * N);
                System.out.print("MSB bytes(trunc): ");
                System.out.println(bitsTrunc);
                // System.out.println(dict.markerAsString1D(br));

                System.out.println();
                System.out.println(dict.markerAsString2D(bitsTrunc));
                // System.out.println(dict.markerAsString2D(br));
                // System.out.println();
            }
        }

    }

    // Bit order in bytes is LSB first (little-endian)
    static void showMarkerBytesLsbFirst() {
        // for (PredefiedDictionary pd : PredefiedDictionary.values()) {
        {PredefiedDictionary pd = PredefiedDictionary.DICT_5X5_1000;
            System.out.println("Dict = " + pd.name());
            Dictionary dict  = pd.getDict();
            System.out.println("Size = " + dict.getMarkerSize());
            System.out.println("ArucoMarker bits = " + dict.getMarkerSize() * dict.getMarkerSize());
            System.out.println("  codes = " + dict.getNumberOfCodes());
            int N = dict.getMarkerSize();
            int markerId = 0;

            for (int r = 0; r < R; r++) {
                byte[] br = dict.getCodeBytes(markerId, r);
                // System.out.println("Rotation r = " + r + ": " + Arrays.toString(br));
                System.out.printf("ID=%d, r=%d bytes=%s\n", markerId, r, toStringUnsigned(br));

                System.out.print("LSB bytes(orig):  ");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < br.length; i++) {
                    String s = String.format("%8s", Integer.toBinaryString(br[i] & 0xFF)).replace(' ', '0');
                    sb.append(reverse(s));
                }
                String bitsAll = sb.toString();
                System.out.println(bitsAll);

                String bitsTrunc = bitsAll.substring(0, N * N);
                System.out.print("LSB bytes(trunc): ");
                System.out.println(bitsTrunc);
                // System.out.println(dict.markerAsString1D(br));

                System.out.println();
                System.out.println(dict.markerAsString2D(bitsTrunc));
                // System.out.println();
            }
        }

    }

    public static void main(String[] args) {
        showMarkerBytesMsbFirst(2);
        // showMarkerBytesLsbFirst();

    }

}
