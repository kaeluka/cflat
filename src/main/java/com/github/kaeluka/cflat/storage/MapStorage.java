package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.util.Mutable;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MapStorage<T> extends Storage<T> {
    private final Map<Integer, T> map;
    private int maxIdx = -1;

    @Override
    public T get(final int i) {
        return this.map.get(i);
    }

    public MapStorage() {
        this(new HashMap<>());
    }

    private MapStorage(final Map<Integer, T> map) {
        this.map = map;
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        if (x != null) {
            maxIdx = i > maxIdx ? i : maxIdx;
            this.map.put(i, x);
        } else {
            this.map.remove(i);
        }
        return this;
    }

    @Override
    public int find(final T x, final int max) {
//         FIXME
        final Set<Map.Entry<Integer, T>> entries = this.map.entrySet();
        for (Map.Entry<Integer, T> e : entries) {
            if (e.getValue().equals(x)) {
                return e.getKey();
            }
        }
        return -1;
    }

    @Override
    public Storage<T> computeIfAbsent(final int i, final Function<Integer, T> f, final Mutable<T> result) {
        result.x = this.map.computeIfAbsent(i, f);
        if (i > maxIdx) { maxIdx = i; }
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.map.clear();
        return this;
    }

    @Override
    public int sizePrecise() {
        return map.size();
    }

    @Override
    public Storage<T> emptyCopy() {
        return new MapStorage<>(new HashMap<>());
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public long bytesUsed() {
        System.err.println("MapStorage: returning bogus value for bytesUsed");
        return 1000; //FIXME //ObjectSizes.HASHMAP_SIZE( map);
    }
}
