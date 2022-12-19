package imagingbook.calibration.zhang;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import java.awt.geom.Point2D;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;


/**
 * Nonlinear optimizer based on the Levenberg-Marquart method, where the Jacobian matrix is calculated from analytic
 * expressions (obtained with MatLab). Part of this implementation is based on code from OpenIMAJ by J. Hare: Copyright
 * (c) 2011, The University of Southampton and the individual contributors. All rights reserved (see
 * https://github.com/openimaj/openimaj/).
 *
 * @author WB
 */
public class NonlinearOptimizerAnalytic extends NonlinearOptimizer {

	NonlinearOptimizerAnalytic(Point2D[] modelPts, Point2D[][] obsPts) {
		super(modelPts, obsPts);
	}

	@Override
	MultivariateVectorFunction makeValueFun() {
		return new ValueFun();
	}

	@Override
	MultivariateMatrixFunction makeJacobianFun() {
		return new JacobianFun();
	}

	private class JacobianFun implements MultivariateMatrixFunction {
		/**
		 * Calculates a "stacked" Jacobian matrix with 2MN rows and K = 7 + 6M columns (for M views with N points each,
		 * K parameters). For example, with M = 5 views and N = 256 points each, J is of size 2560 × 37. Each pair of
		 * rows in the Jacobian corresponds to one point.
		 */
		@Override
		public double[][] value(double[] params) {
			final double[][] J = new double[2 * M * N][];
			int r = 0;	// row
			for (int i = 0; i < M; i++) {	// for all views
				for (int j = 0; j < N; j++) {	// for all points
					final double[][] Jij = subJacobian(i, j, params);
					J[r + 0] = Jij[0];	// row 0 (x-coordinate)
					J[r + 1] = Jij[1];	// row 1 (y-coordinate)
					r = r + 2;
				}
			}
//			System.out.format("**** J = %d / %d\n", J.length, J[0].length);
//			System.out.println(NonlinearOptimizerAnalytic.class.getSimpleName() + 
//	        		": Jacobian inverse condition number = " + MathUtil.inverseConditionNumber(J));
			return J;
		}

		/**
		 * Calculates the sub-Jacobian for view 'i' / model point 'j' with the current parameter vector 'params' .
		 *
		 * @param i the point index (= 0,...,M)
		 * @param j the view index (= 0,...,N)
		 * @param params the current parameters (of length K)
		 * @return the two rows (2 x K sub-matrix) of the Jacobian for the given point
		 */
		private double[][] subJacobian(int i, int j, double[] params) {
			final double[][] A0 = new double[2][camParLength + viewParLength];

			final double X = modelPts[j].getX();
			final double Y = modelPts[j].getY();

			final double alpha = params[0];
			final double beta  = params[1];
			final double gamma = params[2];
			final double uc = params[3];
			final double vc = params[4];
			final double k0 = params[5];
			final double k1 = params[6];

			final double wx = params[i * viewParLength + camParLength + 0];
			final double wy = params[i * viewParLength + camParLength + 1];
			final double wz = params[i * viewParLength + camParLength + 2];
			final double tx = params[i * viewParLength + camParLength + 3];
			final double ty = params[i * viewParLength + camParLength + 4];
			final double tz = params[i * viewParLength + camParLength + 5];

			// begin matlab code
			final double t2 = wx * wx;
			final double t3 = wy * wy;
			final double t4 = wz * wz;
			final double t5 = t2 + t3 + t4;
			final double t6 = sqrt(t5);
			final double t7 = sin(t6);
			final double t8 = 1.0 / sqrt(t5);
			final double t9 = cos(t6);
			final double t10 = t9 - 1.0;
			final double t11 = 1.0 / t5;
			final double t12 = t7 * t8 * wy;
			final double t13 = t10 * t11 * wx * wz;
			final double t14 = t12 + t13;
			final double t15 = t7 * t8 * wz;
			final double t16 = t7 * t8 * wx;
			final double t18 = t10 * t11 * wy * wz;
			final double t17 = t16 - t18;
			final double t19 = Y * t17;
			final double t39 = X * t14;
			final double t20 = t19 - t39 + tz;
			final double t21 = 1.0 / t20;
			final double t22 = t10 * t11 * wx * wy;
			final double t23 = t3 + t4;
			final double t24 = t10 * t11 * t23;
			final double t25 = t24 + 1.0;
			final double t26 = alpha * t25;
			final double t27 = t15 + t22;
			final double t28 = t17 * uc;
			final double t29 = t2 + t4;
			final double t30 = t10 * t11 * t29;
			final double t31 = t30 + 1.0;
			final double t32 = gamma * t31;
			final double t45 = alpha * t27;
			final double t33 = t28 + t32 - t45;
			final double t34 = Y * t33;
			final double t35 = alpha * tx;
			final double t36 = gamma * ty;
			final double t37 = tz * uc;
			final double t40 = t15 - t22;
			final double t41 = gamma * t40;
			final double t42 = t14 * uc;
			final double t43 = t26 + t41 - t42;
			final double t44 = X * t43;
			final double t46 = t34 + t35 + t36 + t37 + t44;
			final double t47 = t21 * t46;
			final double t38 = -t47 + uc;
			final double t48 = 1.0 / (alpha * alpha * alpha);
			final double t49 = t38 * t38;
			final double t50 = t48 * t49 * 2.0;
			final double t51 = 1.0 / (alpha * alpha);
			final double t52 = X * t25;
			final double t57 = Y * t27;
			final double t53 = t52 - t57 + tx;
			final double t54 = t21 * t38 * t51 * t53 * 2.0;
			final double t55 = t50 + t54;
			final double t60 = beta * ty;
			final double t61 = beta * t40;
			final double t62 = t14 * vc;
			final double t63 = t61 - t62;
			final double t64 = X * t63;
			final double t65 = tz * vc;
			final double t66 = t17 * vc;
			final double t67 = beta * t31;
			final double t68 = t66 + t67;
			final double t69 = Y * t68;
			final double t70 = t60 + t64 + t65 + t69;
			final double t71 = t21 * t70;
			final double t56 = -t71 + vc;
			final double t58 = t49 * t51;
			final double t59 = 1.0 / (beta * beta);
			final double t72 = t56 * t56;
			final double t73 = t59 * t72;
			final double t74 = t58 + t73;
			final double t75 = 1.0 / (beta * beta * beta);
			final double t76 = t72 * t75 * 2.0;
			final double t77 = X * t40;
			final double t78 = Y * t31;
			final double t79 = t77 + t78 + ty;
			final double t80 = t21 * t56 * t59 * t79 * 2.0;
			final double t81 = t76 + t80;
			final double t82 = k0 * t74;
			final double t83 = t74 * t74;
			final double t84 = k1 * t83;
			final double t85 = t82 + t84;
			final double t86 = 1.0 / pow(t5, 3.0 / 2.0);
			final double t87 = 1.0 / (t5 * t5);
			final double t88 = t9 * t11 * wx * wz;
			final double t89 = t2 * t7 * t86 * wy;
			final double t90 = t2 * t10 * t87 * wy * 2.0;
			final double t91 = t7 * t86 * wx * wy;
			final double t92 = t2 * t7 * t86 * wz;
			final double t93 = t2 * t10 * t87 * wz * 2.0;
			final double t105 = t10 * t11 * wz;
			final double t106 = t9 * t11 * wx * wy;
			final double t94 = t91 + t92 + t93 - t105 - t106;
			final double t95 = t7 * t8;
			final double t96 = t2 * t9 * t11;
			final double t97 = t10 * t87 * wx * wy * wz * 2.0;
			final double t98 = t7 * t86 * wx * wy * wz;
			final double t103 = t2 * t7 * t86;
			final double t99 = t95 + t96 + t97 + t98 - t103;
			final double t100 = t10 * t29 * t87 * wx * 2.0;
			final double t101 = t7 * t29 * t86 * wx;
			final double t116 = t10 * t11 * wx * 2.0;
			final double t102 = t100 + t101 - t116;
			final double t104 = t7 * t86 * wx * wz;
			final double t107 = X * t94;
			final double t108 = Y * t99;
			final double t109 = t107 + t108;
			final double t110 = 1.0 / (t20 * t20);
			final double t111 = t10 * t23 * t87 * wx * 2.0;
			final double t112 = t7 * t23 * t86 * wx;
			final double t113 = t111 + t112;
			final double t117 = t10 * t11 * wy;
			final double t114 = t88 + t89 + t90 - t104 - t117;
			final double t115 = t94 * uc;
			final double t118 = t99 * uc;
			final double t119 = beta * t102;
			final double t262 = t99 * vc;
			final double t120 = t119 - t262;
			final double t121 = Y * t120;
			final double t122 = beta * t114;
			final double t123 = t94 * vc;
			final double t124 = t122 + t123;
			final double t263 = X * t124;
			final double t125 = t121 - t263;
			final double t126 = t21 * t125;
			final double t127 = t70 * t109 * t110;
			final double t128 = t126 + t127;
			final double t129 = gamma * t114;
			final double t141 = alpha * t113;
			final double t130 = t115 + t129 - t141;
			final double t131 = X * t130;
			final double t132 = -t88 + t89 + t90 + t104 - t117;
			final double t133 = alpha * t132;
			final double t142 = gamma * t102;
			final double t134 = t118 + t133 - t142;
			final double t135 = Y * t134;
			final double t136 = t131 + t135;
			final double t137 = t21 * t136;
			final double t143 = t46 * t109 * t110;
			final double t138 = t137 - t143;
			final double t139 = t38 * t51 * t138 * 2.0;
			final double t264 = t56 * t59 * t128 * 2.0;
			final double t140 = t139 - t264;
			final double t144 = t3 * t7 * t86 * wz;
			final double t145 = t3 * t10 * t87 * wz * 2.0;
			final double t146 = -t91 - t105 + t106 + t144 + t145;
			final double t147 = t3 * t7 * t86;
			final double t156 = t3 * t9 * t11;
			final double t148 = -t95 + t97 + t98 + t147 - t156;
			final double t149 = t10 * t29 * t87 * wy * 2.0;
			final double t150 = t7 * t29 * t86 * wy;
			final double t151 = t149 + t150;
			final double t152 = t9 * t11 * wy * wz;
			final double t153 = t3 * t7 * t86 * wx;
			final double t154 = t3 * t10 * t87 * wx * 2.0;
			final double t155 = t7 * t86 * wy * wz;
			final double t157 = Y * t146;
			final double t158 = X * t148;
			final double t159 = t157 + t158;
			final double t161 = t10 * t11 * wx;
			final double t160 = t152 + t153 + t154 - t155 - t161;
			final double t162 = beta * t160;
			final double t163 = t148 * vc;
			final double t164 = t162 + t163;
			final double t165 = X * t164;
			final double t166 = beta * t151;
			final double t267 = t146 * vc;
			final double t167 = t166 - t267;
			final double t268 = Y * t167;
			final double t168 = t165 - t268;
			final double t169 = t21 * t168;
			final double t269 = t70 * t110 * t159;
			final double t170 = t169 - t269;
			final double t171 = t56 * t59 * t170 * 2.0;
			final double t172 = -t152 + t153 + t154 + t155 - t161;
			final double t173 = alpha * t172;
			final double t174 = t146 * uc;
			final double t189 = gamma * t151;
			final double t175 = t173 + t174 - t189;
			final double t176 = Y * t175;
			final double t177 = t10 * t23 * t87 * wy * 2.0;
			final double t178 = t7 * t23 * t86 * wy;
			final double t190 = t10 * t11 * wy * 2.0;
			final double t179 = t177 + t178 - t190;
			final double t180 = gamma * t160;
			final double t181 = t148 * uc;
			final double t191 = alpha * t179;
			final double t182 = t180 + t181 - t191;
			final double t183 = X * t182;
			final double t184 = t176 + t183;
			final double t185 = t21 * t184;
			final double t192 = t46 * t110 * t159;
			final double t186 = t185 - t192;
			final double t187 = t38 * t51 * t186 * 2.0;
			final double t188 = t171 + t187;
			final double t193 = t4 * t9 * t11;
			final double t194 = t4 * t7 * t86 * wx;
			final double t195 = t4 * t10 * t87 * wx * 2.0;
			final double t196 = -t152 + t155 - t161 + t194 + t195;
			final double t197 = t4 * t7 * t86;
			final double t198 = t10 * t29 * t87 * wz * 2.0;
			final double t199 = t7 * t29 * t86 * wz;
			final double t204 = t10 * t11 * wz * 2.0;
			final double t200 = t198 + t199 - t204;
			final double t201 = t4 * t7 * t86 * wy;
			final double t202 = t4 * t10 * t87 * wy * 2.0;
			final double t203 = t88 - t104 - t117 + t201 + t202;
			final double t205 = t10 * t23 * t87 * wz * 2.0;
			final double t206 = t7 * t23 * t86 * wz;
			final double t207 = t196 * uc;
			final double t208 = t95 + t97 + t98 + t193 - t197;
			final double t209 = t203 * uc;
			final double t210 = -t95 + t97 + t98 - t193 + t197;
			final double t211 = alpha * t210;
			final double t231 = gamma * t200;
			final double t212 = t209 + t211 - t231;
			final double t213 = Y * t212;
			final double t214 = X * t196;
			final double t215 = Y * t203;
			final double t216 = t214 + t215;
			final double t217 = t196 * vc;
			final double t218 = beta * t208;
			final double t219 = t217 + t218;
			final double t220 = X * t219;
			final double t221 = beta * t200;
			final double t273 = t203 * vc;
			final double t222 = t221 - t273;
			final double t274 = Y * t222;
			final double t223 = t220 - t274;
			final double t224 = t21 * t223;
			final double t275 = t70 * t110 * t216;
			final double t225 = t224 - t275;
			final double t226 = t56 * t59 * t225 * 2.0;
			final double t227 = -t204 + t205 + t206;
			final double t228 = gamma * t208;
			final double t237 = alpha * t227;
			final double t229 = t207 + t228 - t237;
			final double t230 = X * t229;
			final double t232 = t213 + t230;
			final double t233 = t21 * t232;
			final double t238 = t46 * t110 * t216;
			final double t234 = t233 - t238;
			final double t235 = t38 * t51 * t234 * 2.0;
			final double t236 = t226 + t235;
			final double t239 = 1.0 / alpha;
			final double t240 = 1.0 / beta;
			final double t241 = t21 * t56 * t240 * 2.0;
			final double t242 = gamma * t21 * t38 * t51 * 2.0;
			final double t243 = t241 + t242;
			final double t244 = t21 * uc;
			final double t248 = t46 * t110;
			final double t245 = t244 - t248;
			final double t246 = t70 * t110;
			final double t285 = t21 * vc;
			final double t247 = t246 - t285;
			final double t249 = t38 * t51 * t245 * 2.0;
			final double t286 = t56 * t59 * t247 * 2.0;
			final double t250 = t249 - t286;
			final double t251 = k0 * t55;
			final double t252 = k1 * t55 * t74 * 2.0;
			final double t253 = t251 + t252;
			final double t254 = k0 * t81;
			final double t255 = k1 * t74 * t81 * 2.0;
			final double t256 = t254 + t255;
			final double t257 = t21 * t79;
			final double t258 = t21 * t79 * t85;
			final double t259 = k0 * t21 * t38 * t51 * t79 * 2.0;
			final double t260 = k1 * t21 * t38 * t51 * t74 * t79 * 4.0;
			final double t261 = t259 + t260;
			final double t265 = k0 * t140;
			final double t266 = k1 * t74 * t140 * 2.0;
			final double t270 = k0 * t188;
			final double t271 = k1 * t74 * t188 * 2.0;
			final double t272 = t270 + t271;
			final double t276 = k0 * t236;
			final double t277 = k1 * t74 * t236 * 2.0;
			final double t278 = t276 + t277;
			final double t279 = k0 * t21 * t38 * t239 * 2.0;
			final double t280 = k1 * t21 * t38 * t74 * t239 * 4.0;
			final double t281 = t279 + t280;
			final double t282 = k0 * t243;
			final double t283 = k1 * t74 * t243 * 2.0;
			final double t284 = t282 + t283;
			final double t287 = k0 * t250;
			final double t288 = k1 * t74 * t250 * 2.0;
			final double t289 = t287 + t288;
			// alpha
			A0[0][0] = t21 * t53 + t253
					* (uc - t21 * (t34 + t35 + t36 + t37 + X * (t26 - t14 * uc + gamma * (t15 - t10 * t11 * wx * wy))))
					+ t21 * t53 * t85;
			// beta
			A0[0][1] = t38 * t256;
			// uc
			A0[0][3] = 1.0;
			// vc
			A0[0][4] = 0.0;
			// gamma
			A0[0][2] = t257 + t258 + t38 * t261;
			// k1
			A0[0][5] = -t38 * t74;
			// k2
			A0[0][6] = -t38 * t83;
			A0[0][7] = t137
					- t143
					+ t38
					* (t265 + t266)
					+ t85
					* (t21
							* (X * (t115 - alpha * t113 + gamma * (t88 + t89 + t90 - t10 * t11 * wy - t7 * t86 * wx * wz)) + Y
									* (t118 + alpha * (-t88 + t89 + t90 + t104 - t10 * t11 * wy) - gamma * t102)) - t46 * t109
									* t110);
			A0[0][8] = t185 - t192 + t85 * t186 + t38 * t272;
			A0[0][9] = -t238
					+ t38
					* t278
					+ t85
					* t234
					+ t21
					* (t213 + X
							* (t207 + gamma * (t95 + t97 + t98 + t193 - t4 * t7 * t86) - alpha
									* (t205 + t206 - t10 * t11 * wz * 2.0)));
			A0[0][10] = alpha * t21 + t38 * t281 + alpha * t21 * t85;
			A0[0][11] = gamma * t21 + t38 * t284 + gamma * t21 * t85;
			A0[0][12] = t244 - t46 * t110 + t38 * t289 + t85 * t245;

			// alpha
			A0[1][0] = t56 * t253;
			// beta
			A0[1][1] = t257 + t258 + t56 * t256;
			// uc
			A0[1][3] = 0.0;
			// vc
			A0[1][4] = 1.0;
			// gamma
			A0[1][2] = t56 * t261;
			// k1
			A0[1][5] = -t56 * t74;
			// k2
			A0[1][6] = -t56 * t83;

			A0[1][7] = -t126 - t127 + t56 * (t265 + t266) - t85 * t128;
			A0[1][8] = t169 - t269 + t85 * t170 + t56 * t272;
			A0[1][9] = t224 - t275 + t85 * t225 + t56 * t278;
			A0[1][10] = t56 * t281;
			A0[1][11] = beta * t21 + t56 * t284 + beta * t21 * t85;
			A0[1][12] = -t246 + t285 - t85 * t247 + t56 * t289;
			// end of matlab code

			final double[][] Jij = new double[2][camParLength + viewParLength * M];
			System.arraycopy(A0[0], 0, Jij[0], 0, camParLength);
			System.arraycopy(A0[1], 0, Jij[1], 0, camParLength);
			System.arraycopy(A0[0], 7, Jij[0], camParLength + i * viewParLength, viewParLength);
			System.arraycopy(A0[1], 7, Jij[1], camParLength + i * viewParLength, viewParLength);
			//System.out.format("**** Jij = %d / %d\n", Jij.length, Jij[0].length);
			return Jij;
		}
	}

}
