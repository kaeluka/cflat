package com.github.kaeluka.cflat.util;

import com.github.kaeluka.cflat.storage.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Storages {
    public static List<Supplier<Storage>> intStorages() {
        final ArrayList<Supplier<Storage>> ret = new ArrayList<>();
        ret.add(new NamedSupplier<>(IntArrayStorage.class));
        ret.add(new NamedSupplier<>(IntTrieStorage.class));
        return ret;
    }

    public static List<Supplier<Storage>> genericStorages() {
        final ArrayList<Supplier<Storage>> ret = new ArrayList<>();
        ret.add(new NamedSupplier<>(ArrayStorage.class));
        ret.add(new NamedSupplier<>(SortedArrayStorage.class));
//        ret.add(new NamedSupplier<>(LinkedStorage.class));
        ret.add(new NamedSupplier<>(ChunkedStorage.class));
        ret.add(new NamedSupplier<>(HashMapStorage.class));
//        ret.add(new NamedSupplier<>(KolobokeMapStorage.class));
        ret.add(new NamedSupplier<>(ImmutableStorage.class));
        ret.add(new NamedSupplier<>(IndexedStorage.class));
        ret.add(new NamedSupplier<>(ReverseStorage.class));
//        ret.add(new NamedSupplier<>(ShallowTrieStorage.class));
        ret.add(new NamedSupplier<>(TrieStorage.class));
        ret.add(new NamedSupplier<>(PMAStorage.class));
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <V> List<Supplier<NestedStorage<V>>> nestedStorages() {
        final ArrayList<Supplier<NestedStorage<V>>> ret = new ArrayList<>();
        for (final Supplier<Storage> sup : genericStorages()) {
            ret.add(new NamedSupplier<>(() -> new Storage2D<>(sup.get(), sup.get()),"Storage2D("+sup.toString()+")"));
        }
        ret.add(new NamedSupplier<>(() -> SparseStorage.getFor(SparseStorage.USAGE.SIZE), "SparseStorage(SIZE)"));
        ret.add(new NamedSupplier<>(() -> SparseStorage.getFor(SparseStorage.USAGE.INSERT_PERFORMANCE), "SparseStorage(PERFORMANCE)"));
        return ret;
    }


}
