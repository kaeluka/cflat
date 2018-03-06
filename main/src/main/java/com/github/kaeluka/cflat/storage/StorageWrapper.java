package com.github.kaeluka.cflat.storage;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public abstract class StorageWrapper<T> implements Storage<T> {
    protected Storage<T> innerStorage;

    public StorageWrapper(final Storage<T> inner) {
        assert inner != null;
        this.innerStorage = inner;
    }

    protected abstract StorageWrapper<T> withNewIdentity(Storage<T> possiblyNewInnerStorage);

    @SuppressWarnings("unchecked")
    public T get(final int i) {
        return (T) innerStorage.get(i);
    }

    @SuppressWarnings("unchecked")
    public T getOrElse(final int i, final Supplier<T> s) {
        return innerStorage.getOrElse(i, s);
    }

    public boolean has(final int i) {
        return innerStorage.has(i);
    }

//    public boolean hasInRange(final int start, final int end) {
//        return innerStorage.hasInRange(start, end);
//    }

    public Storage<T> set(final int i, final T x) {
        return withNewIdentity(innerStorage.set(i, x));
    }

    public Storage<T> clearAll() {
        return withNewIdentity(innerStorage.clearAll());
    }

    public int sizeOverApproximation() {
        return innerStorage.sizeOverApproximation();
    }

//    public int sizePrecise() {
//        return innerStorage.sizePrecise();
//    }

//    public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest) {
//        return withNewIdentity(innerStorage.moveSubtree(source, shape, dest));
//    }

//    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest) {
//        return withNewIdentity(innerStorage.setSubtree(source, shape, dest));
//    }

//    public Storage<T> setRange(final int pos, final T x, final int length) {
//        return withNewIdentity(innerStorage.setRange(pos, x, length));
//    }

//    public Storage<T> moveRange(final int source, final int dest, final int length) {
//        return withNewIdentity(innerStorage.moveRange(source, dest, length));
//    }

//    public Storage<T> copyRange(final int source, final int dest, final int length) {
//        return withNewIdentity(innerStorage.copyRange(source, dest, length));
//    }

//    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove) {
//        return withNewIdentity(innerStorage.setSubtree(source, shape, dest, depth, doMove));
//    }

//    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
//        innerStorage.foreachSuccessor(start, shape, f);
//    }

//    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
//        innerStorage.foreachParent(start, shape, f);
//    }

//    public void foreach(final IntConsumer f) {
//        innerStorage.foreach(f);
//    }

//    public void foreachNonNull(final IntConsumer f) {
//        innerStorage.foreachNonNull(f);
//    }

//    public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f) {
//        innerStorage.joinInner(other, f);
//    }

//    public Iterator<Integer> nonNullIndices() {
//        return innerStorage.nonNullIndices();
//    }

    public Storage<T> addAll(final Storage<T> source) {
        return withNewIdentity(innerStorage.addAll(source));
    }

//    public int find(final T x, final int max) {
//        return innerStorage.find(x, max);
//    }

    public Storage<T> copy() {
        return withNewIdentity(innerStorage.copy());
    }

    public Storage<T> emptyCopy() {
        return withNewIdentity(innerStorage.emptyCopy());
    }

    public long bytesUsed() {
        return innerStorage.bytesUsed();
    }

    public Class<? extends Storage> getInnerClass() {
        return innerStorage.getClass();
    }
}
