/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.data;

import java.util.ArrayList;
import java.util.List;

/*
 * Transformations for Zhang's sample data 
 * (http://research.microsoft.com/en-us/um/people/zhang/calib/Calibration/Calib.txt)
 * 
Camera:
832.5 0.204494 832.53 303.959 206.585
-0.228601 0.190353

 * 
 * View 1:
0.992759 -0.026319 0.117201
0.0139247 0.994339 0.105341
-0.11931 -0.102947 0.987505
-3.84019 3.65164 12.791

View 2:
0.997397 -0.00482564 0.0719419
0.0175608 0.983971 -0.17746
-0.0699324 0.178262 0.981495
-3.71693 3.76928 13.1974

View 3:
0.915213 -0.0356648 0.401389
-0.00807547 0.994252 0.106756
-0.402889 -0.100946 0.909665
-2.94409 3.77653 14.2456

View 4:
0.986617 -0.0175461 -0.16211
0.0337573 0.994634 0.0977953
0.159524 -0.101959 0.981915
-3.40697 3.6362 12.4551

View 5:
0.967585 -0.196899 -0.158144
0.191542 0.980281 -0.0485827
0.164592 0.0167167 0.98622
-4.07238 3.21033 14.3441
*/


abstract class CameraViews {
	
	static List<double[][]> AllViewMatrices = new ArrayList<double[][]>();
	
	static {
		AllViewMatrices.add(makeViewMatrix (	// view 1
				0.992759, -0.026319, 0.117201,
				0.0139247, 0.994339, 0.105341,
				-0.11931, -0.102947, 0.987505,
				-3.84019, 3.65164, 12.791
				));
		AllViewMatrices.add(makeViewMatrix  (	// view 2
				0.997397, -0.00482564, 0.0719419,
				0.0175608, 0.983971, -0.17746,
				-0.0699324, 0.178262, 0.981495,
				-3.71693, 3.76928, 13.1974));
		AllViewMatrices.add(makeViewMatrix  (	// view 3
				0.915213, -0.0356648, 0.401389,
				-0.00807547, 0.994252, 0.106756,
				-0.402889, -0.100946, 0.909665,
				-2.94409, 3.77653, 14.2456));
		AllViewMatrices.add(makeViewMatrix (	// view 4
				0.986617, -0.0175461, -0.16211,
				0.0337573, 0.994634, 0.0977953,
				0.159524, -0.101959, 0.981915,
				-3.40697, 3.6362, 12.4551));
		AllViewMatrices.add(makeViewMatrix (	// view 5
				0.967585, -0.196899, -0.158144,
				0.191542, 0.980281, -0.0485827,
				0.164592, 0.0167167, 0.98622,
				-4.07238, 3.21033, 14.3441));
	}
	
	private static double[][] makeViewMatrix(
			double r11, double r12, double r13,
			double r21, double r22, double r23,
			double r31, double r32, double r33,
			double t1, double t2, double t3) {
		double[][] RT = {
				{r11, r12, r13, t1},
				{r21, r22, r23, t2},
				{r31, r32, r33, t3}};
		
		return RT;
	}
	
	public static double[][] getViewMatrix(int vn)  {	// assumes vn = 0,...,4
		if (vn < 0 || vn >= AllViewMatrices.size()) {
			return null;
		}
		else {
			return AllViewMatrices.get(vn);
		}
	}
}
