package com.github.kaeluka.cflat.storage;

import io.usethesource.capsule.Map;

import java.util.stream.IntStream;

public class ImmutableStorage<T> implements Storage<T> {

    private Map.Immutable<Integer, Object> map;

    public ImmutableStorage() {
        this(io.usethesource.capsule.Map.Immutable.of());
    }

    private ImmutableStorage(Map.Immutable<Integer, Object> map) {
        this.map = map;
    }

    @Override
    public T get(final int i) {
        return (T)this.map.get(i);
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        return new ImmutableStorage<>(this.map.__put(i, x));
    }

    @Override
    public Storage<T> clear() {
        return new ImmutableStorage<>();
    }
}
