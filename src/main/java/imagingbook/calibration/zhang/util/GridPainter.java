package imagingbook.calibration.zhang.util;

import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Helper class for drawing projections of the calibration target.
 * @author W. Burger
 *
 */
public class GridPainter {

	// parameters to modify:
	public Color lineCol = Color.gray;
	public Color[] cornerColors = {Color.red, Color.green, Color.blue, Color.gray};
	public int crossSize = 2;
	
	
	private final ImageProcessor ip;
	
	public GridPainter(ImageProcessor ip) {
		this.ip = ip;
	}

	public void drawSquares(Point2D[] cornerPnts) {
		for (int i = 0; i < cornerPnts.length; i += 4) {
			Point2D[] corners = new Point2D[4];
			for (int j = 0; j < 4; j++) {	
				corners[j] = cornerPnts[i + j];	//TODO: check array bounds!
			}
			drawSquare(corners);
		}
	}
	
	public void drawSquare(Point2D[] cornerPnts) {
		for (int j = 0; j < 4; j++) {
			drawLine(cornerPnts[j], cornerPnts[(j + 1) % 4]);
		}	
		for (int j = 0; j < 4; j++) {
			drawCorner(cornerPnts[j], cornerColors[j]);
		}
	}

	public void drawLine(Point2D p1, Point2D p2) {
		int u1 = (int) Math.round(p1.getX());
		int v1 = (int) Math.round(p1.getY());
		int u2 = (int) Math.round(p2.getX());
		int v2 = (int) Math.round(p2.getY());
		if (lineCol != null) 
			ip.setColor(lineCol);
		ip.drawLine(u1, v1, u2, v2);
	}

	public void drawCorner(Point2D p, Color col) {
		int u = (int) Math.round(p.getX());
		int v = (int) Math.round(p.getY());
		if (col != null) 
			ip.setColor(col);
		ip.drawLine(u - crossSize, v, u + crossSize, v);
		ip.drawLine(u, v - crossSize, u, v + crossSize);
	}

}
