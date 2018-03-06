package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.Mutable;

import java.util.function.Function;

public abstract class SizedStorage<T> implements Storage<T> {
    private int maxIdx = -1;

    @Override
    public Storage<T> set2(final int i, final T x, final T y) {
        updateMaxIdx(i+1);
        return Storage.super.set2(i, x, y);
    }

    @Override
    public Storage<T> computeIfAbsent(final int i, final Function<Integer, T> f, final Mutable<T> result) {
        updateMaxIdx(i);
        return Storage.super.computeIfAbsent(i, f, result);
    }

    @Override
    public Storage<T> setRange(final int pos, final T x, final int length) {
        updateMaxIdx(pos+length);
        return Storage.super.setRange(pos, x, length);
    }

    @Override
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        updateMaxIdx(Math.max(source + length, dest + length));
        return Storage.super.moveRange(source, dest, length);
    }

    @Override
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        updateMaxIdx(Math.max(source + length, dest + length));
        return Storage.super.copyRange(source, dest, length);
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    protected int getMaxIdx() {
        return maxIdx;
    }

    protected void updateMaxIdx(final int i) {
        if (i > maxIdx) {
            maxIdx = i;
        }
    }
}
