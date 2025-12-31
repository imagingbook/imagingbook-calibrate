package imagingbook.jaruco.marker;

import com.lowagie.text.pdf.PdfGraphics2D;
import ij.process.ByteProcessor;
import imagingbook.common.util.bits.BitVector;
import imagingbook.jaruco.dict.ArucoDictionary;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Describes the geometry of a Aruco dictionary marker for visualization.
 */
public class ArucoMarker {

    private final int id;
    private final int rot;
    private final ArucoDictionary dictionary;
    private final int borderBits;


    /**
     * Constructor.
     *
     * @param id the marker's id in the associated {@code dictionary}
     * @param dictionary the {@link ArucoDictionary} instance
     * @param borderBits the number of additional (black) border bits
     */
    public ArucoMarker(int id, int rot, ArucoDictionary dictionary, int borderBits) {
        this.id = id;
        this.rot = rot;
        this.dictionary = dictionary;
        this.borderBits = borderBits;
    }

    protected ArucoMarker(ArucoMarker marker) {
        this(marker.id, marker.rot, marker.dictionary, marker.borderBits);
    }

    // --------------------------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public int getRotation() {
        return rot;
    }

    /**
     * Creates a marker image of the specified dictionary entry with a
     * surrounding black  of the specified width.
     * @return an image of the specified marker with 1 pixel per code bit
     */
    public ByteProcessor getImage() {
        int n = dictionary.getMarkerSize();
        int npix = n + 2 * borderBits;
        BitVector bitcode = dictionary.getBits(id, rot);
        ByteProcessor ip = new ByteProcessor(npix, npix);
        int k = 0;
        for (int v = 0; v < n; v++) {
            for (int u = 0; u < n; u++) {
                ip.set(u + borderBits, v + borderBits, bitcode.getBit(k) ? 0xFF : 0);
                k++;
            }
        }
        return ip;
    }

    // --------------------------------------------------------------------------------------------

    /** Overlap for white boxes to avoid "hairline gap" problem in PDFs. */
    static final double PDF_OVERLAP = 0.0005;   // fraction of marker size 0.0002

    /**
     * Draws this marker to a {@link Graphics2D} canvas. Positions
     * and dimensions are wrt the canvas's coordinate system. If the canvas is
     * a {@link PdfGraphics2D} a small overlap is applied to the white boxes to
     * avoid hairline gaps between white squares on black background.
     *
     * @param g a {@link Graphics2D} canvas
     * @param x0 the marker's x-position (upper-left corner)
     * @param y0 the marker's y-position (upper-left corner)
     * @param markerWidth the width and height of the marker (including its boundary)
     */
    public void drawTo(Graphics2D g, double x0, double y0, double markerWidth) {
        BitVector bitcode = dictionary.getBits(id, rot);
        int innerBits = dictionary.getMarkerSize();
        int totalBits = innerBits + 2 * borderBits;
        double scale = markerWidth / totalBits; // scale factor to enlarge one bit

        // draw the surrounding square black
        double wOuter = totalBits * scale;
        g.setStroke(new BasicStroke(0));
        g.setColor(Color.black);
        g.fill(new Rectangle2D.Double(x0, y0, wOuter, wOuter));

        double overlap = (g instanceof PdfGraphics2D) ? PDF_OVERLAP * markerWidth : 0;

        // draw the active bits as white squares (with micro-overlap in PDFs):
        g.setColor(Color.white);
        int i = 0;
        for (int v = 0; v < innerBits; v++) {
            for (int u = 0; u < innerBits; u++) {
                if (bitcode.getBit(i)) {
                    double x = x0 + (borderBits + u) * scale - overlap;
                    double y = y0 + (borderBits + v) * scale - overlap;
                    double w = 1 * scale + 2 * overlap;
                    g.fill(new Rectangle2D.Double(x, y , w, w));
                }
                i++;
            }
        }
    }

}