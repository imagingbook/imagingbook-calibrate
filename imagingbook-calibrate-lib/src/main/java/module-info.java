module imagingbook.calibrate.lib {
    requires java.desktop;
    requires imagingbook.common;
    requires imagingbook.core;

    exports imagingbook.calibration.data.zhang;
    exports imagingbook.calibration.zhang;
    exports imagingbook.calibration.zhang.util;
}