/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun.obsolete;

import imagingbook.lensfun.LensfunDatabase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class LensfunManager {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String GITHUB_WEB_URL = "https://github.com/lensfun/lensfun/tree/master/data/db";
    private static final String STATIC_BASE_URL = "https://lensfun.github.io/db/";

    // public void upDateLocalLensfunDB() throws Exception {
    //     System.out.println("Scanning GitHub for database files...");
    //
    //     // 1. Get the HTML of the directory page
    //     String html = downloadString(GITHUB_WEB_URL);
    //
    //     // 2. Extract filenames using Regex
    //     // We look for hrefs that look like ".../data/db/filename.xml"
    //     Set<String> xmlFiles = new HashSet<>();
    //     Pattern pattern = Pattern.compile("data/db/([^\"?#]+\\.xml)");
    //     Matcher matcher = pattern.matcher(html);
    //
    //     while (matcher.find()) {
    //         String file = matcher.group(1);
    //         System.out.println("Found file: " + file);
    //         xmlFiles.add(file);
    //     }
    //
    //     // 3. Download each discovered file from the static mirror
    //     Path localPath = Path.of(LOCAL_LENSFUN_DB_PATH);
    //     System.out.println("Local Lensfun DB Path: " + localPath.toAbsolutePath());
    //     if (Files.notExists(localPath)) Files.createDirectories(localPath);
    //
    //     for (String fileName : xmlFiles) {
    //         Path target = localPath.resolve(fileName);
    //         if (Files.notExists(target)) {
    //             System.out.println("Downloading new file: " + fileName);
    //             String content = downloadString(STATIC_BASE_URL + fileName);
    //             Files.writeString(target, content);
    //         }
    //     }
    // }

    public void upDateLocalLensfunDB() throws Exception {
        System.out.println("Scanning GitHub for database files...");

        // 1. Get the HTML of the GitHub directory page
        String html = downloadString(GITHUB_WEB_URL);

        // 2. Refined Regex
        // We want to capture ONLY the filename from the 'href' attribute
        // Example in HTML: <a href="/lensfun/lensfun/blob/master/data/db/slr-canon.xml" ...>
        Pattern pattern = Pattern.compile("data/db/([^\"?#]+\\.xml)");
        Matcher matcher = pattern.matcher(html);

        Set<String> xmlFiles = new HashSet<>();
        while (matcher.find()) {
            String fileName = matcher.group(1);
            // Sometimes the scraper picks up the full path; we just want the name
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            System.out.println("Found file: " + fileName);
            xmlFiles.add(fileName);
        }

        // Path localPath = Path.of("./local_db");
        Path localPath = Path.of(LensfunDatabase.LOCAL_LENSFUN_DB_PATH);
        if (Files.notExists(localPath)) Files.createDirectories(localPath);

        for (String fileName : xmlFiles) {
            Path target = localPath.resolve(fileName);

            // Use the static URL which is ALWAYS raw XML
            String rawUrl = "https://lensfun.github.io/db/" + fileName;

            System.out.println("Fetching Raw XML: " + fileName);
            String content = downloadString(rawUrl);

            if (content.trim().startsWith("<?xml")) {
                System.out.println("**** good XML: " + fileName);
            }

            // Double-check: If it starts with <!, it's probably HTML/Doctype
            if (true) { //(content.trim().startsWith("<?xml")) {
                Files.writeString(target, content);
            } else {
                System.err.println("Warning: Skipping " + fileName + " - result was not valid XML.");
            }
        }
    }

    private String downloadString(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Java17-Lensfun-App") // GitHub requires a User-Agent
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    // ----------------------------------------------

    public static void main(String[] args) throws Exception {
        Path localPath = Path.of(LensfunDatabase.LOCAL_LENSFUN_DB_PATH);
        System.out.println("Local Lensfun DB Path: " + localPath.toAbsolutePath());
        new LensfunManager().upDateLocalLensfunDB();
    }
}
