module imagingbook.calibrate.lib {
    requires java.desktop;
    requires imagingbook.core;
    requires imagingbook.common;

    requires org.apache.commons.math4.core;
    requires org.apache.commons.math4.legacy;
    requires org.apache.commons.numbers.arrays;
    requires org.apache.commons.geometry.euclidean;

    exports imagingbook.calibration.zhang.data;
    exports imagingbook.calibration.zhang;
    exports imagingbook.calibration.util;
    exports imagingbook.calibration.math3legacy;
}