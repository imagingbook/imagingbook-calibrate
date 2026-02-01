/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class Utils {

    private Utils() {}

    public static Path getAppDataDirectory(String appName) {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        Path base;

        if (os.contains("win")) {
            // Windows: AppData/Local
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                base = Paths.get(localAppData);
            } else {
                base = Paths.get(userHome, "AppData", "Local");
            }
        } else if (os.contains("mac")) {
            // macOS: Library/Application Support
            base = Paths.get(userHome, "Library", "Application Support");
        } else {
            // Linux/Unix: .local/share
            String xdgData = System.getenv("XDG_DATA_HOME");
            if (xdgData != null) {
                base = Paths.get(xdgData);
            } else {
                base = Paths.get(userHome, ".local", "share");
            }
        }

        // return base.resolve(appName).resolve("lensfun");
        return base.resolve(appName);
    }

    public static void main(String[] args) {
        Path p = getAppDataDirectory("lensfun").resolve("foo");
        System.out.println(p.toAbsolutePath());
    }


}
