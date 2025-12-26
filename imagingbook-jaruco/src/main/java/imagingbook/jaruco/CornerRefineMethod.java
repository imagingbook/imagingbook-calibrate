package imagingbook.jaruco;

@Deprecated
public enum CornerRefineMethod {
    /**
     * Tag and corners detection based on the ArUco approach
     */
    CORNER_REFINE_NONE,
    /**
     * ArUco approach and refine the corners locations using corner subpixel accuracy
     */
    CORNER_REFINE_SUBPIX,
    /**
     * ArUco approach and refine the corners locations using the contour-points line fitting
     */
    CORNER_REFINE_CONTOUR,
    /**
     * Tag and corners detection based on the AprilTag 2 approach @cite wang2016iros
     */
    CORNER_REFINE_APRILTAG
}
