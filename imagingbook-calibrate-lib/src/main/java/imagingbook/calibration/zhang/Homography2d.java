/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang;

import imagingbook.common.math.Arithmetic;
import imagingbook.common.math.Matrix;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;

/**
 * A 2D homography represented by a 3x3 matrix.
 */
public class Homography2d extends Array2DRowRealMatrix {

    public Homography2d(double[][] H) {
        this(H, false);
    }

    public Homography2d(double[][] H, boolean normalize) {
        super(normalize ? normalize(H) : H);
    }

    /**
     * Scale all elements of H such that H(2,2) = 1.
     * Used for comparing homography matrices.
     * @param H a 3 x 3 homography matrix
     * @return the normalized matrix
     */
    private static double[][] normalize(double[][] H) {
        if (H.length != 3 || H[0].length != 3)
            throw new IllegalArgumentException("homography matrix is not of size 3 x 3");
        double h22 = H[2][2];
        if (Arithmetic.isZero(h22, 1e-15))
            throw new IllegalArgumentException("zero homography matrix element H(2,2)");
        return Matrix.multiply(1.0 / h22, H);
    }


}
