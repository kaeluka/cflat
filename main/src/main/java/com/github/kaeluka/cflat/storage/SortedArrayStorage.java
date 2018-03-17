package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.util.IndexCheck;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;

public class SortedArrayStorage<T> extends SizedStorage<T> {
    final TIntArrayList positions = new TIntArrayList(10);
    final ArrayList<T> values = new ArrayList<>(10);

    @Override
    public T get(final int i) {
        IndexCheck.checkIndexIsNonnegative(i);
        if (i >= maxIdxOverapproximation()) {
            return null;
        }
        final int idx = positions.binarySearch(i);
        if (idx >= 0) {
            return values.get(idx);
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        IndexCheck.checkIndexIsNonnegative(i);
        this.updateMaxIdx(i);
        final int idx = positions.binarySearch(i);
        if (idx < 0) {
            positions.insert(-(idx+1), i);
            values.add(-(idx+1), x);
        } else {
            values.set(idx, x);
        }
        return this;
    }

    @Override
    public Storage<T> clearAll() {
        this.positions.clear();
        this.values.clear();
        return this;
    }

    @Override
    public Iterator<Integer> nonNullIndices() {
        return new Iterator<Integer>() {
            private final TIntIterator iter =
                    positions.iterator();;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Integer next() {
                return iter.next();
            }
        };
    }

    @Override
    public int findFirst(final T x, final int max) {
        final int i = values.indexOf(x);
        if (i >= 0) {
            return positions.get(i);
        } else {
            return -1;
        }
    }

    @Override
    public int findLast(final T x) {
        final int i = values.lastIndexOf(x);
        if (i >= 0) {
            return positions.get(i);
        } else {
            return -1;
        }
    }

    @Override
    public Storage<T> emptyCopy() {
        return new SortedArrayStorage<>();
    }

    @Override
    public long bytesUsed() {
        return ObjectSizes.ARRAYLIST_SIZE(this.positions)
                + ObjectSizes.ARRAYLIST_SIZE(this.values)+
                + ObjectSizes.INT // maxIdx
                + ObjectSizes.OBJ_HEADER;
    }
}
