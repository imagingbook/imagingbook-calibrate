package imagingbook.jaruco.util;

/**
 * Example JSON resources with contents of type double[][]. For testing only.
 */
public enum JsonResourceExample implements JsonResource {
    item1,
    item2
    ;

    @Override
    public Class<?> getResourceClass() {
        return double[][].class;
    }

}
