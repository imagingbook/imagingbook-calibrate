/*
 *  This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2006-2022 Wilhelm Burger, Mark J. Burge.
 * All rights reserved. Visit https://imagingbook.com for additional details.
 */
package imagingbook.calibration.zhang;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CameraTest {

    @Test
    public void test1() {
        Locale.setDefault(Locale.US);
        ViewTransform view = new ViewTransform();

        Camera camera1 = new Camera (
                832.5, 832.53, 0.204494, 	// alpha, beta, gamma,
                303.959, 206.585,			// c_x, c_y
                -0.228601, 0.190353);		// k0, k1
//        System.out.println("Camera 1: " + camera1.toString());

        double[] XYZ1 = {40, 70, 800};
        double[] uv1 = camera1.project(view, XYZ1);
//        System.out.print(Matrix.toString(XYZ1) + " -> ");
//        System.out.format("u=%.4f, u=%.4f\n", uv1[0], uv1[1]);

        assertArrayEquals(new double[] {345.5060, 279.2637}, uv1, 1e-4);

        double r1 = 0.95;
        double rr = camera1.warp(r1);
        assertEquals(0.9013, rr, 1e-4);
//        System.out.format("radial distortion: r1=%.4f -> rr=%.4f\n", r1, rr);
        double r2 = camera1.unwarp(rr);
//        System.out.format("inv. radial distortion: rr=%.4f -> r2=%.4f\n", rr, r2);
        assertEquals(r1, r2, 1e-4);

//        System.out.println();

        // distorted camera
//        System.out.println("Camera 2:");
        RealMatrix A = MatrixUtils.createRealMatrix(new double[][] {
                {832.5, 0.204494, 303.959},
                {  0.0, 832.53, 206.585},
                {  0.0,   0.0,     1.0}});
        Camera camera2 = new Camera(A, new double[] {-0.2, 0.190353});
//        System.out.println("Camera 2: " + camera2.toString());

        double[] XYZ2 = {40, 70, 800};
        double[] uv2 = camera2.project(view, XYZ2);
//        System.out.print(Matrix.toString(XYZ2) + " -> ");
//        System.out.format("u=%.4f, u=%.4f\n", uv2[0], uv2[1]);

        r1 = 0.95;
        rr = camera2.warp(r1);
//        System.out.format("radial distortion: r=%.4f -> rr=%.4f\n", r1, rr);
        r1 = camera2.unwarp(rr);
//        System.out.format("inv. radial distortion: rr=%.4f -> r=%.4f\n", rr, r1);

//        System.out.println("\nTesting radial lens distortion:");
        double[] xy2 = {0.3, -0.7};
//        System.out.format("original x=%.4f, y=%.4f\n", xy2[0], xy2[1]);
        double[] xy2d = camera2.warp(xy2);
        assertArrayEquals(new double[] {0.2844, -0.6636}, xy2d, 1e-4);

//        System.out.format("distorted x=%.4f, y=%.4f\n", xy2d[0], xy2d[1]);
        double[] xy2u = camera2.unwarp(xy2d);
//        System.out.format("undistorted x=%.4f, y=%.4f\n", xy2u[0], xy2u[1]);
        assertArrayEquals(xy2, xy2u, 1e-4);

//        System.out.println("\nTesting only radial lens distortion fun:");
        double ra = 0.10;
        double rb = camera2.warp(ra);
        assertEquals(0.0998, rb, 1e-4);
        double rc = camera2.unwarp(rb);
        assertEquals(ra, rc, 1e-4);
//        System.out.format("ra=%.4f, rb=%.4f, rc=%.4f\n", ra, rb, rc);
    }

}