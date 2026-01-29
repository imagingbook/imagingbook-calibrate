/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun.obsolete;

public class LensfunUpdater {


    public static void upDateLocalLensfunDB() {
        String baseUrl = "https://lensfun.github.io/db/";
        String versionUrl = baseUrl + "versions.json";

        try {
            // 1. Check the version file
            // versions.json looks like: [timestamp, [v1, v2, v3], ["mirror_url"]]
            String json = LensUtils.downloadString(versionUrl);
            System.out.println("json = " + json);

            // 2. Determine the latest version (simplified logic)
            // In a real app, use a JSON library like Jackson or Gson here
            int latestVersion = 2; // Extracted from the JSON array

            // 3. Download the tarball (e.g., version_2.tar.bz2)
            // String archiveUrl = baseUrl + "version_" + latestVersion + ".tar.bz2";
            // downloadAndExtract(archiveUrl, Path.of("./local_db"));

        } catch (Exception e) {
            System.err.println("Update failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------

    public static void main(String[] args) {
        upDateLocalLensfunDB();
    }


}
