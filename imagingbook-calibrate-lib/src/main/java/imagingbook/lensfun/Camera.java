/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import java.util.List;

public record Camera(
        String primaryMaker,
        String primaryModel,
        List<String> allMakers,
        List<String> allModels,
        String mount,
        double cropFactor
    ) {

    // This is what the user sees in the dropdown menu
    public String getDisplayName() {
        return primaryMaker + " " + primaryModel;
    }

    // This helps the user see why they found this camera if they typed an alias
    public String getFullIdentity() {
        return getDisplayName() + (allModels.size() > 1 ? " (also known as: " + String.join(", ", allModels) + ")" : "");
    }
}