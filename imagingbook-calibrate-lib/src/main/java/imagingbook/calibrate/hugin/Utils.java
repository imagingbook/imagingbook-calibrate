/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.hugin;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.common.geometry.basic.Pnt2d;

import java.util.ArrayList;
import java.util.List;

public final class Utils {
    private Utils() {}

    public static List<List<Pnt2d>> sampleStraightLines(DistortionModel dist) {
        return sampleStraightLines(dist, 11, 11);
    }

    public static List<List<Pnt2d>> sampleStraightLines(DistortionModel dist, int hor, int ver) {
        List<List<Pnt2d>> lines = new ArrayList<>();
        // horizontal lines:
        for (int j = 0; j < hor; j++) {
            List<Pnt2d> ln = new ArrayList<>();
            double y = -0.5 + j * 0.1;
            for (int i = 0; i <= 10; i++) {
                double x = -0.5 + i * 0.1;
                ln.add(dist.warp(Pnt2d.from(x, y)));
            }
            lines.add(ln);
        }
        // vertical lines:
        for (int j = 0; j < ver; j++) {
            List<Pnt2d> ln = new ArrayList<>();
            double x = -0.5 + j * 0.1;
            for (int i = 0; i <= 10; i++) {
                double y = -0.5 + i * 0.1;
                ln.add(dist.warp(Pnt2d.from(x, y)));
            }
            lines.add(ln);
        }
        return lines;
    }
}
