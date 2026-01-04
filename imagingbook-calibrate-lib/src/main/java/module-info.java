module imagingbook.calibrate.lib {
    requires java.desktop;
    requires imagingbook.core;
    requires imagingbook.common;

    requires org.apache.commons.math4.core;
    requires org.apache.commons.math4.legacy;
    requires org.apache.commons.numbers.arrays;
    requires org.apache.commons.geometry.euclidean;
    requires org.apache.commons.math4.legacy.core;
    requires org.apache.commons.math4.legacy.exception;
    requires org.apache.commons.numbers.complex;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    exports imagingbook.calibration;
    exports imagingbook.calibration.zhang.data;
    exports imagingbook.calibration.util;
    exports imagingbook.calibration.math3legacy;
    exports imagingbook.calibration.distortion;
    exports imagingbook.calibration.homography;
}