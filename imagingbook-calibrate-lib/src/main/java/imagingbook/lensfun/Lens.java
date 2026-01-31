/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.util.ArrayList;
import java.util.List;

public class Lens {

    public record AspectRatio(int width, int height) {
        public double asDecimal() {
            return (double) width / height;
        }

        @Override
        public String toString() {
            return width + ":" + height;
        }
    }

    public record NumericRange(double min, double max) {
        @Override
        public String toString() {
            return this.min + ":" + this.max;
        }
    }

    // public record Distortion(double focal, String model, double k1, double k2, double k3) {}
    public record Distortion(double focal, String model,
                             double k1, double k2, double a, double b, double c) {
        // Helper to check if this entry actually contains data
        public boolean isValid() {
            return model != null && !model.isEmpty();
        }
    }

    public record Tca(String model, double focal,
                      // model = linear:
                      double kr, double kb,
                      // model = poly3:
                      double vr, double vb, double cr, double cb, double br, double bb ) {}

    public record Vignetting(String model, double focal, double aperture, double distance,
                             double k1, double k2, double k3) {}

// ---------------------------------------------------------------------------------------------

    private String maker;
    private String model;
    private String type; // rectilinear, fisheye, etc.
    private double cropFactor;
    private AspectRatio aspectRatio;
    private NumericRange focalRange;
    private NumericRange apertureRange;
    private List<String> mounts = new ArrayList<>();
    private List<Distortion> distortions = new ArrayList<>();
    private List<Tca> tcaEntries = new ArrayList<>();
    private List<Vignetting> vignettingEntries = new ArrayList<>();

    // ---------------------------------------------------------------------------------------------

    void setMaker(String maker) {
        this.maker = maker;
    }

    void setModel(String model) {
        this.model = model;
    }

    void setType(String type) {
        this.type = type;
    }

    void setCropFactor(double cropfactor) {
        this.cropFactor = cropfactor;
    }

    void setAspectRatio(AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    void addMount(String mount) {
        this.mounts.add(mount);
    }

    void addDistortion(Distortion distortion) {
        this.distortions.add(distortion);
    }

    void addTca(Tca tca) {
        this.tcaEntries.add(tca);
    }

    void addVignetting(Vignetting vignetting) {
        this.vignettingEntries.add(vignetting);
    }

    void setFocalRange(NumericRange focalRange) {
        this.focalRange = focalRange;
    }

    void setApertureRange(NumericRange apertureRange) {
        this.apertureRange = apertureRange;
    }

    // ---------------------------------------------------------------------------------------------

    public String getMaker() {
        return maker;
    }

    public String getModel() {
        return this.model;
    }

    public double getCropFactor() {
        return cropFactor;
    }

    public AspectRatio getAspectRatio() {
        return aspectRatio;
    }

    public NumericRange getFocalRange() {
        return focalRange;
    }

    public NumericRange getApertureRange() {
        return apertureRange;
    }

    public List<String> getMounts() {
        return mounts;
    }

    public List<Distortion> getDistortions() {
        return distortions;
    }

    public List<Tca> getTcaEntries() {
        return tcaEntries;
    }

    public List<Vignetting> getVignettingEntries() {
        return vignettingEntries;
    }



    // ---------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("%s %s (%.2f) Type=%s", maker, model, cropFactor, type);
    }

    public void print() {
        Lens lens = this;
        System.out.println(lens);
        System.out.println("   Aspect ratio: " + lens.getAspectRatio());
        System.out.println("   Mounts:");
        System.out.println("   Focal range: " + lens.getFocalRange());
        System.out.println("   Aperture range: " + lens.getApertureRange());
        for (String m : lens.getMounts()) {
            System.out.println("      " + m);
        }
        System.out.println("   Distortions:");
        for (Lens.Distortion d : lens.getDistortions()) {
            System.out.println("      " + d);
        }
        System.out.println("   TCA:");
        for (Lens.Tca tca : lens.getTcaEntries()) {
            System.out.println("      " + tca);
        }
        System.out.println("   Vignetting:");
        for (Lens.Vignetting vig : lens.getVignettingEntries()) {
            System.out.println("      " + vig);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /*
    Lensfun doesn't just do a direct string match. To search effectively in Java, you should add a
    helper method to your Lens class or a service:
     */
    public boolean matches(String query) {
        String normalizedQuery = query.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normalizedLens = (maker + model).toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalizedLens.contains(normalizedQuery);
    }

}
