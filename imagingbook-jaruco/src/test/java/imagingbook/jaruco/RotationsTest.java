package imagingbook.jaruco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

class RotationsTest {

    @Test
    void makeRotationPermutation1() {
        int[] perm = RotationUtils.makeRotationPermutation(1);
        int[] expected = { 0 };
        assertArrayEquals(expected, perm);
    }

    @Test
    void makeRotationPermutation2() {
        int[] perm = RotationUtils.makeRotationPermutation(2);
        int[] expected = { 1, 3, 0, 2 };
        // System.out.println(Arrays.toString(perm));
        assertArrayEquals(expected, perm);
    }

    @Test
    void makeRotationPermutation3() {
        int[] perm = RotationUtils.makeRotationPermutation(3);
        int[] expected = { 2, 5, 8, 1, 4, 7, 0, 3, 6 };
        // System.out.println(Arrays.toString(perm));
        assertArrayEquals(expected, perm);
    }

    @Test
    void makeRotationPermutation4() {
        int[] perm = RotationUtils.makeRotationPermutation(4);
        int[] expected = { 3, 7, 11, 15, 2, 6, 10, 14, 1, 5, 9, 13, 0, 4, 8, 12 };
        // System.out.println(Arrays.toString(perm));
        assertArrayEquals(expected, perm);
    }

    @Test
    void makeRotationPermutation5() {
        int[] perm = RotationUtils.makeRotationPermutation(5);
        int[] expected = { 4, 9, 14, 19, 24, 3, 8, 13, 18, 23, 2, 7, 12, 17, 22, 1, 6, 11, 16, 21, 0, 5, 10, 15, 20 };
        // System.out.println(Arrays.toString(perm));
        assertArrayEquals(expected, perm);
    }

    @Test
    void makeRotationPermutation7() {
        int[] perm = RotationUtils.makeRotationPermutation(7);
        int[] expected = { 6, 13, 20, 27, 34, 41, 48, 5, 12, 19, 26, 33, 40, 47,
                4, 11, 18, 25, 32, 39, 46, 3, 10, 17, 24, 31, 38, 45, 2, 9, 16,
                23, 30, 37, 44, 1, 8, 15, 22, 29, 36, 43, 0, 7, 14, 21, 28, 35, 42 };
        // System.out.println(Arrays.toString(perm));
        assertArrayEquals(expected, perm);
    }

    @Test
    void permuteTest0() {
        int[] perm = { 1, 3, 0, 2 };
        char[] data = "HolyMoly".toCharArray(); // data array too long
        assertThrows(IllegalArgumentException.class, () -> {
            RotationUtils.permute(data, perm);
        });
    }

    @Test       // AssertJ example
    void permuteTest0_AssertJ() {
        int[] perm = { 1, 3, 0, 2 };
        char[] data = "HolyMoly".toCharArray(); // data array too long
        assertThatThrownBy(() ->
                RotationUtils.permute(data, perm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not of same length");
    }

    @Test
    void permuteTest1() {
        int[] perm = { 1, 3, 0, 2 };
        char[] data = "Holy".toCharArray();
        char[] expected = "oyHl".toCharArray();
        char[] datap = RotationUtils.permute(data, perm);
        System.out.println("datap = " + new String(datap));
        assertArrayEquals(expected, datap);
    }
}