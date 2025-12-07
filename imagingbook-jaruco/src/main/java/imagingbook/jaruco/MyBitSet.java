package imagingbook.jaruco;

import java.util.BitSet;

/**
 * An extension of Java's native {@link BitSet} class to represent an
 * immutable bit vector that is aware of its full length;
 */
public class MyBitSet extends BitSet {

    final int nbits;

    public MyBitSet(int nbits) {
        super(nbits);
        this.nbits = nbits;
    }

    public MyBitSet (int... vals) {
        this(vals.length);
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == 1) {
                super.set(i);
            }
            else if (vals[i] != 0) {
                throw new IllegalArgumentException("only 0/1 values allowed");
            }
        }
    }

    public MyBitSet (byte[] vals) {
        this(vals.length);
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == 1) {
                super.set(i);
            }
            else if (vals[i] != 0) {
                throw new IllegalArgumentException("only 0/1 values allowed");
            }
        }
    }

    public MyBitSet (char[] vals) {
        this(vals.length);
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == '1') {
                super.set(i);
            }
            else if (vals[i] != '0') {
                throw new IllegalArgumentException("only 0/1 values allowed");
            }
        }
    }

    public MyBitSet (String str) {
        this(str.toCharArray());
    }

    // ----------------------------------------------------------
    // alteratively make mutable values but immutable length!
    // clip access to initial bounds (like a bit[]).
    // Alternatively use own BitVector implementation?

    @Override
    public void set(int bitIndex) {
        throw new UnsupportedOperationException("instances are immutable, cannot use set()");
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("instances are immutable, cannot use set()");
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        throw new UnsupportedOperationException("instances are immutable, cannot use set()");
    }

    // ----------------------------------------------------------



}
