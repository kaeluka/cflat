package com.github.kaeluka.cflat.storage;

import io.usethesource.capsule.Map;

import java.util.stream.IntStream;

public class ImmutableStorage<T> implements Storage<T> {

    private Map.Immutable<Long, Object> map;

    public ImmutableStorage() {
        this(io.usethesource.capsule.Map.Immutable.of());
    }

    private ImmutableStorage(Map.Immutable<Long, Object> map) {
        this.map = map;
    }

    @Override
    public T get(final long i) {
        return (T)this.map.get(i);
    }

    @Override
    public Storage<T> set(final long i, final T x) {
        return new ImmutableStorage<>(this.map.__put(i, x));
    }

    @Override
    public Storage<T> clear() {
        return new ImmutableStorage<>();
    }
}
