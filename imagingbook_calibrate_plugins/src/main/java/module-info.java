module imagingbook_calibrate_plugins {
    exports Calibration_Plugins_1;
    exports Calibration_Plugins_2;

    requires ij;
    requires java.desktop;
    requires org.apache.commons.geometry.euclidean;
    requires imagingbook.core;
    requires imagingbook.common;
    requires imagingbook.calibrate.lib;
    requires imagingbook.jaruco;
}