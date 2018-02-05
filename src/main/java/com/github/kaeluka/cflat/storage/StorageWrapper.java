package com.github.kaeluka.cflat.storage;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public abstract class StorageWrapper<T> extends Storage<T> {
    protected final Storage<T> innerStorage;

    public StorageWrapper(final Storage<T> inner) {
        assert inner != null;
        this.innerStorage = inner;
    }

    protected abstract StorageWrapper<T> withNewIdentity(Storage<T> possiblyNewInnerStorage);

    @SuppressWarnings("unchecked")
    @Override
    public T get(final int i) {
        return (T) innerStorage.get(i);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getOrElse(final int i, final Supplier<T> s) {
        return innerStorage.getOrElse(i, s);
    }

    @Override
    public boolean has(final int i) {
        return innerStorage.has(i);
    }

    @Override
    public boolean hasInRange(final int start, final int end) {
        return innerStorage.hasInRange(start, end);
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        return withNewIdentity(innerStorage.set(i, x));
    }

    @Override
    public Storage<T> clear() {
        return withNewIdentity(innerStorage.clear());
    }

    @Override
    public int sizeOverApproximation() {
        return innerStorage.sizeOverApproximation();
    }

    @Override
    public int sizePrecise() {
        return innerStorage.sizePrecise();
    }

    @Override
    public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest) {
        return withNewIdentity(innerStorage.moveSubtree(source, shape, dest));
    }

    @Override
    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest) {
        return withNewIdentity(innerStorage.setSubtree(source, shape, dest));
    }

    @Override
    public Storage<T> setRange(final int pos, final T x, final int length) {
        return withNewIdentity(innerStorage.setRange(pos, x, length));
    }

    @Override
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        return withNewIdentity(innerStorage.moveRange(source, dest, length));
    }

    @Override
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        return withNewIdentity(innerStorage.copyRange(source, dest, length));
    }

    @Override
    Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove) {
        return withNewIdentity(innerStorage.setSubtree(source, shape, dest, depth, doMove));
    }

    @Override
    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        innerStorage.foreachSuccessor(start, shape, f);
    }

    @Override
    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
        innerStorage.foreachParent(start, shape, f);
    }

    @Override
    public void foreach(final IntConsumer f) {
        innerStorage.foreach(f);
    }

    @Override
    public void foreachNonNull(final IntConsumer f) {
        innerStorage.foreachNonNull(f);
    }

    @Override
    public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f) {
        innerStorage.joinInner(other, f);
    }

    @Override
    public Iterator<Integer> nonNullIndices() {
        return innerStorage.nonNullIndices();
    }

    @Override
    public Storage<T> addAll(final Storage<T> source) {
        return withNewIdentity(innerStorage.addAll(source));
    }

    @Override
    public int find(final T x, final int max) {
        return innerStorage.find(x, max);
    }

    @Override
    public Storage<T> copy() {
        return withNewIdentity(innerStorage.copy());
    }

    @Override
    public Storage<T> emptyCopy() {
        return withNewIdentity(innerStorage.emptyCopy());
    }

    @Override
    public long bytesUsed() {
        return innerStorage.bytesUsed();
    }

    public Class<? extends Storage> getInnerClass() {
        return innerStorage.getClass();
    }
}
