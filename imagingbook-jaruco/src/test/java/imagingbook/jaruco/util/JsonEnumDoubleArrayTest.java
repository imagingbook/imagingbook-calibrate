package imagingbook.jaruco.util;

import imagingbook.common.math.Matrix;
import org.junit.jupiter.api.Test;

import static imagingbook.jaruco.util.JsonEnumDoubleArray.item1;
import static imagingbook.jaruco.util.JsonEnumDoubleArray.item2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonEnumDoubleArrayTest {

    @Test
    void getResourceClassTest() {
        for (var item : JsonEnumDoubleArray.values()) {
            assertEquals(double[][].class, item.getResourceClass());
        }
    }

    @Test
    void readObjectTest0() {
        for (var item : JsonEnumDoubleArray.values()) {
            assertInstanceOf(double[][].class, item.readObject(double[][].class));
            assertInstanceOf(double[][].class, item.readObject(item.getResourceClass()));
            assertInstanceOf(double[][].class, item.readObject());
        }
    }

    @Test
    void readObjectTest1() {
        for (var item : JsonEnumDoubleArray.values()) {
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
    void readObjectTest2() {
        for (var item : JsonEnumDoubleArray.values()) {
            double[][] a = item.readObject(item.getResourceClass());
            // System.out.println(item + " = \n" + Matrix.toString(a));
            assertNotNull(a);
        }
        {
            double[][] a1 = item1.readObject(item1.getResourceClass());
            assertEquals(2, a1.length);
            assertEquals(4, a1[0].length);
        }
        {
            double[][] a2 = item2.readObject(item2.getResourceClass());;
            assertEquals(4, a2.length);
            assertEquals(4, a2[0].length);
        }
    }

    @Test
    void readObjectTest3() {
        for (var item : JsonEnumDoubleArray.values()) {
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