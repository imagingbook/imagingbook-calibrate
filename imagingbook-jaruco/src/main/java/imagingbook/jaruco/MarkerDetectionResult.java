package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.Arrays;

/**
 * Represents the detection result for a single marker.
 */
public record MarkerDetectionResult(
        int markerId,
        int rotation,
        int hDist,
        MarkerOutline corners,
        Pnt2d[] rejectedPoints) {
}


// public class MarkerDetectionResult {
//     // TODO: add getter methods!
//     public final int markerId;
//     public final int rotation;
//     public final int hammingDist;
//     public final MarkerOutline corners;
//     public final Pnt2d[] rejectedPoints;
//
//     // Constructor (private).
//     private MarkerDetectionResult(int markerId, int rotation, int hDist,
//                                   MarkerOutline corners, Pnt2d[] rejectedPoints) {
//         this.markerId = markerId;
//         this.rotation = rotation;
//         this.hammingDist = hDist;
//         this.corners = corners;
//         this.rejectedPoints = rejectedPoints;
//     }
//
//     /**
//      * Builds a {@link MarkerDetectionResult} from a given {@link ArucoDictionary.LookupResult}
//      * instance, adding the associated corner positions and rejected points.
//      *
//      * @param lookupR        a {@link ArucoDictionary.LookupResult} instance
//      * @param corners        corner positions for the detected marker
//      * @param rejectedPoints
//      * @return a new {@link MarkerDetectionResult} instance
//      */
//     MarkerDetectionResult(ArucoDictionary.LookupResult lookupR, MarkerOutline corners, Pnt2d[] rejectedPoints) {
//         this(lookupR.markerIndex(), lookupR.rotation(), lookupR.hammingDistance(), corners, rejectedPoints);
//     }
//
//     @Override
//     public String toString() {
//         return String.format("%s [id=%d rot=%d dist=%d corners=%s]",
//                 getClass().getSimpleName(), markerId, rotation, hammingDist,
//                 Arrays.toString(corners.polygon.toArray(new Pnt2d[0])));
//     }
// }
