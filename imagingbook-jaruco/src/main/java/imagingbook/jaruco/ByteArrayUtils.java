package imagingbook.jaruco;

import java.util.Arrays;
// import java.util.BitSet;

public abstract class ByteArrayUtils {

    public static String toStringUnsigned(byte[] bytes) {
        int[] tmp = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            tmp[i] = 0xFF & bytes[i];
        }
        return Arrays.toString(tmp);
    }

//    public static String toStringBinary(byte[] bytes) {
//        BitVector bv = BitVector.from(bytes);
//        return bv.toString();
//    }

    // -------------------------------------------------------------

    public static byte[] toByteArray(String str) {
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
    public static byte[][] toMatrix(byte[] src, int n) {
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
    public static byte[] flatten(byte[][] m) {
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
    public static void rotateLeft(byte[][] m) {
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

    public static String toString(byte[][] m) {
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

    public static int countValues(byte[] m, byte x) {
        int count = 0;
        for (byte b : m) {
            if (b == x) count++;
        }
        return count;
    }

    // ---------------------------------

    // /**
    //  * Converts a 0/1 byte array to a {@link BitSet}.
    //  * @param arr the input byte array
    //  * @return the corresponding {@link BitSet}
    //  */
    // public static BitSet toBitSet(byte[] arr) {
    //     BitSet bs = new BitSet(arr.length);
    //     // System.out.println("bs length = " + bs.length());
    //     for (int i = 0; i < arr.length; i++) {
    //         if (arr[i] == 1) bs.set(i);
    //     }
    //     return bs;
    // }


    // /**
    //  * Converts a {@link BitSet} into a byte array with 0/1 values.
    //  * @param bs the input {@link BitSet}
    //  * @return
    //  */
    // public static byte[] toByteArray(BitSet bs) {
    //     final int n = bs.length();
    //     byte[] bytes01 = new byte[n];
    //     StringBuilder sb = new StringBuilder(n);
    //     for (int i = 0; i < n; i++) {
    //         bytes01[i] =  bs.get(i) ? (byte) 1 : (byte) 0;
    //     }
    //     return bytes01;
    // }

    // /**
    //  * Converts the input {@link BitSet} to a 0/1 character string,
    //  * which ends with the last (highest) 1-bit in the {@link BitSet}.
    //  * @param bs the input {@link BitSet}
    //  * @return a 0/1 character string
    //  */
    // public static String toString01(BitSet bs) {
    //     return toString01(bs, bs.length());
    // }

//     /**
//      * Converts the input {@link BitSet} to a 0/1 character string
//      * of the specified length. The bit sequence is either truncated
//      * or extended with zeros.
//      * @param bs the input {@link BitSet}
//      * @param n the output length
//      * @return a 0/1 character string
//      */
//     public static String toString01(BitSet bs, int n) {
//         StringBuilder sb = new StringBuilder(n);
//         for (int i = 0; i < n; i++) {
//             sb.append(bs.get(i) ? '1' : '0');
//         }
// //        sb.append(" (");
// //        sb.append(n);
// //        sb.append(")");
//         return sb.toString();
//     }

    // static void playBitString() {
    //     byte[] arr1 = new byte[] {0,0,1,0,1,1,1};
    //     System.out.println("arr1 = " + Arrays.toString(arr1));
    //     BitSet bs = toBitSet(arr1);
    //     System.out.println("bs String  = " + toString01(bs));
    //     System.out.println("arr2 = " + Arrays.toString(toByteArray(bs)));
    // }
    //
    // static void playBitString2() {
    //     BitSet bs1 = toBitSet(new byte[] {0,0,1,0,1,1,1});
    //     BitSet bs2 = toBitSet(new byte[] {1,0,1,0,1,0,0});
    //     int n = bs1.length();
    //     System.out.println("bs1  = " + toString01(bs1, n));
    //     System.out.println("bs2  = " + toString01(bs2, n));
    //
    //     BitSet bs1d ;
    //
    //     bs1d = (BitSet) bs1.clone();
    //     bs1d.xor(bs2);
    //     System.out.println("bs1 xor bs2  =  " + toString01(bs1d, n));
    //
    //     bs1d = (BitSet) bs1.clone();
    //     bs1d.and(bs2);
    //     System.out.println("bs1 and bs2  = " + toString01(bs1d, n));
    //     System.out.println("card = " + bs1d.cardinality());
    //
    //     // bs1d = (BitSet) bs1.clone();
    //     // bs1d.andNot(bs2);
    //     // System.out.println("bs1 andnot bs2  = " + toString01(bs1d));
    //
    //     BitSet copy = new BitSet() {{ or(bs1); }};
    // }

    // public static void main(String[] args) {
    //     playBitString2();
    // }
}
