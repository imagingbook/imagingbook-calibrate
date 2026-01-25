/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize.obsolete;

import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.AbstractCamera;
import imagingbook.calibrate.intrinsics.Camera;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.List;

public interface NonlinearOptimizer {

    public boolean optimize();
	public int getIterations();
    public int getEvaluations();
    public RealVector getResiduals();

    public AbstractCamera getFinalCamera();
    public List<ViewTransform> getFinalViews();
}
