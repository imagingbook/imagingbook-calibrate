package imagingbook.calibration.data.zhang;

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

	private final static String BASEDIR = "DATA";
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
	
	@Override
	public String getRelativeDirectory() {
		return BASEDIR;
	}


	String getAbsolutePath() {
		return this.getClass().getResource("") + "/" + getRelativeDirectory() + "/" + getFileName();
	}

}
