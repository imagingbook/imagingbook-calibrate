/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.intrinsics;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial2TermDistortion;
import imagingbook.calibrate.extrinsics.ViewTransform;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SimpleCameraTest {

    static final double tol = 1e-6;
    static final Camera cam1 =
            new SimpleCamera(new double[] { 832.5, 303.959, 206.585 },
            new Radial2TermDistortion(new double[] {-0.228601, 0.190353}));
    static final ViewTransform view = new ViewTransform();

    @Test
    public void CameraConstructorTest() {
        double alpha = 810;
        double beta = alpha;
        double gamma = 0;
        double uc = 300, vc = 200;
        Camera cam = new SimpleCamera(new double[] {alpha, uc, vc}, new Radial2TermDistortion());
        // ----------------------------------------------------
        assertEquals(alpha, cam.getAlpha(), tol);
        assertEquals(beta, cam.getBeta(), tol);
        assertEquals(gamma, cam.getGamma(), tol);
        assertEquals(uc, cam.getUc(), tol);
        assertEquals(vc, cam.getVc(), tol);
        // ----------------------------------------------------
        RealMatrix A = cam.getAffineMatrix();
        assertEquals(alpha, A.getEntry(0, 0), tol);
        assertEquals(beta, A.getEntry(1, 1), tol);
        assertEquals(gamma, A.getEntry(0, 1), tol);
        assertEquals(uc, A.getEntry(0, 2), tol);
        assertEquals(vc, A.getEntry(1, 2), tol);
        assertEquals(0, A.getEntry(1, 0), tol);

        assertEquals(3, cam.getLinParameterCount());
    }

    @Test
    public void projectTest() {
        double[] XYZ2 = {40, 70, 800};
        double[] uv2 = cam1.project(view, XYZ2);
        assertArrayEquals(new double[] {345.4365654549305, 279.17073954612835}, uv2, tol);
    }

    @Test
    public void withParametersTest1() {
        double[] p = {830, 300, 200, -0.4, 0.25};
        Camera cam2 = cam1.withParameters(p);
        assertNotNull(cam2);
        assertArrayEquals(p, cam2.getParameters(), tol);
        assertEquals(p.length, cam2.getParameterCount());
        assertEquals(p[0], cam2.getAlpha(), tol);
        assertEquals(p[1], cam2.getUc(), tol);
        assertEquals(p[2], cam2.getVc(), tol);
        assertEquals(p[3], cam2.getDistortion().getParameter(0), tol);
        assertEquals(p[4], cam2.getDistortion().getParameter(1), tol);
        assertEquals(cam1.getDistortion().getClass(), cam2.getDistortion().getClass());
    }

    @Test
    public void withParametersTest2() {
        double[] linP = {830, 300, 200};
        double[] distP = {-0.4, 0.25};
        Camera cam2 = cam1.withParameters(linP, distP);
        assertNotNull(cam2);
        assertArrayEquals(linP, cam2.getLinearParameters(), tol);

        assertEquals(linP.length, cam2.getLinParameterCount());
        assertEquals(linP[0], cam2.getAlpha(), tol);
        assertEquals(linP[1], cam2.getUc(), tol);
        assertEquals(linP[2], cam2.getVc(), tol);

        assertEquals(distP[0], cam2.getDistortion().getParameter(0), tol);
        assertEquals(distP[1], cam2.getDistortion().getParameter(1), tol);
        assertEquals(cam1.getDistortion().getClass(), cam2.getDistortion().getClass());
    }

    @Test
    public void getDistortion() {
        DistortionModel dist = cam1.getDistortion();
        assertNotNull(dist);
        assertTrue(dist instanceof Radial2TermDistortion);
    }

    @Test
    public void getParameterCount() {
        assertEquals(5, cam1.getParameterCount());
        assertEquals(3, cam1.getLinParameterCount());
        assertEquals(2, cam1.getDistParameterCount());
    }

    @Test
    public void getParameters() {
        double alpha = 810, beta = 810, gamma = 0, uc = 300, vc = 200;
        double k0 = 0.02, k1 = -0.1;
        Camera cam = new SimpleCamera(new double[] {alpha, uc, vc},
                new Radial2TermDistortion((new double[] {k0, k1})));

        System.out.println("cam = " + cam);
        double[] lp = cam.getLinearParameters();
        assertArrayEquals(new double[] {alpha, uc, vc}, lp, tol);

        double[] dp = cam.getDistortionParameters();
        assertArrayEquals(new double[] {k0, k1}, dp, tol);

        double[] p = cam.getParameters();
        assertArrayEquals(new double[] {alpha, uc, vc, k0, k1}, p, tol);

        assertEquals(alpha, cam.getAlpha(), tol);
        assertEquals(beta, cam.getBeta(), tol);
        assertEquals(gamma, cam.getGamma(), tol);
        assertEquals(uc, cam.getUc(), tol);
        assertEquals(vc, cam.getVc(), tol);
    }

    @Test
    public void projectNormalized() {
    }

    @Test
    public void testProjectNormalized() {
    }

    @Test
    public void mapToSensorPlane() {
    }

    @Test
    public void getAffineMatrix() {
    }

    @Test
    public void getInverseA() {
    }

}