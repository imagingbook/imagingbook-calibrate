package imagingbook.jaruco;

import org.junit.jupiter.api.Test;

import java.util.BitSet;


import static imagingbook.jaruco.ArucoDictionary.normHamming;
import static imagingbook.jaruco.ArucoDictionary.toBitSet;
import static imagingbook.jaruco.ByteArrayUtils.toString01;
import static org.junit.jupiter.api.Assertions.*;

class ArucoDetectorTest {

    @Test
    void bitSetTestXor() {
        BitSet bs1 = toBitSet("000110101000100");
        BitSet bs2 = toBitSet("111010011001101");
        BitSet bs3 = (BitSet) bs2.clone();
        System.out.println("bs1 = " + toString01(bs1));
        System.out.println("bs2 = " + toString01(bs2));
        System.out.println("bs3 = " + toString01(bs3));

        int h = normHamming(bs1, bs2);
        System.out.println("h = " +h);



    }
}