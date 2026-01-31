/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.io.IOException;
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

    private static final String DTD_FILE_URL = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/lensfun-database.dtd";

    // where to store the .etag string
    private static final Path ETAG_STORE = Path.of(LensfunDatabase.LOCAL_LENSFUN_DB_PATH, ".etag");

    private static final String HttpHeaderName = "User-Agent";
    private static final String HttpHeaderValue = "Java-Lensfun-App";

// ---------------------------------------------------------------------------------------------

    /**
     * This method checks the GitHub API. It compares the ETag of the directory listing.
     * If it matches, we stop. If it doesn't, we proceed with the download.
     * @return
     * @throws Exception
     */
    @Deprecated
    public boolean isUpdateAvailable() {
        try {
            String lastEtag = Files.exists(ETAG_STORE) ? Files.readString(ETAG_STORE) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header(HttpHeaderName, HttpHeaderValue)
                    .header("If-None-Match", lastEtag) // This is the magic header
                    .build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 304) {
                System.out.println("No updates found (304 Not Modified).");
                return false;
            } else if (response.statusCode() == 200) {
                System.out.println("Updates available!");
                // Store the new Etag for next time
                // Optional<String> newEtag = response.headers().firstValue("ETag");
                // newEtag.ifPresent(s -> {
                //     try { Files.writeString(ETAG_STORE, s); } catch (Exception ignored) {}
                // });
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Update failed: " + e.getMessage());
            // ETag wasn't saved, so it will retry next time.
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs an update of lensfun's XML files by first checking the upstream ETag
     *
     * If you save the ETag immediately after receiving the 200 OK from the API, but your download
     * loop crashes halfway through (due to a timeout, power loss, or a "file not found" error), your
     * app will think it’s up-to-date on the next run. You'd be left with a corrupted or partial
     * database and no way to trigger a fix.
     *
     * The "Transactional" Update Pattern:
     * To make this robust, you should follow this sequence:
     * Check: Send the If-None-Match request.
     * Hold: If you get a 200 OK, grab the new ETag from the header but don't save it to disk yet. Keep it in a variable.
     * Execute: Run your full upDateLocalLensfunDB() logic.
     * Commit: Only after the last file is successfully saved to disk, write the new ETag to ./local_db/.etag.
     *
     * // TODO: timestamp.txt file? clear directory?
     */
    public void performSmartUpdate() {
        try {
            String lastEtag = Files.exists(ETAG_STORE) ? Files.readString(ETAG_STORE) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header(HttpHeaderName, HttpHeaderValue)
                    .header("If-None-Match", lastEtag)
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 304) {
                System.out.println("Database is current. No action needed.");
                return;
            }

            if (response.statusCode() == 200) {
                System.out.println("Updating Lensfun Database now ...");
                // 1. Keep the new ETag in memory
                String newEtag = response.headers().firstValue("ETag").orElse("");

                // 2. Perform the actual file downloads (Pass the JSON body to save an API call!)
                String json = response.body();
                boolean success = upDateLocalLensfunDBWithJson(json);

                // 3. ONLY COMMIT if the download was 100% successful
                if (success && !newEtag.isEmpty()) {
                    Files.writeString(ETAG_STORE, newEtag);
                    System.out.println("Update committed. New ETag saved.");
                }
                else {
                    System.out.println("Update failed. No new ETag saved.");
                }
            }
        } catch (Exception e) {
            System.err.println("Update failed: " + e.getMessage());
            // ETag wasn't saved, so it will retry next time.
        }
    }

    // ---------------------------------------------------------------------------------------------

    // public void upDateLocalLensfunDBWithJson() throws Exception {
    //     Path localPath = Path.of(LensfunManager.LOCAL_LENSFUN_DB_PATH);
    //     if (Files.notExists(localPath)) Files.createDirectories(localPath);
    //
    //     // 1. Download the DTD file first
    //     System.out.println("Step 1: Fetching DTD file ...");
    //     // String dtdUrl = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/lensfun-database.dtd";
    //     downloadRawFile(DTD_FILE_URL, localPath.resolve("lensfun-database.dtd"));
    //     System.out.println("DTD synchronized.");
    //
    //
    //     // 2. Fetch the file list from API
    //     System.out.println("Step 2: Fetching file list...");
    //     HttpRequest listReq = HttpRequest.newBuilder()
    //             .uri(URI.create(API_URL))
    //             .header("Accept", "application/vnd.github.v3+json")
    //             .header(HttpHeaderName, HttpHeaderValue) // Required by GitHub
    //             .build();
    //
    //     String json = CLIENT.send(listReq, HttpResponse.BodyHandlers.ofString()).body();
    //
    //     upDateLocalLensfunDBWithJson(json);
    //
    //     // // Extracting filenames from JSON
    //     // List<String> files = new ArrayList<>();
    //     // Matcher m = Pattern.compile("\"name\":\"([^\"]+\\.xml)\"").matcher(json);
    //     // while (m.find()) files.add(m.group(1));
    //     //
    //     // System.out.println("Found " + files.size() + " files. Starting download...");
    //     //
    //     // for (String name : files) {
    //     //     String downloadUrl = RAW_URL_BASE + name;
    //     //     downloadRawFile(downloadUrl, localPath.resolve(name));
    //     // }
    // }

    private boolean upDateLocalLensfunDBWithJson(String json) {
        Path localPath = Path.of(LensfunDatabase.LOCAL_LENSFUN_DB_PATH);
        // Extracting filenames from JSON
        List<String> files = new ArrayList<>();
        Matcher m = Pattern.compile("\"name\":\"([^\"]+\\.xml)\"").matcher(json);
        while (m.find()) {
            files.add(m.group(1));
        }

        System.out.println("Found " + files.size() + " files. Starting download...");

        for (String name : files) {
            String downloadUrl = RAW_URL_BASE + name;
            try {
                downloadRawFile(downloadUrl, localPath.resolve(name));
            } catch (Exception e) {
                System.out.println("Error: failed to download file " + name);
                return false;
            }
        }
        return true;
    }

    /**
     * Helper to download raw bytes and save them,
     * ensuring we don't accidentally save HTML error pages.
     */
    private void downloadRawFile(String url, Path target) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header(HttpHeaderName, HttpHeaderValue)
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("Updates available: " + new LensfunUpdater().isUpdateAvailable());
        // new LensfunUpdater().performSmartUpdate();
    }
}