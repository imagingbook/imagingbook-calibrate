package imagingbook.calibration.zhang.util;


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


@Deprecated
public class PlanarCalibrationPattern {
	
	
	/**
	 * This target is composed of a checkered chess board like squares.  Each corner of an interior square
	 * touches an adjacent square, but the sides are separated.  Only interior square corners provide
	 * calibration points.
	 *
	 * @param numCols Number of grid columns in the calibration target
	 * @param numRows Number of grid rows in the calibration target
	 * @param squareWidth How wide each square is.  Units are target dependent.
	 * @return Target description
	 */
	
	final int numCols;
	final int numRows;
	final double squareWidth;	// in arbitrary units
	final Rectangle2D[] rectangles;
	final Point2D[] points;
	
	
	public PlanarCalibrationPattern(int numRows, int numCols, double squareWidth) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.squareWidth = squareWidth;
		this.rectangles = new Rectangle2D[numRows * numCols];
		this.points = new Point2D[4 * numRows * numCols];
		initialize();
	}
	
	private void initialize() {
		double startX = 0, startY = 0;	// top-left corner
		int ri = 0, ci = 0;
		for (int row = 0; row < numRows; row++) {
			double y0 = startY + 2 * row * squareWidth;
			double y1 = y0 + squareWidth;
			for (int col = 0; col < numCols; col++) {
				double x0 = startX + 2 * col * squareWidth;
				double x1 = x0 + squareWidth;
				rectangles[ri++] = new Rectangle2D.Double(x0, y0, squareWidth, squareWidth);
				points[ci++] = new Point2D.Double(x0, y0);
				points[ci++] = new Point2D.Double(x1, y0);
				points[ci++] = new Point2D.Double(x1, y1);
				points[ci++] = new Point2D.Double(x0, y1);
			}
		}
	}
	
	public Rectangle2D[] getRectangles() {
		return rectangles;
	}
	
	public Point2D[] getPoints() {
		return points;
	}
	
	public static void main(String[] args) {
		PlanarCalibrationPattern pat = new PlanarCalibrationPattern(3, 4, 30.0);
		Point2D[] ps = pat.getPoints();
		System.out.println("Pattern as points: " + ps.length);
	}

}
