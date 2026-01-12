/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.math3legacy;

import java.lang.reflect.Field;
import org.junit.Test;

import static org.junit.Assert.*;

// Class ported from org.apache.commons.math3.geometry.euclidean.threed

public class RotationOrderTest {

    @Test
    public void testName() {

        RotationOrder[] orders = {
                RotationOrder.XYZ, RotationOrder.XZY, RotationOrder.YXZ,
                RotationOrder.YZX, RotationOrder.ZXY, RotationOrder.ZYX,
                RotationOrder.XYX, RotationOrder.XZX, RotationOrder.YXY,
                RotationOrder.YZY, RotationOrder.ZXZ, RotationOrder.ZYZ
        };

        for (int i = 0; i < orders.length; ++i) {
            assertEquals(getFieldName(orders[i]), orders[i].toString());
        }

    }

    private String getFieldName(RotationOrder order) {
        try {
            Field[] fields = RotationOrder.class.getFields();
            for (int i = 0; i < fields.length; ++i) {
                if (fields[i].get(null) == order) {
                    return fields[i].getName();
                }
            }
        } catch (IllegalAccessException iae) {
            // ignored
        }
        return "unknown";
    }


}