/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibration;

import imagingbook.calibration.distortion.Radial2TermDistortionModel;
import imagingbook.calibration.distortion.RadialDistortionModel;
import org.apache.commons.math4.legacy.linear.MatrixUtils;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CameraTest {

    static final double tol = 1e-6;
    static final Camera camera1 =  new Camera(832.5, 832.53, 0.204494, 303.959, 206.585,
            new Radial2TermDistortionModel(-0.228601, 0.190353));

    static final ViewTransform view = new ViewTransform();

    static {
        Locale.setDefault(Locale.US);
        // System.out.println("Camera 1: " + camera1.toString());
    }

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
    }

    @Test
    public void radialDistortionTest() {
        RadialDistortionModel dist = (RadialDistortionModel) camera1.getDistortion();
        double r1 = 0.95;
        double rr = dist.warp(r1);
        System.out.format("radial distortion: r1=%.6f -> rr=%.6f\n", r1, rr);
        assertEquals(0.901295, rr, tol);

        double r2 = dist.unwarp(rr);
        System.out.format("inv. radial distortion: rr=%.6f -> r2=%.6f\n", rr, r2);
        assertEquals(r1, r2, tol);
    }


    @Test
    public void projectTest() {
        RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
                {832.5, 0.204494, 303.959},
                {  0.0, 832.53, 206.585},
                {  0.0,   0.0,     1.0}});
        Camera camera2 = new Camera(A, new Radial2TermDistortionModel(-0.2, 0.190353));
        // System.out.println("Camera 2: " + camera2.toString());
        double[] XYZ2 = {40, 70, 800};
        double[] uv2 = camera2.project(view, XYZ2);
        // System.out.print(Arrays.toString(XYZ2) + " -> ");
        // System.out.format("u=%.6f, u=%.6f\n", uv2[0], uv2[1]);
        assertArrayEquals(new double[] {345.518124, 279.284836}, uv2, tol);
    }

}