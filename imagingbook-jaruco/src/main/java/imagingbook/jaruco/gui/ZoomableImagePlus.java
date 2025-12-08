package imagingbook.jaruco.gui;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;

/**
 * Usage:
 * new ZoomableImagePlus("title", ip).show(2.0);
 */
public class ZoomableImagePlus extends ImagePlus {

    public ZoomableImagePlus(String title, ImageProcessor ip) {
        super(title, ip);
    }

    public void show(double zoomFactor) {
        if (isVisible()) {
            super.show(); // fallback
            return;
        }

        // Build a custom window instead of ImagePlus.show()
        ImageWindow win = new ImageWindow(this);
        ImageCanvas canvas = win.getCanvas();

        double z = canvas.getMagnification();
        int cx = 0, cy = 0;

        while (z < zoomFactor) { canvas.zoomIn(cx, cy);  z = canvas.getMagnification(); }
        while (z > zoomFactor) { canvas.zoomOut(cx, cy); z = canvas.getMagnification(); }
    }
}

