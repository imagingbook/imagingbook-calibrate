package imagingbook.jaruco.util;

import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.util.JsonResourceExample.item1;
import static imagingbook.jaruco.util.JsonResourceExample.item2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonResourceExampleTest {

    @Test
    void getResourceClassTest() {
        for (var item : JsonResourceExample.values()) {
            assertEquals(double[][].class, item.getResourceClass());
        }
    }

    @Test
    void readObjectTest0() {
        for (var item : JsonResourceExample.values()) {
            assertInstanceOf(double[][].class, item.readObject(double[][].class));
            assertInstanceOf(double[][].class, item.readObject(item.getResourceClass()));
            assertInstanceOf(double[][].class, item.readObject());
        }
    }

    @Test
    void readObjectTest1() {
        for (var item : JsonResourceExample.values()) {
            double[][] a = item.readObject(double[][].class);
            // System.out.println(item + " = \n" + Matrix.toString(a));
            assertNotNull(a);
        }
        {
            double[][] a1 = item1.readObject(double[][].class);
            assertEquals(2, a1.length);
            assertEquals(4, a1[0].length);
        }
        {
            double[][] a2 = item2.readObject(double[][].class);
            assertEquals(4, a2.length);
            assertEquals(4, a2[0].length);
        }
    }

    @Test
    void readObjectTest3() {
        for (var item : JsonResourceExample.values()) {
            double[][] a = item.readObject();
            // System.out.println(item + " = \n" + Matrix.toString(a));
            assertNotNull(a);
        }
        {
            double[][] a1 = item1.readObject();
            assertEquals(2, a1.length);
            assertEquals(4, a1[0].length);
        }
        {
            double[][] a2 = item2.readObject();;
            assertEquals(4, a2.length);
            assertEquals(4, a2[0].length);
        }
    }
}