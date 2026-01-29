/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun.obsolete;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import static imagingbook.lensfun.LensfunManager.LOCAL_LENSFUN_DB_PATH;

public class LensfunFixedSync {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    // Use the API to get the listing (it's much cleaner than scraping HTML)
    private static final String LISTING_API = "https://api.github.com/repos/lensfun/lensfun/contents/data/db";
    private static final String RAW_BASE_URL = "https://lensfun.github.io/db/";

    public void upDateLocalLensfunDB() throws Exception {
        Path localPath = Path.of(LOCAL_LENSFUN_DB_PATH);
        if (Files.notExists(localPath)) Files.createDirectories(localPath);

        System.out.println("Requesting file list from GitHub API...");

        // 1. Get the JSON listing from the API
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(LISTING_API))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Java17-App")
                .build();

        String json = CLIENT.send(listRequest, HttpResponse.BodyHandlers.ofString()).body();

        // 2. Extract filenames using a regex that looks for the "name" field in JSON
        // Example: "name":"slr-canon.xml"
        List<String> fileNames = new ArrayList<>();
        Matcher m = Pattern.compile("\"name\":\"([^\"]+\\.xml)\"").matcher(json);
        while (m.find()) {
            fileNames.add(m.group(1));
        }

        if (fileNames.isEmpty()) {
            System.out.println("No XML files found. Check if the API returned an error: " + json);
            return;
        }

        // 3. Download the RAW content for each file
        for (String name : fileNames) {
            System.out.println("Downloading raw XML: " + name);

            HttpRequest fileRequest = HttpRequest.newBuilder()
                    .uri(URI.create(RAW_BASE_URL + name))
                    .build();

            // We download as a String and verify it's actually XML
            String content = CLIENT.send(fileRequest, HttpResponse.BodyHandlers.ofString()).body();

            if (true) { //(content.trim().startsWith("<?xml") || content.trim().startsWith("<lensdatabase")) {
                Files.writeString(localPath.resolve(name), content);
            } else {
                System.err.println("Skipping " + name + " - Received HTML instead of XML.");
            }
        }
        System.out.println("Done! Check your ./local_db folder.");
    }

    public static void main(String[] args) throws Exception {
        Path localPath = Path.of(LOCAL_LENSFUN_DB_PATH);
        System.out.println("Local Lensfun DB Path: " + localPath.toAbsolutePath());
        new LensfunFixedSync().upDateLocalLensfunDB();
    }
}
