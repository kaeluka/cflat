package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.util.IndexCheck;
import com.github.kaeluka.pma.PMA;

public class PMAStorage<T> extends PMA<T> implements Storage<T> {
    private int maxIdx = -1;

    private void updateMaxIdx(final int i) {
        if (i > maxIdx) {
            maxIdx = i;
        }
    }

    @Override
    public T get(final int i) {
        IndexCheck.checkIndexIsNonnegative(i);
        if (i <= maxIdx) {
            return super.get(i);
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        final int sourceStart = find(source);
        final int destStart = find(dest);

        if (destStart == sourceStart) {
            int curIdx = sourceStart;
            System.out.println("fast path");
            IndexCheck.checkIndexIsNonnegative(source);
            IndexCheck.checkIndexIsNonnegative(dest);
            IndexCheck.checkLengthIsNonnegative(length);
            final int diff = dest - source;
            int curKey;
            while (sourceStart < keys.size() &&
                    (curKey = keys.get(sourceStart)) < source+length) {
                keys.set(sourceStart, curKey+diff);
                curIdx++;
            }
            return this;
        } else {
            System.out.println("slow path "+sourceStart+"/"+destStart);
            return Storage.super.moveRange(source, dest, length);
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        IndexCheck.checkIndexIsNonnegative(i);
        updateMaxIdx(i);
        put(i, x);
        return this;
    }

    @Override
    public Storage<T> clearAll() {
        this.clear();
        return this;
    }

    @Override
    public int maxIdxOverapproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new PMAStorage<>();
    }

    @Override
    public long bytesUsed() {
        return ObjectSizes.ARRAYLIST_SIZE(this.keys) +
                ObjectSizes.ARRAYLIST_SIZE(this.values);
    }
}
