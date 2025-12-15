module imagingbook.jaruco {
    requires com.fasterxml.jackson.databind;
    requires imagingbook.common;
    requires ij;
    requires org.apache.commons.math4.legacy;

    exports imagingbook.jaruco;
    exports imagingbook.jaruco.pyramid;
    exports imagingbook.jaruco.obsolete;
}