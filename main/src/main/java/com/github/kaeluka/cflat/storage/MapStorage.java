//package com.github.kaeluka.cflat.storage;
//
//import com.github.kaeluka.cflat.util.Mutable;
//
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//
//public final class MapStorage<T> implements Storage<T> {
//    private final Map<Integer, T> map;
//    private int maxIdxOverapproximation = -1;
//
//    @Override
//    public T get(final int i) {
//        return this.map.get(i);
//    }
//
//    public MapStorage() {
//        this(new HashMap<>());
//    }
//
//    private MapStorage(final Map<Integer, T> map) {
//        this.map = map;
//    }
//
//    @Override
//    public Storage<T> set(final int i, final T x) {
//        if (x != null) {
//            maxIdxOverapproximation = i > maxIdxOverapproximation ? i : maxIdxOverapproximation;
//            this.map.put(i, x);
//        } else {
//            this.map.remove(i);
//        }
//        return this;
//    }
//
//    @Override
//    public int findFirst(final T x, final int max) {
////         FIXME
//        final Set<Map.Entry<Integer, T>> entries = this.map.entrySet();
//        for (Map.Entry<Integer, T> e : entries) {
//            if (e.getValue().equals(x)) {
//                return e.getKey();
//            }
//        }
//        return -1;
//    }
//
//    @Override
//    public Storage<T> computeIfAbsent(final int i, final Function<Integer, T> f, final Mutable<T> result) {
//        result.x = this.map.computeIfAbsent(i, f);
//        if (i > maxIdxOverapproximation) { maxIdxOverapproximation = i; }
//        return this;
//    }
//
//    @Override
//    public Storage<T> clearAll() {
//        this.map.clear();
//        return this;
//    }
//
//    @Override
//    public Storage<T> emptyCopy() {
//        return new MapStorage<>(new HashMap<>());
//    }
//
//    @Override
//    public Iterator<Integer> nonNullIndices() {
//        return this.map.keySet().iterator();
//    }
//
//    @Override
//    public int maxIdxOverapproximation() {
//        return maxIdxOverapproximation+1;
//    }
//
//    @Override
//    public long bytesUsed() {
//        System.err.println("MapStorage: returning bogus value for bytesUsed");
//        return 1000; //FIXME //ObjectSizes.HASHMAP_SIZE( map);
//    }
//}
