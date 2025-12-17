package imagingbook.jaruco;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import imagingbook.common.util.bits.BitVector;

import java.util.Arrays;

public class MarkerChecker {

    private final ImageProcessor ip;
    private final ArucoDictionary dictionary;

    public MarkerChecker(ImageProcessor ip, ArucoDictionary dictionary) {
        this.ip = ip;
        this.dictionary = dictionary;
    }

//    public MarkerDetectionResult checkMarker(ByteProcessor canonical, int threshold) {
//        // STEP 4c - sample marker fields to generate the 1D marker pattern
//        BitVector sampleBits = extractMarkerBits(canonical, threshold);
//
//        double maxCorrectionRate = 1.0; // TODO: CHECK!!!
//        ArucoDictionary.LookupResult lookup =
//                dictionary.lookup(sampleBits, maxCorrectionRate);
//
//        // rotation - how to recalculate the mapping?
//
//
//
//        // if (lookup != null) {
//        //     // Collections.rotate(markerOutline.polygon, lookup.rotation);
//        //     markerOutline.rotatePolygon(lookup.rotation);   // rotate vertices to canonical state
//        //     return new MarkerDetectionResult(lookup, markerOutline, null);   // TODO: rejectedPoints?
//        // }
//        // else {
//        //     return null;
//        // }
//        return null;
//    }


}
