/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.math3legacy;

/**
 * Substitute for analogous exception in commons math3.
 */
public class NotARotationMatrixException extends RuntimeException {

    public NotARotationMatrixException(String s, Object ... parts) {    // UNFINISHED, check original!
        super(s);
    }
}
