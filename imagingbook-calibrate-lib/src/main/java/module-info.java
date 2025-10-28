module imagingbook.calibrate.lib {
    requires java.desktop;
    requires imagingbook.common;
    requires imagingbook.core;
    requires org.apache.commons.math4.legacy;
    requires org.apache.commons.geometry.euclidean;
    requires org.apache.commons.numbers.quaternion;
    requires org.apache.commons.math4.core;

    exports imagingbook.calibration.zhang.data;
    exports imagingbook.calibration.zhang;
    exports imagingbook.calibration.zhang.util;
}