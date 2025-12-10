module imagingbook.jaruco {
    requires com.fasterxml.jackson.databind;
    requires imagingbook.common;
    requires ij;

    exports imagingbook.jaruco;
    exports imagingbook.jaruco.obsolete;
}