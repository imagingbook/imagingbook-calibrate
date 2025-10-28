/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.geom3d;

// import org.apache.commons.math3.exception.MathIllegalStateException;
// import org.apache.commons.math3.exception.util.LocalizedFormats;

/** This class represents exceptions thrown while extractiong Cardan
 * or Euler angles from a rotation.

 * @since 1.2
 */
public class CardanEulerSingularityException
        extends IllegalStateException {

    static final String CARDAN_ANGLES_SINGULARITY = "Cardan angles singularity";
    static final String EULER_ANGLES_SINGULARITY = "Euler angles singularity";

    /** Serializable version identifier */
    private static final long serialVersionUID = -1360952845582206770L;

    /**
     * Simple constructor.
     * build an exception with a default message.
     * @param isCardan if true, the rotation is related to Cardan angles,
     * if false it is related to EulerAngles
     */
    public CardanEulerSingularityException(boolean isCardan) {
        super(isCardan ? CARDAN_ANGLES_SINGULARITY : EULER_ANGLES_SINGULARITY);
    }

}
