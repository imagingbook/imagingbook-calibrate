# Java/ImageJ implementation of Zhang's camera calibration algorithm #

This is a Java implementation of Zhang's camera calibration algorithm. 
It implements the geometric part only, i.e., does not include interest point detection and point correspondence calculations.
For details see this extensive [**report**](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation).

### Project Structure ###

This software is part of the [imagingbook project](https://imagingbook.com).
It is built with Maven and depends on 
[**ImageJ**](https://imagej.nih.gov/ij/), 
the [**imagingbook-common**](https://github.com/imagingbook/imagingbook-public) library and
[**Apache Commons Math**](http://commons.apache.org/proper/commons-math/).

This project consists of two sub-projects (Maven modules):
* `imagingbook-calibrate-lib`: the calibration library,
* `imagingbook-calibrate-data`: a small set of calibration test data,
* `imagingbook-calibrate-plugins`: various ImageJ demo plugins (embedded in a complete ImageJ setup).

### Stand-Alone Installation ###

* Clone this repository.
* Enter `imagingbook-calibrate-plugins`.
* Launch ImageJ (requires a Java runtime version 1.8 or higher).
* In ImageJ, use the `Plugins` menu to run the demo plugins (test images are loaded automatically).

### Use in an Existing ImageJ Environment ###

* Copy all JAR files from `imagingbook-calibrate-plugins/ImageJ/jars`.
* Copy the plugin `.class` files from `imagingbook-calibrate-plugins/ImageJ/plugins`.

### Use with Maven

To use the ``imagingbook-calibrate-lib`` library and ``imagingbook-calibrate-data`` resources in your Maven project, 
add the following lines to your ``pom.xml`` file:
````
<repositories>
    <repository>
	<id>imagingbook-maven-repository</id>
    	<url>https://raw.github.com/imagingbook/imagingbook-maven-repository/master</url>
    	<layout>default</layout>
    </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.imagingbook</groupId>
    <artifactId>imagingbook-calibrate-lib</artifactId>
    <version>2.0-SNAPSHOT</version>
  </dependency>
  <dependency>
    <groupId>com.imagingbook</groupId>
    <artifactId>imagingbook-calibrate-data</artifactId>
    <version>2.0-SNAPSHOT</version>
  </dependency>
  <!-- other dependencies ... -->
</dependencies>
````
The above setup refers to version ``2.0-SNAPSHOT``, check the [ImagingBook Maven Repository](https://github.com/imagingbook/imagingbook-maven-repository/tree/master/com/imagingbook/) for the most recent version.
See also [this demo repository](https://github.com/imagingbook/imagingbook-maven-demo-project) for how to set up a simple ImageJ project with Maven.

### Documentation ###

* **JavaDoc: [imagingbook-calibrate-lib](https://imagingbook.github.io/imagingbook-calibrate/javadoc/imagingbook-calibrate-lib/index.html?overview-summary.html), [imagingbook-calibrate-plugins](https://imagingbook.github.io/imagingbook-calibrate/javadoc/imagingbook-calibrate-plugins/index.html?overview-summary.html)**
* **Report: [Zhang's Camera Calibration Algorithm: In-Depth Tutorial and Implementation](https://www.researchgate.net/publication/303233579_Zhang%27s_Camera_Calibration_Algorithm_In-Depth_Tutorial_and_Implementation)**

