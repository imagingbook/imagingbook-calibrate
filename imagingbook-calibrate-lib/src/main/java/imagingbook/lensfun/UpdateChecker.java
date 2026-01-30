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
import java.util.Optional;

public class UpdateChecker {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String API_URL = "https://api.github.com/repos/lensfun/lensfun/contents/data/db";
    private static final Path ETAG_STORE = Path.of(LensfunManager.LOCAL_LENSFUN_DB_PATH, ".etag");     // Path.of("./local_db/.etag");

    /**
     * This method checks the GitHub API. It compares the ETag of the directory listing.
     * If it matches, we stop. If it doesn't, we proceed with the download.
     * @return
     * @throws Exception
     */
    @Deprecated
    public boolean isUpdateAvailable() throws Exception {
        String lastEtag = Files.exists(ETAG_STORE) ? Files.readString(ETAG_STORE) : "";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("User-Agent", "Java-Lensfun-App")
                .header("If-None-Match", lastEtag) // This is the magic header
                .build();

        HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() == 304) {
            System.out.println("No updates found (304 Not Modified).");
            return false;
        } else if (response.statusCode() == 200) {
            System.out.println("Update available!");
            // Store the new Etag for next time
            Optional<String> newEtag = response.headers().firstValue("ETag");
            newEtag.ifPresent(s -> {
                try { Files.writeString(ETAG_STORE, s); } catch (Exception ignored) {}
            });
            return true;
        }

        return false;
    }

    /**
     * Performs an update of lensfun's XML files by first checking the upstream ETag
     *
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
     */
    public void performSmartUpdate() {
        try {
            String lastEtag = Files.exists(ETAG_STORE) ? Files.readString(ETAG_STORE) : "";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "Java-Lensfun-App")
                    .header("If-None-Match", lastEtag)
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 304) {
                System.out.println("Database is current. No action needed.");
                return;
            }

            if (response.statusCode() == 200) {
                // 1. Keep the new ETag in memory
                String newEtag = response.headers().firstValue("ETag").orElse("");

                // 2. Perform the actual file downloads (Pass the JSON body to save an API call!)
                boolean success = new LensfunUpdater().upDateLocalLensfunDBWithJson(response.body());

                // 3. ONLY COMMIT if the download was 100% successful
                if (success && !newEtag.isEmpty()) {
                    Files.writeString(ETAG_STORE, newEtag);
                    System.out.println("Update committed. New ETag saved.");
                }
            }
        } catch (Exception e) {
            System.err.println("Update failed: " + e.getMessage());
            // ETag wasn't saved, so it will retry next time.
        }
    }

    public static void main(String[] args) throws Exception {
        // new UpdateChecker().isUpdateAvailable();
        new UpdateChecker().performSmartUpdate();
    }
}
