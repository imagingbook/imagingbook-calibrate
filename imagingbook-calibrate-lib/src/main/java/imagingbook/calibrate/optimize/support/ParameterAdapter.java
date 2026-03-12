/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.calibrate.optimize.support;

import imagingbook.common.util.SubsequenceMapping;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

/**
 * Maps between <em>full</em> (original) parameters (pF) and <em>model</em> parameters (pM), which
 * are a subset of the full parameters. Converts parameter indexes and scales. Let i, j be the
 * indexes for the same parameter in the full and model parameter vectors, respectively, s[i] the
 * scale for parameter i:
 * <pre>{@code
 *     pM[j] <- pF[i] / s[i]     // pM = scale(pF)
 *     pF[i] <- pM[j] * s[i]     // pF = unscale(pM)
 * }</pre>
 * Scale values must be non-zero but may be positive or negative.
 */
public class ParameterAdapter {

    private final SubsequenceMapping sMap;     // maps full parameter to model indexes (and back)
    private final double[] scales;             // scale vales (for full parameter vector)

    /**
     * Constructor. Throws an exception if {@code subset} and {@code scales} are not of the same
     * length or if {@code scales} contains zero values.
     *
     * @param subset a {@link BitVector} flagging the model parameters
     * @param scales a vector of non-zero scale values, one for each full parameter (pass
     * {@code null} to set all scales to 1.0)
     */
    public ParameterAdapter(BitVector subset, double[] scales) {
        this.sMap = new SubsequenceMapping(subset);
        if (scales == null) {
            this.scales = new double[subset.length()];
            Arrays.fill(this.scales, 1.0);
        } else {
            this.scales = scales.clone();
        }
    }

    public ParameterAdapter(BitVector subset) {
        this(subset, null);
    }

    /**
     * Updates the parameter scale values.
     *
     * @param scales a vector of non-zero scale values, one for each full parameter
     */
    public void setScales(double[] scales) {
        if (scales.length != sMap.getOrigSequenceLength()) {
            throw new IllegalArgumentException("scales.length != skipArray.length");
        }
        for (int i = 0; i < scales.length; i++) {
            if (Math.abs(scales[i]) < 1e-12) {
                throw new IllegalArgumentException("scale value < 1e-12 at pos " + i);
            }
            this.scales[i] = scales[i];
        }
    }

    /**
     * Maps unscaled, full parameters {@code pFull} to reduced and scaled model parameters.
     *
     * @param pFull unscaled full parameters
     * @return reduced and scaled model parameters
     */
    public double[] getModelParameters(double[] pFull) {
        if (pFull.length != scales.length) {
            throw new IllegalArgumentException("pp.length != scales.length");
        }
        // reduce and scale by omitting skipped
        double[] pModel = new double[sMap.getSubSequenceLength()];
        for (int j = 0; j < pModel.length; j++) {
            int i = sMap.getOrigPosition(j);
            pModel[j] = pFull[i] / scales[i];
        }
        return pModel;
    }

    /**
     * Maps scaled model parameters {@code pModel} to full and unscaled physical parameters.
     * Parameters from {@code pModel} are merged into a copy of the full parameter vector
     * {@code pFull}. Parameters missing from {@code pModel} are carried over from {@code pFull}.
     *
     * @param pModel reduced, scaled model parameters
     * @param pFull expanded, unscaled full parameters (template)
     * @return expanded, unscaled full parameters
     */
    public double[] getFullParameters(double[] pModel, double[] pFull) {
        double[] pF = pFull.clone();
        // insert from reduced parameters:
        for (int j = 0; j < pModel.length; j++) {
            int i = sMap.getOrigPosition(j);
            pF[i] = pModel[j] * scales[i];
        }
        return pF;
    }

    /**
     * Returns the scale value for the specified parameter.
     *
     * @param p parameter index (in full parameter vector)
     * @return the corresponding scale value
     */
    public double getParameterScale(int p) {
        return scales[p];
    }

    /**
     * Returns the index in the full parameter vector for the given model parameter index.
     *
     * @param modelIdx index in model parameter vector
     * @return the corresponding full parameter index
     */
    public int getFullParamIdx(int modelIdx) {
        return sMap.getOrigPosition(modelIdx);
    }

    /**
     * Returns the model parameter index for a given full parameter index. Returns -1 if the
     * corresponding parameter is not contained in the model parameters.
     *
     * @param fullIdx index in full parameter vector
     * @return the model parameter index or -1 if not conteined in the model
     */
    public int getModelParamIdx(int fullIdx) {
        return sMap.getSubPosition(fullIdx);
    }

    /**
     * Returns the length of the full parameter vector.
     *
     * @return the length of the full parameter vector
     */
    public int getFullParamLength() {
        return sMap.getOrigSequenceLength();
    }

    /**
     * Returns the length of the model parameter vector.
     *
     * @return the length of the model parameter vector
     */
    public int getModelParamLength() {
        return sMap.getSubSequenceLength();
    }
}
