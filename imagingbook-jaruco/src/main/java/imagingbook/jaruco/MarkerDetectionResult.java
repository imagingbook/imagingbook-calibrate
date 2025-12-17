package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * Represents the detection result for a single marker.
 */

 public class MarkerDetectionResult {
     public final int markerId;
     public final int rotation;
     public final int hammingDist;
     public final List<Pnt2d> corners;

     MarkerDetectionResult(ArucoDictionary.LookupResult lookup, SegmentedContour poly) {
         this.markerId = lookup.markerIndex();
         this.rotation = lookup.rotation();
         this.hammingDist = lookup.hammingDistance();
         this.corners = poly.getCorners();

         // TODO: fix corners rotation , calculate precise image cooordinates
         // by projection from unit square
     }

 }
