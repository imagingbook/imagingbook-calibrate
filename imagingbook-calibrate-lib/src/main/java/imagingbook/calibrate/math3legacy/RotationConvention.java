/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/

/*
Note: This class was ported from org.apache.commons.math3.geometry.euclidean.RotationConvention.java
(see original license above).
 */

package imagingbook.calibrate.math3legacy;


/**
 * This enumerates is used to differentiate the semantics of a rotation.
 * @see Rotation
 * @since 3.6
 */
public enum RotationConvention {

    /** Constant for rotation that have the semantics of a vector operator.
     * <p>
     * According to this convention, the rotation moves vectors with respect
     * to a fixed reference frame.
     * </p>
     * <p>
     * This means that if we define rotation r is a 90 degrees rotation around
     * the Z axis, the image of vector {@code Vector3D.Unit.PLUS_X} would be
     * {@code Vector3D.Unit.PLUS_Y}, the image of vector {@code Vector3D.Unit.PLUS_Y}
     * would be {@code Vector3D.Unit.MINUS_X}, the image of vector {@code Vector3D.Unit.PLUS_Z}
     * would be {@code Vector3D.Unit.PLUS_Z}, and the image of vector with coordinates (1, 2, 3)
     * would be vector (-2, 1, 3). This means that the vector rotates counterclockwise.
     * </p>
     * <p>
     * This convention was the only one supported by Apache Commons Math up to version 3.5.
     * </p>
     * <p>
     * The difference with {@link #FRAME_TRANSFORM} is only the semantics of the sign
     * of the angle. It is always possible to create or use a rotation using either
     * convention to really represent a rotation that would have been best created or
     * used with the other convention, by changing accordingly the sign of the
     * rotation angle. This is how things were done up to version 3.5.
     * </p>
     */
    VECTOR_OPERATOR,

    /** Constant for rotation that have the semantics of a frame conversion.
     * <p>
     * According to this convention, the rotation considered vectors to be fixed,
     * but their coordinates change as they are converted from an initial frame to
     * a destination frame rotated with respect to the initial frame.
     * </p>
     * <p>
     * This means that if we define rotation r is a 90 degrees rotation around
     * the Z axis, the image of vector {@code Vector3D.Unit.PLUS_X} would be
     * {@code Vector3D.Unit.MINUS_Y}, the image of vector {@code Vector3D.Unit.PLUS_Y}
     * would be {@code Vector3D.Unit.PLUS_X}, the image of vector {@code Vector3D.Unit.PLUS_Z}
     * would be {@code Vector3D.Unit.PLUS_Z}, and the image of vector with coordinates (1, 2, 3)
     * would be vector (2, -1, 3). This means that the coordinates of the vector rotates
     * clockwise, because they are expressed with respect to a destination frame that is rotated
     * counterclockwise.
     * </p>
     * <p>
     * The difference with {@link #VECTOR_OPERATOR} is only the semantics of the sign
     * of the angle. It is always possible to create or use a rotation using either
     * convention to really represent a rotation that would have been best created or
     * used with the other convention, by changing accordingly the sign of the
     * rotation angle. This is how things were done up to version 3.5.
     * </p>
     */
    FRAME_TRANSFORM;

    public static final RotationConvention DEFAULT = VECTOR_OPERATOR;

}

