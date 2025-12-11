package imagingbook.jaruco.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListUtils {

    public static <T> List<T> reversedCopy(List<T> original) {
        List<T> copy = new ArrayList<>(original); // copy original
        Collections.reverse(copy);                // reverse the copy
        return copy;
    }

}
