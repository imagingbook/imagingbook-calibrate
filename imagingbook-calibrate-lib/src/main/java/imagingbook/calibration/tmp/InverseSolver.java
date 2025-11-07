/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration.tmp;

// Pseudocode / sketch — adapt to your concrete fx, fy, and Commons Math version.
import org.apache.commons.math4.legacy.linear.*;

public class InverseSolver {
    // user-supplied
    interface Func {
        RealVector eval(RealVector x);           // returns [u_calc, v_calc]
        RealMatrix jacobian(RealVector x);       // expensive; can be null if unavailable
    }

    Func f;
    double tolRes = 1e-8;
    double tolStep = 1e-8;
    int maxIters = 50;
    int recomputeJacobianEvery = 10; // safety

    public RealVector solve(RealVector uTarget, RealVector xInit) {
        RealVector x = xInit.copy();
        RealMatrix J = f.jacobian(x); // compute at start
        RealVector Fx = f.eval(x);
        RealVector res = uTarget.subtract(Fx);

        for (int k = 0; k < maxIters; ++k) {
            if (res.getNorm() < tolRes) return x;
            // solve J * delta = res  (use QR or LU)
            DecompositionSolver solver = new QRDecomposition(J).getSolver();
            RealVector delta;
            try {
                delta = solver.solve(res);
            } catch (SingularMatrixException ex) {
                // fallback: regularize J (J^T J + lambda I) or recompute full J
                J = f.jacobian(x);
                solver = new QRDecomposition(J).getSolver();
                delta = solver.solve(res);
            }

            // damp / line-search: try full step then reduce if residual doesn't improve
            double alpha = 1.0;
            RealVector xNew = x.add(delta.mapMultiply(alpha));
            RealVector FxNew = f.eval(xNew);
            RealVector resNew = uTarget.subtract(FxNew);
            double resid = res.getNorm();
            if (resNew.getNorm() >= resid) {
                // backtrack a few times
                double[] alphas = {0.5, 0.25, 0.1, 0.01};
                boolean improved = false;
                for (double a : alphas) {
                    RealVector xt = x.add(delta.mapMultiply(a));
                    RealVector Ft = f.eval(xt);
                    RealVector rt = uTarget.subtract(Ft);
                    if (rt.getNorm() < resid) {
                        alpha = a;
                        xNew = xt; FxNew = Ft; resNew = rt;
                        improved = true;
                        break;
                    }
                }
                if (!improved) {
                    // Failed to improve: recompute exact Jacobian and try one damped step
                    J = f.jacobian(x);
                    solver = new QRDecomposition(J).getSolver();
                    delta = solver.solve(res);
                    xNew = x.add(delta.mapMultiply(0.5));
                    FxNew = f.eval(xNew);
                    resNew = uTarget.subtract(FxNew);
                }
            }

            // Broyden update for J using s and y
            RealVector sVec = xNew.subtract(x);
            RealVector yVec = FxNew.subtract(Fx);
            double sDotS = sVec.dotProduct(sVec);
            if (sDotS > 0) {
                RealVector diff = yVec.subtract(J.operate(sVec));
                // J = J + (diff * s^T) / (s^T s)
                RealMatrix add = diff.outerProduct(sVec).scalarMultiply(1.0 / sDotS);
                J = J.add(add);
            } else {
                // tiny step: force recompute
            }

            x = xNew;
            Fx = FxNew;
            res = resNew;

            // recompute true Jacobian periodically or based on residual drift
            if (k % recomputeJacobianEvery == 0) {
                J = f.jacobian(x);
            }

            if (sVec.getNorm() < tolStep) return x;
        }
        return x; // give best effort (could indicate non-convergence)
    }
}
