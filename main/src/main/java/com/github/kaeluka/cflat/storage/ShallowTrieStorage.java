package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;

import java.util.Arrays;
import java.util.function.IntConsumer;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class ShallowTrieStorage<T> implements Storage<T> {
    public final Object[][] data = new Object[0x7FFF][];

    private int maxIdx = -1;

    @Override
    public int findFirst(final T x, final int max) {
        assert max == -1;
        int _max = this.maxIdxOverapproximation();
        for (int i=0; i<_max; ++i) {
            int coord1 = getCoord1(i);

            Object[] d1;
            if ((d1 = data[coord1]) != null) {
                if (x.equals((T) d1[i & 0xFFFF])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int getCoord1(final int i) {
        return i >> 16 & 0x7FFF;
    }

    @Override
    public void foreachNonNull(final IntConsumer f) {
        for (int c1 = 0; c1 < data.length; c1++) {
            final Object[] d1 = data[c1];
            if (d1 != null) {
                for (int c2 = 0; c2 < d1.length; c2++) {
                    if (d1[c2] != null) {
                        f.accept(c2 + c1*0x7FFF);
                    }
                }
            }
        }
    }

    public T get(final int idx) {
        checkIndexIsNonnegative(idx);
        int coord1 = getCoord1(idx);

        Object[] d1;
        if ((d1 = data[coord1]) == null) {
            return null;
        }

        return (T) d1[idx & 0xFFFF];
    }

    public Storage<T> set(final int idx, final T x) {
        checkIndexIsNonnegative(idx);
        if (x == null && !has(idx)) { return this; }
        //FIXME implement set2
        assert idx >= 0;
        if (idx > maxIdx) {
            maxIdx = idx;
        }
        int coord1 = getCoord1(idx);

        Object[] d1;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new Object[0x10000];
        }
        d1[idx & 0xFFFF] = x;
        return this;
    }

    public Storage<T> clearAll() {
        Arrays.fill(data, null);
        return this;
    }

    public int maxIdxOverapproximation() {
        return maxIdx+1;
    }

    public Storage<T> emptyCopy() {
        return new HashMapStorage<>();
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    @Override
    public long bytesUsed() {
        long sz = ObjectSizes.ARRAY_SIZE(data) + ObjectSizes.INT;
        for (final Object[] lvl1 : data) {
            if (lvl1 != null) {
                sz += ObjectSizes.ARRAY_SIZE(lvl1);
            }
        }
        return sz;
    }
}
