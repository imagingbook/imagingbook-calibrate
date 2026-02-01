/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.nio.file.Path;

public final class Settings {

    public static final String APP_NAME = "Java-Lensfun-App";
    public static final Path LOCAL_LENSFUN_DB_PATH = Utils.getAppDataDirectory("local_lensfun_db");  // "local_lensfun_db\\";

    // API to list files
    static final String API_URL = "https://api.github.com/repos/lensfun/lensfun/contents/data/db";
    // Domain for RAW data
    static final String RAW_URL_BASE = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/";

    static final String DTD_FILE_URL = "https://raw.githubusercontent.com/lensfun/lensfun/master/data/db/lensfun-database.dtd";
    static final String DTD_FILE_NAME = DTD_FILE_URL.substring(DTD_FILE_URL.lastIndexOf('/') + 1);

    private Settings() {
    }

}
