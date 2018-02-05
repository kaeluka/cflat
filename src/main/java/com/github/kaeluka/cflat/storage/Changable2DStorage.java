package com.github.kaeluka.cflat.storage;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class Changable2DStorage<T> extends Storage<Storage<T>> {
    Storage<Storage<T>> inner;
    SparseStorage.USAGE current;

    Changable2DStorage(SparseStorage.USAGE startWith) {
        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
        this.inner = SparseStorage.getFor(startWith);
        this.current = startWith;
    }

    Changable2DStorage(SparseStorage.USAGE startWith, int rowsSizeHint, int colsSizeHint, double sparsityHint) {
        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
        this.inner = SparseStorage.getFor(startWith, rowsSizeHint, colsSizeHint, sparsityHint);
        this.current = startWith;
    }

    private Changable2DStorage(Storage<Storage<T>> inner, SparseStorage.USAGE startWith) {
        assert(startWith != SparseStorage.USAGE.CHANGEABLE);
        this.inner = inner;
        this.current = startWith;
    }

    @Override
    public Storage<T> get(final int i) {
        return inner.get(i);
    }

    @Override
    public Storage<T> getOrElse(final int i, final Supplier<Storage<T>> s) {
        return inner.getOrElse(i, s);
    }

    @Override
    public boolean has(final int i) {
        return inner.has(i);
    }

    @Override
    public Storage<Storage<T>> set(final int i, final Storage<T> x) {
        assert(false);
        inner = inner.set(i, x);
        return this;
    }

    @Override
    public Storage<Storage<T>> clear() {
        inner = inner.clear();
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return inner.sizeOverApproximation();
    }

    @Override
    public int sizePrecise() {
        return inner.sizePrecise();
    }

    @Override
    public Storage<Storage<T>> setSubtree(final int source, final Object[] shape, final int dest) {
        inner = inner.setSubtree(source, shape, dest);
        return this;
    }

    @Override
    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        inner.foreachSuccessor(start, shape, f);
    }

    @Override
    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
        inner.foreachParent(start, shape, f);
    }

    @Override
    public void foreachNonNull(final IntConsumer f) {
        inner.foreachNonNull(f);
    }

    @Override
    public Storage<Storage<T>> addAll(final Storage<Storage<T>> source) {
        inner = inner.addAll(source);
        return this;
    }

    @Override
    public int find(final Storage<T> x, final int max) {
        return inner.find(x, max);
    }

    @Override
    public Storage<Storage<T>> copy() {
        return new Changable2DStorage<>(inner.copy(), this.current);
    }

    @Override
    public Storage<Storage<T>> emptyCopy() {
        return new Changable2DStorage<>(inner.emptyCopy(), this.current);
    }

    @Override
    public long bytesUsed() {
        return inner.bytesUsed();
    }

    public void change(SparseStorage.USAGE changeTo) {
        inner = SparseStorage.reshape(inner, changeTo);
    }
}
