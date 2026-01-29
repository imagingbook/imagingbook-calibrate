/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LensfunUpdater {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    // API to list files
    private static final String API_URL = "https://api.github.com/repos/lensfun/lensfun/contents/data/db";
    // Domain for RAW data
    private static final String RAW_URL_BASE = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/";

    public void upDateLocalLensfunDB() throws Exception {
        Path localPath = Path.of(LensfunManager.LOCAL_LENSFUN_DB_PATH);
        if (Files.notExists(localPath)) Files.createDirectories(localPath);

        // 1. Download the DTD file first
        System.out.println("Step 1: Fetching DTD file ...");
        String dtdUrl = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/lensfun-database.dtd";
        downloadRawFile(dtdUrl, localPath.resolve("lensfun-database.dtd"));
        System.out.println("DTD synchronized.");


        // 2. Fetch the file list from API
        System.out.println("Step 2: Fetching file list...");
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Java-Lensfun-App") // Required by GitHub
                .build();

        String json = CLIENT.send(listReq, HttpResponse.BodyHandlers.ofString()).body();

        // Extracting filenames from JSON
        List<String> files = new ArrayList<>();
        Matcher m = Pattern.compile("\"name\":\"([^\"]+\\.xml)\"").matcher(json);
        while (m.find()) files.add(m.group(1));

        System.out.println("Found " + files.size() + " files. Starting download...");

        for (String name : files) {
            String downloadUrl = RAW_URL_BASE + name;
            downloadRawFile(downloadUrl, localPath.resolve(name));
        }
    }
    /**
     * Helper to download raw bytes and save them,
     * ensuring we don't accidentally save HTML error pages.
     */
    private void downloadRawFile(String url, Path target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Java-Lensfun-App")
                .build();

        byte[] bytes = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        String check = new String(bytes, 0, Math.min(bytes.length, 100));

        // Valid Lensfun files start with DOCTYPE, <lensdatabase, or <!ELEMENT (for DTD)
        if (check.contains("<!DOCTYPE") || check.contains("<lensdatabase") || check.contains("<!ELEMENT")) {
            Files.write(target, bytes);
            System.out.println("Saved: " + target.getFileName());
        } else {
            System.err.println("Failed to download raw data for: " + url);
        }
    }


    public static void main(String[] args) throws Exception {
        new LensfunUpdater().upDateLocalLensfunDB();
    }
}