/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate;

import imagingbook.calibrate.distortion.DistortionModel;
import imagingbook.calibrate.distortion.Radial2TermDistortionModel;
import imagingbook.calibrate.extrinsics.ViewTransform;
import imagingbook.calibrate.intrinsics.Camera;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CameraTest {

    static final double tol = 1e-6;
    static final Camera cam1 =
            new Camera(832.5, 832.53, 0.204494, 303.959, 206.585,
            new Radial2TermDistortionModel(new double[] {-0.228601, 0.190353}));
    static final ViewTransform view = new ViewTransform();

    @Test
    public void CameraConstructorTest() {
        double alpha = 810, beta = 815, gamma = 0.2;
        double uc = 300, vc = 200;
        Camera cam = new Camera(alpha, beta, gamma, uc, vc, new Radial2TermDistortionModel());
        // ----------------------------------------------------
        assertEquals(alpha, cam.getAlpha(), tol);
        assertEquals(beta, cam.getBeta(), tol);
        assertEquals(gamma, cam.getGamma(), tol);
        assertEquals(uc, cam.getUc(), tol);
        assertEquals(vc, cam.getVc(), tol);
        // ----------------------------------------------------
        RealMatrix A = cam.getMatrixA();
        assertEquals(alpha, A.getEntry(0, 0), tol);
        assertEquals(beta, A.getEntry(1, 1), tol);
        assertEquals(gamma, A.getEntry(0, 1), tol);
        assertEquals(uc, A.getEntry(0, 2), tol);
        assertEquals(vc, A.getEntry(1, 2), tol);
        assertEquals(0, A.getEntry(1, 0), tol);

        assertEquals(7, cam.getParameterCount());
    }


    @Test
    public void projectTest() {
        double[] XYZ2 = {40, 70, 800};
        double[] uv2 = cam1.project(view, XYZ2);
        // System.out.print(Arrays.toString(XYZ2) + " -> ");
        // System.out.format("u=%.6f, u=%.6f\n", uv2[0], uv2[1]);
        assertArrayEquals(new double[] {345.5060273, 279.2636757}, uv2, tol);
    }

    @Test
    public void copyOf() {
        double[] p = {830, 832, 0.5, 300, 200, -0.4, 0.25};
        Camera cam2 = cam1.copyOf(p);
        assertNotNull(cam2);
        assertArrayEquals(p, cam2.getParameterVector(), tol);
        assertEquals(p.length, cam2.getParameterCount());
        assertEquals(p[0], cam2.getAlpha(), tol);
        assertEquals(p[1], cam2.getBeta(), tol);
        assertEquals(p[2], cam2.getGamma(), tol);
        assertEquals(p[3], cam2.getUc(), tol);
        assertEquals(p[4], cam2.getVc(), tol);
        assertEquals(p[5], cam2.getDistortion().getParameter(0), tol);
        assertEquals(p[6], cam2.getDistortion().getParameter(1), tol);
        assertEquals(cam1.getDistortion().getClass(), cam2.getDistortion().getClass());
    }

    @Test
    public void getDistortion() {
        DistortionModel dist = cam1.getDistortion();
        assertNotNull(dist);
        assertTrue(dist instanceof Radial2TermDistortionModel);
    }

    @Test
    public void project() {
    }

    @Test
    public void testProject() {
    }

    @Test
    public void testProject1() {
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
    public void getParameterVector() {
    }

    @Test
    public void getParameterCount() {
    }

    @Test
    public void getAlpha() {
    }

    @Test
    public void getBeta() {
    }

    @Test
    public void getGamma() {
    }

    @Test
    public void getUc() {
    }

    @Test
    public void getVc() {
    }

    @Test
    public void getMatrixA() {
    }

    @Test
    public void getInverseA() {
    }

    @Test
    public void getHomography() {
    }
}