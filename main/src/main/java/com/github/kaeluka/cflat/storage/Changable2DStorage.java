//package com.github.kaeluka.cflat.storage;
//
//import java.util.function.IntConsumer;
//import java.util.function.Supplier;
//
//public final class Changable2DStorage<T> implements NestedStorage<T> {
//    NestedStorage<T> inner;
//    SparseStorage.USAGE current;
//
//    Changable2DStorage(SparseStorage.USAGE startWith) {
//        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
//        this.inner = SparseStorage.getFor(startWith);
//        this.current = startWith;
//    }
//
//    Changable2DStorage(SparseStorage.USAGE startWith, int rowsSizeHint, int colsSizeHint, double sparsityHint) {
//        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
//        this.inner = SparseStorage.getFor(startWith, rowsSizeHint, colsSizeHint, sparsityHint);
//        this.current = startWith;
//    }
//
//    private Changable2DStorage(NestedStorage<T> inner, SparseStorage.USAGE startWith) {
//        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
//        this.inner = inner;
//        this.current = startWith;
//    }
//
//    @Override
//    public Storage<T> get(final int i) {
//        return inner.get(i);
//    }
//
//    @Override
//    public Storage<T> getOrElse(final int i, final Supplier<Storage<T>> s) {
//        return inner.getOrElse(i, s);
//    }
//
//    @Override
//    public boolean has(final int i) {
//        return inner.has(i);
//    }
//
//    @Override
//    public int maxColIdxOverapproximation() {
//        return this.inner.maxColIdxOverApproximation();
//    }
//
//    @Override
//    public NestedStorage<T> set(final int i, final Storage<T> x) {
//        assert(false);
//        inner = inner.set(i, x);
//        return this;
//    }
//
//    @Override
//    public NestedStorage<T> clearAll() {
//        inner = (NestedStorage<T>) inner.clearAll();
//        return this;
//    }
//
//    @Override
//    public int maxIdxOverapproximation() {
//        return inner.maxIdxOverapproximation();
//    }
//
//    @Override
//    public int maxIdx() {
//        return inner.maxIdx();
//    }
//
//    @Override
//    public NestedStorage<T> setSubtree(final int source, final Object[] shape, final int dest) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
//        inner.foreachSuccessor(start, shape, f);
//    }
//
//    @Override
//    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
//        inner.foreachParent(start, shape, f);
//    }
//
//    @Override
//    public void foreachNonNull(final IntConsumer f) {
//        inner.foreachNonNull(f);
//    }
//
//    @Override
//    public Storage<Storage<T>> addAll(final Storage<Storage<T>> source) {
//        throw new UnsupportedOperationException("AddAll not supported for nested storages");
//    }
//
//    @Override
//    public int findFirst(final Storage<T> x, final int max) {
//        return inner.findFirst(x, max);
//    }
//
//    @Override
//    public Storage<Storage<T>> copy() {
//        return new Changable2DStorage<>(inner.copyNested(), this.current);
//    }
//
//    @Override
//    public Storage<Storage<T>> emptyCopy() {
//        return new Changable2DStorage<>(inner.emptyCopy(), this.current);
//    }
//
//    @Override
//    public long bytesUsed() {
//        return inner.bytesUsed();
//    }
//
//    public void change(SparseStorage.USAGE changeTo) {
//        inner = SparseStorage.reshape(inner, changeTo);
//    }
//}
