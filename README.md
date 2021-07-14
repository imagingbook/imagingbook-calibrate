# Java/ImageJ implementation of Zhang's camera calibration algorithm #

This Java library implements the geometric part of Zhang's camera calibration algorithm. 
It does not include interest point detection and point correspondence calculations.
See this extensive [**report**](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation) for details.

This implementation is part of the [imagingbook project](https://imagingbook.com).
It is built with Maven and depends on 
[**ImageJ**](https://imagej.nih.gov/ij/), 
the [**imagingbook-common**](https://github.com/imagingbook/imagingbook-public) library and
[**Apache Commons Math**](http://commons.apache.org/proper/commons-math/).


### Stand-Alone Installation ###

* Clone this repository (which includes a complete ImageJ environment).
* Launch ImageJ (requires a Java runtime version 1.8 or higher).
* Use the `Plugins` menu to run the demo plugins (test images are loaded automatically).

### Use in an Existing ImageJ Environment ###

* Copy all JAR files from `ImageJ/jars`.
* Copy the plugin `.class` files from `ImageJ/plugins/calibration_demos`.

### Documentation ###

* **[JavaDoc](https://imagingbook.github.io/imagingbook-calibrate/javadoc/index.html)**
* **[Report](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation)**

