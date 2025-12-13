package imagingbook.jaruco;

import ij.process.FloatProcessor;
import imagingbook.common.corners.SubpixelMaxInterpolator;
import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.pyramid.GaussianPyramid;

public class CornerTuner {

    static double CORNER_SCORE_THRESHOLD = 100;
    private final GaussianPyramid pyramid;

    public CornerTuner(GaussianPyramid pyramid) {
        this.pyramid = pyramid;
    }

    /**
     * Use the corner score in pyramid to adjust the corner positions of the
     * given outline.
     * @param outline
     */
    public void refineCorners(MarkerOutline outline) {
        System.out.println("refineCorners: refine outline " + outline.uid);
        for (int i = 0; i < outline.polygon.size(); i++) {
            Pnt2d p = outline.polygon.get(i);
            System.out.printf("corner %d: %s\n", i, p);
            findNearbyCorner(p);
//            Pnt2d pp = refineOneCorner(p);
//            if (pp != null) {
//                // replace this corner vertex with the refined one
//                outline.polygon.set(i, pp);
//            }
//            else {
//                // System.out.println("   ***** keeping ********: " + p);
//            }
        }
    }

    // ---------------------------------------------------
    private record CornerLocalMax(int u, int v, int level, float score) {}
    // ---------------------------------------------------

    /**
     * Find the maximum corner response nearby.
     * @param xy
     * @return
     */
    private Pnt2d findNearbyCorner(Pnt2d xy) {
        int K = pyramid.getLevelCount();

        CornerLocalMax lmax = null;
        // check pyramid levels from top to bottom for a local max in corner score:
        for (int k = K - 1; k >= 0; k--) {
            Pnt2d uv = pyramid.getLevelPosition(xy, k);
            int u0 = (int) Math.round(uv.getX());
            int v0 = (int) Math.round(uv.getY());
            lmax = findMaxCornerInNeighborhood(u0, v0, k);
            if (lmax != null) {
                break;  // finish when a suitable corner was found
            }
        }
        if (lmax == null) {
            System.out.println("no corner local max not found at any level!");
            return null;
        }
        System.out.println("CornerLocalMax found: " + lmax);

        return null;    // TODO:
    }

    // static int NEIGHBORHOOD_SEARCH_SIZE = 5;

    private final float[][] scratch5x5 = new float[5][5];

    /**
     * Extract the 5x5 image neighborhood centered at {@code (u0, v0)}.
     * Uses a scratch array to be read only!
     * @param fp the image
     * @param u0 the x-center
     * @param v0 the y-center
     * @return the neighborhood values
     */
    private float[][] getNeighborhood5x5(FloatProcessor fp, int u0, int v0) {
        for (int i = 0; i < 5; i++) {
            int u = u0 - 2 + i;
            for (int j = 0; j < 5; j++) {
                int v = v0 - 2 + j;
                scratch5x5[i][j] = fp.getf(u, v);
            }
        }
        return scratch5x5;
    }

    // Find
    CornerLocalMax findMaxCornerInNeighborhood(int u0, int v0, int level) {
        FloatProcessor Q = pyramid.getLevel(level).getCornerScore();
        float[][] S = getNeighborhood5x5(Q, u0, v0);;
        // search the inner 3x3 neighborhood of the 5x5 NH for a maximum score value
        int m = -1, n = -1;
        float qm = Float.NEGATIVE_INFINITY;
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                float q = S[i][j];
                if (q > qm) {
                    qm = q;
                    m = i;
                    n = j;
                }
            }
        }
        // scratch[m][n] = qmax is the max value, with m,n in {1,2,3}
        // check if scratch[m][n] is a local maximum
        // TODO: check also if minimum score is reached!
        boolean isLocalMax = isLocalMax8(S, m, n);

        int umax = u0 + m - 2;  // level coordinates
        int vmax = v0 + n - 2;
        System.out.printf("  max corner for (%d,%d) k=%d at (%d,%d) q=%.2f dist=(%d,%d) locMax=%b\n",
                u0, v0, level, umax, vmax, qm, umax-u0, vmax-v0, isLocalMax);

        return isLocalMax ? new CornerLocalMax(umax, vmax, level, qm) : null;
    }

    /**
     * Checks if {@code S[m][n]} is a local maximum (over all its 8
     * neighbors).
     */
    private static boolean isLocalMax8(float[][] S, int m, int n) {
        float qm = S[m][n];
        return
                qm > S[m-1][n-1] && qm > S[m][n-1] && qm > S[m+1][n-1] &&
                qm > S[m-1][n]   &&                   qm > S[m+1][n]   &&
                qm > S[m-1][n+1] && qm > S[m][n+1] && qm > S[m+1][n+1] ;
    }

    /**
     * Checks if {@code S[m][n]} is a local maximum (over only 4
     * neighbors).
     */
    private static boolean isLocalMax4(float[][] S, int m, int n) {
        float qm = S[m][n];
        return
                                    qm > S[m][n-1] &&
                qm > S[m-1][n]   &&                   qm > S[m+1][n]   &&
                                    qm > S[m][n+1] ;
    }

    // -----------------------------------------------------------------------

    void refineCorner(CornerLocalMax lmax, float[][] S) {
        // TODO: CONTINUE HERE, watch for coordinates in lmax & S!
    }



    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------

    @Deprecated
    private Pnt2d refineOneCorner(Pnt2d xy) {
        // find the coarsest level (kstart) with an acceptable corner score at this position:
        // System.out.printf("    refining corner %s\n", xy);
        int K = pyramid.getLevelCount();
        // System.out.printf("    K = %d\n", K);
        int kstart = -1;
        int u = -1, v = -1;
        float q = 0;
        float[] neighborhood = null;

        for (int k = K - 1; k >= 0; k--) {
            Pnt2d uv = pyramid.getLevelPosition(xy, k);
            u = (int) Math.round(uv.getX());
            v = (int) Math.round(uv.getY());
            // get the corner score:
            FloatProcessor Q = pyramid.getLevel(k).getCornerScore();
            q = pyramid.getLevel(k).getCornerScore().getf(u, v);
            // System.out.printf("    checking level %d, q=%.2f\n", k, q);
            if (q > CORNER_SCORE_THRESHOLD) {
                neighborhood = getNeighborhood(pyramid.getLevel(k).getCornerScore(),u, v);
                if (isLocalMax3x3(neighborhood)) {
                    kstart = k;
                    break;
                }
            }
        }
        if (kstart == -1) {
            System.out.println("   *** found no suitable corner for: " + xy + " q=" + q);
            return null;    // could not refine
        }

        // SubpixelMaxInterpolator interpolator = SubpixelMaxInterpolator.QuadraticTaylor.getInstance();
        SubpixelMaxInterpolator interpolator = SubpixelMaxInterpolator.QuadraticLeastSquares.getInstance();
        // (u, v, kstart) is the first position to check
        // float[] neighborhood = getNeighborhood(pyramid.getLevel(kstart).getCornerScore(),u, v);
        //if (isLocalMax(neighborhood)) {

        float[] xyz = interpolator.getMax(neighborhood);
        if (xyz != null) {
            Pnt2d xyR = Pnt2d.from(u + xyz[0], v + xyz[1]);
            System.out.println("   Refined corner: " + xy + " --> " + xyR + " at level " + kstart);
            return pyramid.getOriginalPosition(xyR, kstart);
        }
        else {
            System.out.println("   *** interpolator failed : " + xy + " at level " + kstart);
        }

        // }
        // else {
        //     System.out.println("   *** no local max, for : " + xy + " at level " + kstart);
        // }
        return null;
    }

    /*
     * Returned samples are arranged as follows:
     * 	s4 s3 s2
     *  s5 qm s1
     *  s6 s7 s8
     */
    private static float[] getNeighborhood(FloatProcessor Q, int u, int v) {
        int M = Q.getWidth();
        int N = Q.getHeight();
        if (u <= 0 || u >= M - 1 || v <= 0 || v >= N - 1) {
            return null;
        }
        else {
            final float[] q = (float[]) Q.getPixels();
            float[] s = new float[9];
            final int i0 = (v - 1) * M + u;
            final int i1 = v * M + u;
            final int i2 = (v + 1) * M + u;
            s[0] = q[i1];
            s[1] = q[i1 + 1];
            s[2] = q[i0 + 1];
            s[3] = q[i0];
            s[4] = q[i0 - 1];
            s[5] = q[i1 - 1];
            s[6] = q[i2 - 1];
            s[7] = q[i2];
            s[8] = q[i2 + 1];
            return s;
        }
    }

    private static boolean isLocalMax3x3(float[] s) {
        if (s == null) {
            return false;
        }
        else {
            final float qm = s[0];
            return	// check 8 neighbors of q0
                    qm > s[4] && qm > s[3] && qm > s[2] &&
                    qm > s[5] &&              qm > s[1] &&
                    qm > s[6] && qm > s[7] && qm > s[8] ;
        }
    }

}
