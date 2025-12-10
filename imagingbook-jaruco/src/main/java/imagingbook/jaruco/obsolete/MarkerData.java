/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2025 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.jaruco.obsolete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Reading dictionary files copied from
 * C:\_GITHUB\opencv-super\opencv\apps\pattern-tools
 * (originally compressed)
 */
@Deprecated
public class MarkerData {
    private int nmarkers;
    private int markersize;
    private int maxCorrectionBits;
    private Map<String, String> markers; // Map for "marker_0", "marker_1", ...

    // Getters and setters
    public int getNmarkers() { return nmarkers; }
    public void setNmarkers(int nmarkers) { this.nmarkers = nmarkers; }

    public int getMarkersize() { return markersize; }
    public void setMarkersize(int markersize) { this.markersize = markersize; }

    public int getMaxCorrectionBits() { return maxCorrectionBits; }
    public void setMaxCorrectionBits(int maxCorrectionBits) { this.maxCorrectionBits = maxCorrectionBits; }

    public Map<String, String> getMarkers() { return markers; }
    public void setMarkers(Map<String, String> markers) { this.markers = markers; }


    // -----------------------------------------------------------

    static void readJsonFile(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("File not found : " + f.getAbsolutePath());
            return;
        }

        try {
            readJsonFromStream(new FileInputStream(f));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void readJsonFileCompressed(String filename) {
        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("File not found : " + f.getAbsolutePath());
            return;
        }

        try {
            InputStream fileStream = new FileInputStream(f);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            readJsonFromStream(gzipStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    static void readJsonFromStream(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = null;
        try {
            root = mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int nmarkers = root.get("nmarkers").asInt();
        int markersize = root.get("markersize").asInt();
        int maxCorrectionBits = root.get("maxCorrectionBits").asInt();

        String[] markerBits = new String[nmarkers];

        // Map<String, String> markers = new HashMap<>();
        Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (field.startsWith("marker_")) {
                int idx = Integer.parseInt(field.substring(7));
                // System.out.println("marker " + idx);
                String bits = root.get(field).asText();
                // markers.put(field, bits);
                if (idx < 0 || idx >= nmarkers) {
                    System.out.println("[ERROR] out of range marker index: " + idx);
                    continue;
                }
                if (markerBits[idx] != null) {
                    System.out.println("[ERROR] marker already exists: " + idx);
                    continue;
                }
                markerBits[idx] = bits;
            }
        }

        System.out.println("nmarkers: " + nmarkers);
        System.out.println("markersize: " + markersize);
        // System.out.println("marker_0: " + markers.get("marker_0"));

        System.out.println("Listing makerBits:");
        for (int idx = 0; idx < nmarkers; idx++) {
            if (markerBits[idx] == null) {
                System.out.println("[ERROR] missing data for marker index: " + idx);
            }
            System.out.println(idx + ": " + markerBits[idx]);
        }

    }

    // static String FILENAME = "C:/_GITHUB/imagingbook-super/imagingbook-calibrate/imagingbook-calibrate-lib/src/main/resources/imagingbook/aruco/dict-gz/DICT_7X7_1000.json.gz";
    static String filename = "dict-gz/DICT_5x5_1000.json.gz";
    // static String filename = "dict-gz/DICT_7X7_1000.json.gz";

    public static void main(String[] args) {
        // readJsonFile(FILENAME);
        // readJsonFileCompressed(FILENAME);
        try (InputStream is = PredefiedDictionary.class.getResourceAsStream(filename)) {
            assert is != null;
            InputStream gzipStream = new GZIPInputStream(is);
            readJsonFromStream(gzipStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

