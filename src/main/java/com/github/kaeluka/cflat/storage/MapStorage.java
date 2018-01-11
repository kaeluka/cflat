package com.github.kaeluka.cflat.storage;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class MapStorage<T> implements Storage<T> {
    private final Map<Long, Object> map = new TreeMap<>();

    @Override
    public T get(final long i) {
        return (T) this.map.get(i);
    }

    @Override
    public Storage<T> set(final long i, final T x) {
        this.map.put(i, x);
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.map.clear();
        return this;
    }
}
