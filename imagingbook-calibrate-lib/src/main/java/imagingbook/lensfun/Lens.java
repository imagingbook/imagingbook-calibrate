/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lens {
    // Identity
    private String maker;
    private String model;
    private String type; // rectilinear, fisheye, etc.
    private double cropFactor;
    private Range focalRange;
    private Range apertureRange;

    private List<String> mounts = new ArrayList<>();


    // Calibration Data
    private List<Distortion> distortions = new ArrayList<>();
    private List<Tca> tcaEntries = new ArrayList<>();
    private List<Vignetting> vignettingEntries = new ArrayList<>();

    // ---------------------------------------------------------------------------------------------

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCropFactor(String cropfactor) {
        this.cropFactor = Double.parseDouble(cropfactor);
    }

    public void addMount(String mount) {
        this.mounts.add(mount);
    }

    public void addDistortion(Distortion distortion) {
        this.distortions.add(distortion);
    }

    public void addTca(Tca tca) {
        this.tcaEntries.add(tca);
    }

    public void addVignetting(Vignetting vignetting) {
        this.vignettingEntries.add(vignetting);
    }

    // ---------------------------------------------------------------------------------------------

    public record Range(double min, double max) {}

    // public record Distortion(double focal, String model, double k1, double k2, double k3) {}
    public record Distortion(double focal, String model,
            double k1, double k2, double a, double b, double c) {
        // Helper to check if this entry actually contains data
        public boolean isValid() {
            return model != null && !model.isEmpty();
        }
    }

    public record Tca(double focal, String model, double kr, double kb) {}
    public record Vignetting(double focal, double aperture, double distance, String model, double k1, double k2, double k3) {}

    public double getCropFactor() { return cropFactor; }
    public Range getFocalRange() { return focalRange; }
    public Range getApertureRange() { return apertureRange; }
    public List<String> getMounts() { return mounts; }
    public List<Distortion> getDistortions() { return distortions; }
    public List<Tca> getTcaEntries() { return tcaEntries; }
    public List<Vignetting> getVignettingEntries() { return vignettingEntries; }

    // ---------------------------------------------------------------------------------------------

    // Getters, Setters, and a toString() for debugging
    @Override
    public String toString() {
        return String.format("%s %s (%.2f): %s", maker, model, cropFactor, Arrays.toString(getMounts().toArray(new String[0])));
    }

    // ---------------------------------------------------------------------------------------------

    // Add standard Getters/Setters here...

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
