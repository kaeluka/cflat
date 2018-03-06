package com.github.kaeluka.cflat.storage;

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
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new PMAStorage<>();
    }

    @Override
    public long bytesUsed() {
        throw new UnsupportedOperationException();
//        return 0;
    }
}
