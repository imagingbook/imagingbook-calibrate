module imagingbook.calibrate.plugins {
    exports Calibration_Plugins_1;
    exports Calibration_Plugins_2;

    requires ij;
    requires java.desktop;
    requires imagingbook.calibrate.lib;
    requires imagingbook.common;
    requires imagingbook.core;
}