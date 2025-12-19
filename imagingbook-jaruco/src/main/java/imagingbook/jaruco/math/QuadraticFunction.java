package imagingbook.jaruco.math;

import static imagingbook.common.math.Arithmetic.sqr;

/**
 * Represents a function of type
 * {@code outVal = a * (inVal - d)^2 + c }.
 */
public class QuadraticFunction {

    public final double a;
    public final double c;
    public final double d;

    public QuadraticFunction(double a, double c, double d) {
        this.a = a;
        this.c = c;
        this.d = d;
    }

    public double getVal(double inVal) {
        return a * sqr(inVal - d) + c;
    }

    public double getDeriv(double inVal) {
        return 2.0 * a * (inVal - d);
    }


}
