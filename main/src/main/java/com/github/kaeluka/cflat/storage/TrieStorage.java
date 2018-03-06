package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.Mutable;

import java.util.Arrays;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class TrieStorage<T> extends SizedStorage<T> {
    public final Object[][][][] data = new Object[0x80][][][];

    @Override
    public T get(final int idx) {
        checkIndexIsNonnegative(idx);

        Object[][][] d1;
        final int coord1 = idx >> 24 & 0x7F;
        if ((d1 = data[coord1]) == null) {
            return null;
        }
        Object[][] d2;
        final int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            return null;
        }
        Object[] d3;
        final int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            return null;
        }

        return (T) d3[idx & 0xFF];
    }

    @Override
    public T get2(final int idx, final Mutable<T> v2) {
        checkIndexIsNonnegative(idx);
        checkIndexIsNonnegative(idx+1);
        assert idx >= 0;

        int coord1 = idx >> 24 & 0x7F;
        int coord2 = idx >> 16 & 0xFF;
        int coord3 = idx >> 8 & 0xFF;
        int coord4 = idx & 0xFF;

        Object[][][] d1;
        Object[][] d2;
        Object[] d3;
        if ((d1 = data[coord1]) == null ||
                (d2 = d1[coord2]) == null ||
                (d3 = d2[coord3]) == null) {
            return null;
        }
        if (coord4 != 0xFF) {
            v2.x = (T) d3[coord4+1];
        } else {
            handleOverflow(v2, d1, coord1, d2, coord2, coord3);
        }
        return (T) d3[coord4];
    }

    private void handleOverflow(final Mutable<T> v2, final Object[][][] d1, final int coord1, final Object[][] d2, final int coord2, final int coord3) {
        if (coord3 != 0xFF) {
            v2.x = (T) d2[coord3+1][0];
        } else {
            if (coord2 != 0xFF) {
                v2.x = (T) d1[coord2+1][0][0];
            } else {
                if (coord1 != 0xFF) {
                    v2.x = (T) data[coord1+1][0][0][0];
                } else {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }
        }
    }

    @Override
    public Storage<T> set(final int idx, final T x) {
        checkIndexIsNonnegative(idx);
        updateMaxIdx(idx);

        Object[][][] d1;
        Object[][] d2;
        Object[] d3;
        int coord1 = idx >> 24 & 0xFF;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new Object[0x100][][];
        }
        int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            d2 = d1[coord2] = new Object[0x100][];
        }
        int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            d3 = d2[coord3] = new Object[0x100];
        }

        d3[idx & 0xFF] = x;
        return this;
    }

    @Override
    public Storage<T> set2(final int idx, final T x, final T y) {
        checkIndexIsNonnegative(idx);
        checkIndexIsNonnegative(idx+1);
        updateMaxIdx(idx);

        Object[][][] d1;
        Object[][] d2;
        Object[] d3;
        final int coord1 = idx >> 24 & 0xFF;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new Object[0x100][][];
        }
        final int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            d2 = d1[coord2] = new Object[0x100][];
        }
        final int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            d3 = d2[coord3] = new Object[0x100];
        }

        final int coord4 = idx & 0xFF;
        d3[coord4] = x;
        if (coord4 != 0xFF) {
            d3[coord4+1] = y;
        } else {
            set(idx+1, y);
        }
        return this;
    }

    @Override
    public Storage<T> clearAll() {
        Arrays.fill(data, null);
        return this;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new HashMapStorage<>();
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    @Override
    public long bytesUsed() {
        System.err.println("returning bogus value for TrieStorage::bytesUsed");
        return -1;
    }
}
