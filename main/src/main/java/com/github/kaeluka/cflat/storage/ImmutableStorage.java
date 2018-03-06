package com.github.kaeluka.cflat.storage;

import io.usethesource.capsule.Map;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

public final class ImmutableStorage<T> implements Storage<T> {

    private final Map.Immutable<Integer, Object> map;
    private final int maxIdx;

    public ImmutableStorage() {
        this(io.usethesource.capsule.Map.Immutable.of(), -1);
    }

    private ImmutableStorage(Map.Immutable<Integer, Object> map, final int maxIdx) {
        this.map = map;
        this.maxIdx = maxIdx;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(final int i) {
        checkIndexIsNonnegative(i);
        return (T)this.map.get(i);
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        checkIndexIsNonnegative(i);
        if (x != null) {
            int newMaxIdx = i > maxIdx ? i : maxIdx;
            return new ImmutableStorage<>(this.map.__put(i, x), newMaxIdx);
        } else {
            return new ImmutableStorage<>(this.map.__remove(i), maxIdx);
        }
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> copy() {
        return this;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new ImmutableStorage<>();
    }

    @Override
    public Storage<T> clearAll() {
        return new ImmutableStorage<>();
    }

    @Override
    public long bytesUsed() {
        return -1;
    }
}
