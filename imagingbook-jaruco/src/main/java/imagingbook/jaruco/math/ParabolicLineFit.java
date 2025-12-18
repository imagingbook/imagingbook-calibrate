package imagingbook.jaruco.math;

import imagingbook.common.geometry.basic.Pnt2d;
import imagingbook.jaruco.util.Polygons;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DecompositionSolver;
import org.apache.commons.math4.legacy.linear.QRDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParabolicLineFit {
    int n;
    public RealVector A, B;
    public RealVector M;
    public RealVector u;
    public RealVector v;
    public double d;
    public double a, c;

    public ParabolicLineFit(List<Pnt2d> pts) {
        this.n = pts.size();
        this.A = new ArrayRealVector(pts.get(0).toDoubleArray());
        this.B = new ArrayRealVector(pts.get(n-1).toDoubleArray());
        this.M = A.add(B).mapMultiply(0.5);
        RealVector BmA = B.subtract(A);
        this.d = BmA.getNorm();
        this.u = BmA.mapMultiply(1 / d);
        this.v = new ArrayRealVector(new double[]{-u.getEntry(1), u.getEntry(0)});


    }

    RealVector mapForward(RealVector P) {
        double x = P.subtract(M).dotProduct(u);
        double y = P.subtract(M).dotProduct(v);
        return new ArrayRealVector(new double[]{x, y});
    }

    RealVector curveAt(double x, double a, double c) {
        return M.add(u.mapMultiply(x)).add(v.mapMultiply(a*x*x + c));
    }

    void printStuff() {
        System.out.println("A = " + A);
        System.out.println("B = " + B);
        System.out.println("M = " + M);
        System.out.println("u = " + u);
        System.out.println("v = " + v);
        System.out.println("d = " + d);

        System.out.println("AA = " + mapForward(A));
        System.out.println("BB = " + mapForward(B));

    }

    RealVector doFit(List<Pnt2d> pts) {
        double[] bb = new double[n];
        double[][] MM = new double[n][2];
        for (int k = 1; k < n-1; k++) {
            RealVector Xi = mapForward(new ArrayRealVector(pts.get(k).toDoubleArray()));
            double xi = Xi.getEntry(0);
            double yi = Xi.getEntry(1);
            bb[k] = yi;
            MM[k][0] = xi*xi;
            MM[k][1] = 1;
        }
        RealMatrix M = new Array2DRowRealMatrix(MM, false);
        RealVector b = new ArrayRealVector(bb, false);
        DecompositionSolver solver = new QRDecomposition(M).getSolver();
        RealVector a = solver.solve(b);
        return a;
    }

    void listMappedPoints(List<Pnt2d> pts) {
        System.out.println("\nMapped points:");
        for (Pnt2d p : pts) {
            RealVector q = mapForward(new ArrayRealVector(p.toDoubleArray()));
            System.out.println(q);
        }
    }

    void plotValues(double a, double c, List<Pnt2d> pts) {
        for (Pnt2d p : pts) {
            RealVector q = mapForward(new ArrayRealVector(p.toDoubleArray()));
            double x = q.getEntry(0);
            double y = a * x * x + c;
            System.out.println(Arrays.toString(new double[]{x, y}));
        }

    }

    void plotValues(double a, double c) {
        for (double x = -d; x <= d; x += 0.2) {
            double y = a * x * x + c;
            System.out.println(Arrays.toString(new double[]{x, y}));
        }

    }

    void plotValuesInRealSpace(double a, double c) {
        for (double x = -d; x <= d; x += 0.2) {
            double y = a * x * x + c;
            RealVector P = M.add(u.mapMultiply(x)).add(v.mapMultiply(y));

            System.out.println(x + " -> " + P);
        }

    }

    // ------------------------------------------------------------------

    public static void main(String[] args) {
        List<Pnt2d> pts = Polygons.makePolygon(1, 2, 3, 4, 4, 5, 5, 4, 7, 4);
        //List<Pnt2d> pts = Polygons.makePolygon(3, 4, 4, 5, 5, 4);
        ParabolicLineFit fit = new ParabolicLineFit(pts);
        fit.printStuff();

        fit.listMappedPoints(pts);


        RealVector ac = fit.doFit(pts);
        System.out.println("\nSolution ac = " + ac);



        // fit.plotValues(ac.getEntry(0), ac.getEntry(1), pts);
        // fit.plotValues(ac.getEntry(0), ac.getEntry(1));
        // fit.plotValuesInRealSpace(ac.getEntry(0), ac.getEntry(1));

    }

}
