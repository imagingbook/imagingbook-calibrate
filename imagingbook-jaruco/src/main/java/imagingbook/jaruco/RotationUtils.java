package imagingbook.jaruco;

public abstract class Rotations {

    /**
     * Calculates and returns the 1D permutation vector for rotating a
     * NxN square matrix, assuming that its values are stored in row-major
     * order.
     *
     * @param N size of the matrix (NxN)
     * @return the 1D  vector
     */
    public static int[] makeRotationPermutation(int N) {
        // create a NxN matrix with row-major indices:
        int[][] mat = new int[N][N];
        int k = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                mat[i][j] = k;
                k++;
            }
        }

        // rotate the index matrix
        matrixRotateLeft(mat);

        // flatten to 1D array:
        int[] perm = new int[N * N];
        k = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                perm[k] = mat[i][j];
                k++;
            }
        }

        return perm;
    }

    /**
     * Rotates the given square 2D array left (in-place).
     * @param m a 2D square array
     */
    public static void matrixRotateLeft(int[][] m) {
        int n = m.length;
        // Transpose (swap m[i][j] with m[j][i])
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int tmp = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = tmp;
            }
        }
        // Reverse each column (swap rows)
        for (int col = 0; col < n; col++) {
            int top = 0;
            int bottom = n - 1;
            while (top < bottom) {
                int tmp = m[top][col];
                m[top][col] = m[bottom][col];
                m[bottom][col] = tmp;
                top++;
                bottom--;
            }
        }
    }

    // ----------------------------------------------------------

    public static char[] permute(char[] data, int[] perm) {
        if (data.length != perm.length)
            throw new IllegalArgumentException(
                    String.format("data and permutation vector not of same length: %d vs. %d", data.length, perm.length));
        char[] datap = data.clone();
        for (int i = 0; i < data.length; i++) {
            datap[i] = data[perm[i]];
        }
        return datap;
    }



}
