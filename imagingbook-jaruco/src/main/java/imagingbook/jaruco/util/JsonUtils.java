package imagingbook.jaruco.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {

    // public static double[][] loadDoubleArray2d(Class<?> clazz, String resourcePath) {
    //     String classPath = clazz.getResource("").toExternalForm(); // safe for JARs
    //
    //     ObjectMapper mapper = new ObjectMapper();
    //
    //     try (InputStream is = clazz.getClassLoader().getResourceAsStream(resourcePath)) {
    //         if (is == null) {
    //             throw new IllegalArgumentException("Resource not found: " + classPath + resourcePath);
    //         }
    //         return mapper.readValue(is, double[][].class);
    //     } catch (IOException e) {
    //         throw new RuntimeException("something went wrong while loading " + resourcePath, e);
    //     }
    // }

    /**
     * Generic JSON reader.
     * Tries to read a single arbitrary object from a JSON file.
     * If successful, the read object is returned as a {@link Object} instance
     * that must be typecast to the actual type. Example:
     * <pre>
     * double[][] matrix =
     *   (double[][]) loadObject(rootCLass, resourcePath, double[][].class);
     * </pre>
     * @param resourceClass the anchor class for the relative path to this resource
     * @param resourcePath the relative path from the root class to the resource
     * @param objectClass the type of object expected to be read
     * @return
     */
    public static Object loadObject(Class<?> resourceClass, String resourcePath, Class<?> objectClass) {
        String classPath = resourceClass.getResource("").toExternalForm(); // safe for JARs

        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = resourceClass.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + classPath + resourcePath);
            }
            return mapper.readValue(is, objectClass);
        } catch (IOException e) {
            throw new RuntimeException("something went wrong while loading " + resourcePath, e);
        }
    }

}

