package imagingbook.jaruco.gui;

// Thanks to: https://chatgpt.com/share/6936f7ff-eeec-8006-88d8-9f2fc68d4561

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

public final class MoreImageJUtils {

    private MoreImageJUtils() {} // utility class

    /**
     * Shows an ImagePlus at a given zoom factor and optional window position.
     *
     * @param imp          the image to display
     * @param zoomFactor   the desired magnification (e.g. 2.0 = 200%)
     * @param x            window X position (use -1 for “don't set”)
     * @param y            window Y position (use -1 for “don't set”)
     * @return the created ImageWindow
     */
    public static ImageWindow show(ImagePlus imp, double zoomFactor, int x, int y) {
        ImageWindow win = new ImageWindow(imp);
        ImageCanvas canvas = win.getCanvas();

        // --- set zoom ---
        double z = canvas.getMagnification();
        int cx = 0, cy = 0;

        while (z < zoomFactor) { canvas.zoomIn(cx, cy);  z = canvas.getMagnification(); }
        while (z > zoomFactor) { canvas.zoomOut(cx, cy); z = canvas.getMagnification(); }

        // --- set window position ---
        if (x >= 0 && y >= 0)
            win.setLocation(x, y);

        return win;
    }

    /** Convenience overload: zoom only. */
    public static ImageWindow show(ImagePlus imp, double zoomFactor) {
        return show(imp, zoomFactor, -1, -1);
    }
}

