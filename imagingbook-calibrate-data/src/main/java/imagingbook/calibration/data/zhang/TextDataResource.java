package imagingbook.calibration.data.zhang;

import imagingbook.core.resource.NamedResource;

public enum TextDataResource implements NamedResource {
		CalibrationResultZhangWithDistortion_txt("Calibration-result-zhang-withdistortion.txt"),
		Model_txt("Model.txt"),
		AllCalibrationResultsEasyCalib_txt("all-calibration-results-EasyCalib.txt"),
		AllCalibrationResults_txt("all-calibration-results.txt"),
		Data1_txt("data1.txt"),
		Data2_txt("data2.txt"),
		Data3_txt("data3.txt"),
		Data4_txt("data4.txt"),
		Data5_txt("data5.txt");

	private final static String BASEDIR = "DATA/";
	private final String relPath;
	
	private TextDataResource(String relPath) {
		this.relPath = BASEDIR + relPath;
	}
	
	@Override
	public String getRelativePath() {
		return relPath;
	}
	
	public static void main(String[] args) {
		for (NamedResource nr : TextDataResource.values()) {
			System.out.println(nr.getURL());
		}
	}

}
