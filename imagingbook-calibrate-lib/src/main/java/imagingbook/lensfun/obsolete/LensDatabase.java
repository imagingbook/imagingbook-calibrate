/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun.obsolete;

import imagingbook.lensfun.LensfunDatabase;

import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class LensDatabase {
    // private static final Path LOCAL_DIR = Path.of("./local_db");

    public List<String> searchLens(String keyword) throws Exception {
        Path localPath = Path.of(LensfunDatabase.LOCAL_LENSFUN_DB_PATH);
        System.out.println("Local Lensfun DB Path: " + localPath.toAbsolutePath());
        try (var paths = Files.walk(localPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .parallel() // Use all CPU cores to search faster
                    .flatMap(path -> {
                        try {
                            String content = Files.readString(path);
                            return extractLenses(content, keyword).stream();
                        } catch (Exception e) {
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    private List<String> extractLenses(String xml, String keyword) {
        // A simple "quick and dirty" search within the XML content
        // In a real app, you would use a proper XML parser (DOM or StAX)
        return java.util.Arrays.stream(xml.split("<lens>"))
                .filter(segment -> segment.contains(keyword))
                .map(segment -> {
                    // Extract the <model> tag value
                    int start = segment.indexOf("<model>") + 7;
                    int end = segment.indexOf("</model>");
                    return (start > 6 && end > start) ? segment.substring(start, end) : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // -------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        LensDatabase lensDatabase = new LensDatabase();
        List<String> lenses = lensDatabase.searchLens("Nikon");
        for (String lensName : lenses) {
            System.out.println(lensName);
        }

    }
}
