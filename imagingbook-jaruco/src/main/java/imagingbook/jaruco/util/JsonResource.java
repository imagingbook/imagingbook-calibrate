package imagingbook.jaruco.util;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import imagingbook.core.resource.NamedResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * An extension of {@link NamedResource} for defining a collection of JSON-backed named resources.
 * This interface is to be implemented ba an enum-type representing one collection.
 * Each enum-item {@code itemX} of an implementing enum class
 * {@code com.foo.MyJsonResource.java} must be backed by an associated JSON resource file
 * {@code itemX.json} in
 * {@code com.foo.MyJsonResource-data}.
 * It is assumed that all JSON objects such a collection are of the same Java class.
 */
public interface JsonResource extends NamedResource {

    public final String DefaultFileExtension = "json";

    /**
     * Implementing enum classes may override this method if another default file extension is required.
     * @return the file extension for resource files, e.g. "json" (without dot,
     * see {@link #DefaultFileExtension}).
     */
    public default String getFileExtension() {
        return DefaultFileExtension;
    }

    @Override
    public default String getFileName() {
        String itemname = this.toString();
        return itemname + "." + getFileExtension();
    }

    /**
     * Specifies the type of object contained in each {@link JsonResource}.
     * All items in a JSON resource must be of the same type.
     * This method must be implemented by concrete enum types. For example,
     * for objects of type {@code double[][]}, this is done in the following form:
     * <pre>{@code
     *     @Override
     *     public Class<double[][]> getResourceClass() {
     *         return double[][].class;
     *     }
     * }</pre>
     * Exceptions will be thrown when resading the associated JSON file if its content is
     * not of the declared type.
     * @return the object class expected when loading a JSON item
     */
    public Class<?> getResourceClass();

    /**
     * Tries to read the associated JSON file and convert its content to a Java object of
     * the type defined by {@link #getResourceClass()}.
     * @return an object read from the associated JSON resource
     * @param <T> the generic object type
     */
    public default <T> T readObject() {
        return readObject((Class<T>) this.getResourceClass());
    }

    /**
     * Tries to read the associated JSON file and convert its content to a Java object of the
     * specified type. An exception is thrown if the specified type does not match the
     * result from calling {@link #getResourceClass()} on this item.
     * Note: This method is for internal use only (interface 'default' methods are always public),
     * {@link #readObject()} should be used instead.
     * @param type the type of object stored in the JSON resource
     * @return an object of the specified type
     * @param <T> the generic object type
     */
    public default <T> T readObject(Class<T> type) {
        if (!type.equals(this.getResourceClass())) {
            throw new IllegalArgumentException("specified type is not of resource type " +
                    this.getResourceClass());
        }
        // String classPath = resourceClass.getResource("").toExternalForm(); // safe for JARs
        ObjectMapper mapper = new ObjectMapper();
        // System.out.println("rel path = " + getRelativePath());
        // InputStream is = this.getClass().getResourceAsStream(getRelativePath())
        try (InputStream is = this.getStream()) {
            if (is == null) {
                throw new IllegalArgumentException("JSON resource not found: " + getURL());
            }
            return mapper.readValue(is, type);
        } catch (DatabindException e) {
            throw new RuntimeException("could not read object of type " + type.getSimpleName()
                    + " from JSON file", e);
        } catch (IOException e) {
            throw new RuntimeException("something went wrong while opening JSON file " + getURL(), e);
        }
    }

}
