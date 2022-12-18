/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.zhang.data;

import imagingbook.core.resource.ImageResource;

public enum CalibrationImage implements ImageResource {
	CalibImageStack("CalibImageStack.tif"),
	CalibIm1,
	CalibIm2,
	CalibIm3,
	CalibIm4,
	CalibIm5,
	marked1,
	marked2,
	marked3,
	marked4,
	marked5;

	// private final static String BASEDIR = "DATA";
	private final String filename;

	CalibrationImage() {
		this(null);
	}

	CalibrationImage(String filename) {
		this.filename = filename;
	}

	@Override
	public String getFileName() {
		return (this.filename != null) ? this.filename : this.autoName();
	}
	
	// @Override
	// public String getRelativeDirectory() {
	// 	return BASEDIR;
	// }


	String getAbsolutePath() {
		return this.getClass().getResource("") + "/" + getRelativeDirectory() + "/" + getFileName();
	}

}
