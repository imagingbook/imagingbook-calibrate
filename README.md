# Java/ImageJ Implementation of Zhang's Camera Calibration Algorithm #

This is a Java implementation of Zhang's camera calibration algorithm. 
It implements the geometric part only, i.e., does not include interest point detection and point correspondence calculations.
For details see this extensive [**report**](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation).

### Project Structure ###

This software is part of the [imagingbook project](https://imagingbook.com).
It is built with Maven and depends on 
[**ImageJ**](https://imagej.nih.gov/ij/), 
the [**imagingbook**](https://github.com/imagingbook/imagingbook-public) library and
[**Apache Commons Math**](http://commons.apache.org/proper/commons-math/).

This project consists of two sub-projects (Maven modules):
* `imagingbook-calibrate-lib`: the calibration library plus a small set of calibration test data,
* `imagingbook-calibrate-plugins`: various ImageJ demo plugins (embedded in a complete ImageJ setup).

### Stand-Alone Installation ###

* Download the [**latest release of this repository**](https://github.com/imagingbook/imagingbook-calibrate/releases/latest).
* Enter `imagingbook-calibrate-plugins`.
* Start ImageJ by double-clicking `ImageJ.exe` (Win) or launching `ij.jar` (Mac). This requires an installed Java runtime version 1.8 or higher.
* In ImageJ, use the `Plugins` menu to run the demo plugins (test images are loaded automatically).

### Use in an Existing ImageJ Environment ###

* Copy all JAR files from `imagingbook-calibrate-plugins/ImageJ/jars`.
* Copy the plugin `.class` files from `imagingbook-calibrate-plugins/ImageJ/plugins`.

### Documentation ###

* **JavaDoc: [imagingbook-calibrate](https://imagingbook.github.io/imagingbook-calibrate/javadoc/index.html)**
* **Report: [Zhang's Camera Calibration Algorithm: In-Depth Tutorial and Implementation](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation)**

