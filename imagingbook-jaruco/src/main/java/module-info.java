module imagingbook.jaruco {
    requires com.fasterxml.jackson.databind;
    requires imagingbook.common;
    requires ij;
    requires org.apache.commons.math4.legacy;
    requires org.apache.commons.math4.legacy.core;
    requires imagingbook.core;
    requires com.github.librepdf.openpdf;
    //requires java.desktop; // needed for PdfGraphics2D

    exports imagingbook.jaruco;
    exports imagingbook.jaruco.pyramid;
    exports imagingbook.jaruco.obsolete;
    exports imagingbook.jaruco.util;
    exports imagingbook.jaruco.gui;
    exports imagingbook.jaruco.math;
}