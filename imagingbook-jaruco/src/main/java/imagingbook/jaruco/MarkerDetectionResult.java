package imagingbook.jaruco;

import imagingbook.common.geometry.basic.Pnt2d;

import java.util.List;

/**
 * Represents the detection result for a single marker.
 */

 public record MarkerDetectionResult (
         int markerId,
         int rotation,
         int hammingDist,
         List<Pnt2d> corners)
{

     MarkerDetectionResult(ArucoDictionary.LookupResult lookup, List<Pnt2d> poly) {
         this(lookup.markerIndex(), lookup.rotation(), lookup.hammingDistance(), poly);
     }
 }
