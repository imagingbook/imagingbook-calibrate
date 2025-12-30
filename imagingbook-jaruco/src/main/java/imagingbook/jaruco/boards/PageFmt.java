package imagingbook.jaruco.boards;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.RectangleReadOnly;

import java.nio.file.Path;

/**
 * Bridge class to avoid import of {@link com.lowagie.text.Rectangle} when calling
 * {@link AbstractBoard#saveAsPdf(Path, PageFmt, boolean)}. Custom page formats can be created by
 * <pre>
 *     new PageFmt(width, height); </pre> with dimensions in mm.
 * See OpenPDF {@link PageSize} for additional standard formats.
 *
 * @param width document width (in millimeters)
 * @param height document height (in millimeters)
 */
public record PageFmt(double width, double height) {

    // instantiate from OpenPDF Rectangle
    PageFmt(Rectangle pageSize) {
        this(pageSize.getWidth(), pageSize.getHeight());
    }

    // returns the OpenPDF rectangle
    Rectangle getRectangle() {
        return new RectangleReadOnly((float) width, (float) height);
    }

    // standard page formats:
    public static final PageFmt A4_Portrait = new PageFmt(PageSize.A4);
    public static final PageFmt A4_Landscape = new PageFmt(PageSize.A4.rotate());
    public static final PageFmt A3_Portrait = new PageFmt(PageSize.A3);
    public static final PageFmt A3_Landscape = new PageFmt(PageSize.A3.rotate());

}
