module imagingbook.calibrate.plugins {
    exports Calibration_Demos;
    exports Calibration_Demos_More;
    exports Obsolete;

    requires ij;
    requires java.desktop;
    requires imagingbook.calibrate.lib;
    requires imagingbook.common;
    requires imagingbook.core;
}