package com.github.kaeluka.cflat.storage;

public final class EmptyStorage<T> implements Storage<T> {

    @Override
    public T get(final int i) {
        return null;
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        throw new UnsupportedOperationException("can't store into empty storage!");
    }

    @Override
    public Storage<T> clearAll() {
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return 0;
    }

    @Override
    public Storage<T> emptyCopy() {
        return this;
    }

    @Override
    public long bytesUsed() {
        return 0;
    }
}
