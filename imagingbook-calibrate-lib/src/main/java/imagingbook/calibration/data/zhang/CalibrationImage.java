package imagingbook.calibration.data.zhang;

import imagingbook.core.resource.ImageResource;

public enum CalibrationImage implements ImageResource {
		CalibIm1_png("CalibIm1.png"),
		CalibIm2_png("CalibIm2.png"),
		CalibIm3_png("CalibIm3.png"),
		CalibIm4_png("CalibIm4.png"),
		CalibIm5_png("CalibIm5.png"),
		marked1_png("marked1.png"),
		marked2_png("marked2.png"),
		marked3_png("marked3.png"),
		marked4_png("marked4.png"),
		marked5_png("marked5.png"),
		CalibImageStack_tif("CalibImageStack.tif");

	private final static String BASEDIR = "DATA";
	private final String filename;
	
	CalibrationImage(String filename) {
		this.filename = filename;
	}
	
	@Override
	public String getFileName() {
		return filename;
	}
	
	@Override
	public String getRelativeDirectory() {
		return BASEDIR;
	}
	
	
	public static void main(String[] args) {
		for (ImageResource ir : CalibrationImage.values()) {
			System.out.println(ir.getImage().toString());
		}
	}

}
