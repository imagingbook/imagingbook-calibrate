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
    requires java.net.http;

    exports imagingbook.calibrate;
    exports imagingbook.calibrate.zhang.data;
    exports imagingbook.calibrate.util;
    exports imagingbook.calibrate.math3legacy;
    exports imagingbook.calibrate.distortion;
    exports imagingbook.calibrate.homography;
    exports imagingbook.calibrate.intrinsics;
    exports imagingbook.calibrate.extrinsics;
    exports imagingbook.calibrate.optimize;
    exports imagingbook.calibrate.plumbline;
    exports imagingbook.calibrate.optimize.support;
}