package com.github.kaeluka.cflat.storage;

import java.util.Arrays;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class ShallowTrieStorage<T> implements Storage<T> {
    public final Object[][] data = new Object[0x7FFF][];

    private int maxIdx = -1;

    public T get(final int idx) {
        checkIndexIsNonnegative(idx);
        int coord1 = idx >> 16 & 0x7FFF;

        Object[] d1;
        if ((d1 = data[coord1]) == null) {
            return null;
        }

        return (T) d1[idx & 0xFFFF];
    }

    public Storage<T> set(final int idx, final T x) {
        checkIndexIsNonnegative(idx);
        //FIXME implement set2
        assert idx >= 0;
        if (idx > maxIdx) {
            maxIdx = idx;
        }
        int coord1 = idx >> 16 & 0x7FFF;

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

    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    public Storage<T> emptyCopy() {
        return new HashMapStorage<>();
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    public long bytesUsed() {
        System.err.println("returning bogus value for TrieStorage::bytesUsed");
        return -1;
    }
}
