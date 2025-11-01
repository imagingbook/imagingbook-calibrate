/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/

/*
Note: This class was ported from org.apache.commons.math3.geometry.euclidean.RotationOrder.java
(see original license above).
 */

package imagingbook.calibration.math3legacy;

import org.apache.commons.geometry.euclidean.threed.Vector3D;

/**
 * This class is a utility representing a rotation order specification
 * for Cardan or Euler angles specification.
 *
 * This class cannot be instanciated by the user. He can only use one
 * of the twelve predefined supported orders as an argument to either
 * the {@link Rotation#Rotation(RotationOrder,double,double,double)}
 * constructor or the {@link Rotation#getAngles} method.
 *
 * @since 1.2
 */
public final class RotationOrder {

    // Definitions copied from org.apache.commons.math3.geometry.euclidean.threed.Vector3D

    public static final Vector3D ZERO   = Vector3D.of(0, 0, 0);

    /** First canonical vector (coordinates: 1, 0, 0). */
    public static final Vector3D PLUS_I = Vector3D.of(1, 0, 0);

    /** Opposite of the first canonical vector (coordinates: -1, 0, 0). */
    public static final Vector3D MINUS_I = Vector3D.of(-1, 0, 0);

    /** Second canonical vector (coordinates: 0, 1, 0). */
    public static final Vector3D PLUS_J = Vector3D.of(0, 1, 0);

    /** Opposite of the second canonical vector (coordinates: 0, -1, 0). */
    public static final Vector3D MINUS_J = Vector3D.of(0, -1, 0);

    /** Third canonical vector (coordinates: 0, 0, 1). */
    public static final Vector3D PLUS_K = Vector3D.of(0, 0, 1);

    /** Opposite of the third canonical vector (coordinates: 0, 0, -1).  */
    public static final Vector3D MINUS_K = Vector3D.of(0, 0, -1);

    // CHECKSTYLE: stop ConstantName
    /** A vector with all coordinates set to NaN. */
    public static final Vector3D NaN = Vector3D.of(Double.NaN, Double.NaN, Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** A vector with all coordinates set to positive infinity. */
    public static final Vector3D POSITIVE_INFINITY =
            Vector3D.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /** A vector with all coordinates set to negative infinity. */
    public static final Vector3D NEGATIVE_INFINITY =
            Vector3D.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    // ---------------------------------------------------------------------

    /** Set of Cardan angles.
     * this ordered set of rotations is around X, then around Y, then
     * around Z
     */
    public static final RotationOrder XYZ =
            new RotationOrder("XYZ", PLUS_I, PLUS_J, PLUS_K);

    /** Set of Cardan angles.
     * this ordered set of rotations is around X, then around Z, then
     * around Y
     */
    public static final RotationOrder XZY =
            new RotationOrder("XZY", PLUS_I, PLUS_K, PLUS_J);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Z
     */
    public static final RotationOrder YXZ =
            new RotationOrder("YXZ", PLUS_J, PLUS_I, PLUS_K);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around X
     */
    public static final RotationOrder YZX =
            new RotationOrder("YZX", PLUS_J, PLUS_K, PLUS_I);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Y
     */
    public static final RotationOrder ZXY =
            new RotationOrder("ZXY", PLUS_K, PLUS_I, PLUS_J);

    /** Set of Cardan angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around X
     */
    public static final RotationOrder ZYX =
            new RotationOrder("ZYX", PLUS_K, PLUS_J, PLUS_I);

    /** Set of Euler angles.
     * this ordered set of rotations is around X, then around Y, then
     * around X
     */
    public static final RotationOrder XYX =
            new RotationOrder("XYX", PLUS_I, PLUS_J, PLUS_I);

    /** Set of Euler angles.
     * this ordered set of rotations is around X, then around Z, then
     * around X
     */
    public static final RotationOrder XZX =
            new RotationOrder("XZX", PLUS_I, PLUS_K, PLUS_I);

    /** Set of Euler angles.
     * this ordered set of rotations is around Y, then around X, then
     * around Y
     */
    public static final RotationOrder YXY =
            new RotationOrder("YXY", PLUS_J, PLUS_I, PLUS_J);

    /** Set of Euler angles.
     * this ordered set of rotations is around Y, then around Z, then
     * around Y
     */
    public static final RotationOrder YZY =
            new RotationOrder("YZY", PLUS_J, PLUS_K, PLUS_J);

    /** Set of Euler angles.
     * this ordered set of rotations is around Z, then around X, then
     * around Z
     */
    public static final RotationOrder ZXZ =
            new RotationOrder("ZXZ", PLUS_K, PLUS_I, PLUS_K);

    /** Set of Euler angles.
     * this ordered set of rotations is around Z, then around Y, then
     * around Z
     */
    public static final RotationOrder ZYZ =
            new RotationOrder("ZYZ", PLUS_K, PLUS_J, PLUS_K);

    /** Name of the rotations order. */
    private final String name;

    /** Axis of the first rotation. */
    private final Vector3D a1;

    /** Axis of the second rotation. */
    private final Vector3D a2;

    /** Axis of the third rotation. */
    private final Vector3D a3;

    /** Private constructor.
     * This is a utility class that cannot be instantiated by the user,
     * so its only constructor is private.
     * @param name name of the rotation order
     * @param a1 axis of the first rotation
     * @param a2 axis of the second rotation
     * @param a3 axis of the third rotation
     */
    private RotationOrder(final String name, final Vector3D a1, final Vector3D a2, final Vector3D a3) {
        this.name = name;
        this.a1   = a1;
        this.a2   = a2;
        this.a3   = a3;
    }

    /** Get a string representation of the instance.
     * @return a string representation of the instance (in fact, its name)
     */
    @Override
    public String toString() {
        return name;
    }

    /** Get the axis of the first rotation.
     * @return axis of the first rotation
     */
    public Vector3D getA1() {
        return a1;
    }

    /** Get the axis of the second rotation.
     * @return axis of the second rotation
     */
    public Vector3D getA2() {
        return a2;
    }

    /** Get the axis of the second rotation.
     * @return axis of the second rotation
     */
    public Vector3D getA3() {
        return a3;
    }

}

