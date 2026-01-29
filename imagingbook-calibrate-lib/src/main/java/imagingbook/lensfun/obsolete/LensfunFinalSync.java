/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun.obsolete;

import imagingbook.lensfun.LensfunManager;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class LensfunFinalSync {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    // API to list files
    private static final String API_URL = "https://api.github.com/repos/lensfun/lensfun/contents/data/db";
    // Domain for RAW data
    private static final String RAW_URL_BASE = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/";

    public void upDateLocalLensfunDB() throws Exception {
        Path localPath = Path.of(LensfunManager.LOCAL_LENSFUN_DB_PATH);
        if (Files.notExists(localPath)) Files.createDirectories(localPath);

        System.out.println("Step 1: Fetching file list...");
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
            // We force the RAW URL here
            String downloadUrl = RAW_URL_BASE + name;

            HttpRequest fileReq = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .build();

            // Download as bytes to avoid any encoding weirdness
            byte[] content = CLIENT.send(fileReq, HttpResponse.BodyHandlers.ofByteArray()).body();
            String contentStr = new String(content);

            if (contentStr.contains("<lensdatabase") || contentStr.contains("<!DOCTYPE")) {
                Files.write(localPath.resolve(name), content);
                System.out.println("Successfully saved: " + name);
            } else {
                System.err.println("Skipping " + name + " - content does not look like Lensfun data.");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new LensfunFinalSync().upDateLocalLensfunDB();
    }
}