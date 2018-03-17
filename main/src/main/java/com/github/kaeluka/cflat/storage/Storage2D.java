package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.IndexCheck;
import com.github.kaeluka.cflat.util.Mutable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class Storage2D<T> implements NestedStorage<T> {
    private final Storage<T> colsProto;
    private int maxColIdx = -1;

    @Override
    public String toString() {
        return super.toString()+"("+colsProto.getClass().getSimpleName()+")";
    }

    public Storage<Storage<T>> data;
    private int maxIdx = -1;

    @SuppressWarnings("unchecked")
    public Storage2D(final Storage<Storage<T>> rowsProto, final Storage<T> colsProto) {
        this.colsProto = colsProto;
        this.data = rowsProto.emptyCopy();
    }

    private void doReplaceRow(int row, Storage<T> newRow) {
        IndexCheck.checkIndexIsNonnegative(row);
        data = data.set(row, newRow);
    }

    private class IdentityChangeHandlingCol implements Storage<T> {

        private int row;
        private Storage<T> storage;

        IdentityChangeHandlingCol(final int row, final Storage<T> storage) {
            assert row >= 0;
            IndexCheck.checkIndexIsNonnegative(row);
            if (storage == null) {
                throw new NullPointerException();
            }
            this.row = row;
            this.storage = storage;
        }

        private Storage<T> handleChangeRow(final Storage<T> possiblyNewRow) {
            if (possiblyNewRow != this.storage.get(row)) {
                Storage2D.this.doReplaceRow(row, possiblyNewRow);
            }
            return new IdentityChangeHandlingCol(row, possiblyNewRow);
        }

        @Override
        public T get(final int i) { return storage.get(i); }

        @Override
        public T get2(final int i, final Mutable<T> v2) {
            return storage.get2(i, v2);
        }

        @Override
        public T getOrElse(final int i, final Supplier<T> s) {
            return storage.getOrElse(i, s);
        }

        @Override
        public boolean has(final int i) {
            return storage.has(i);
        }

        @Override
        public boolean hasInRange(final int start, final int end) {
            return storage.hasInRange(start, end);
        }

        @Override
        public Storage<T> set(final int i, final T x) {
            if (i > maxColIdx) { maxColIdx = i; }
            return handleChangeRow(storage.set(i, x));
        }

        @Override
        public Storage<T> computeIfAbsent(final int i, final IntFunction<T> f, final Mutable<T> result) {
            return handleChangeRow(storage.computeIfAbsent(i, f, result));
        }

        @Override
        public Storage<T> clearAll() {
            return handleChangeRow(storage.clearAll());
        }

        @Override
        public int maxIdxOverapproximation() {
            return storage.maxIdxOverapproximation();
        }

        @Override
        public int maxIdx() {
            return storage.maxIdx();
        }

        @Override
        public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest) {
            return handleChangeRow(storage.moveSubtree(source, shape, dest));
        }

        @Override
        public Storage<T> setSubtree(final int source, final Object[] shape, final int dest) {
            return handleChangeRow(storage.setSubtree(source, shape, dest));
        }

        @Override
        public Storage<T> setRange(final int pos, final T x, final int length) {
            return handleChangeRow(storage.setRange(pos, x, length));
        }

        @Override
        public Storage<T> moveRange(final int source, final int dest, final int length) {
            return handleChangeRow(storage.moveRange(source, dest, length));
        }

        @Override
        public Storage<T> copyRange(final int source, final int dest, final int length) {
            return handleChangeRow(storage.copyRange(source, dest, length));
        }

        @Override
        public Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove) {
            return handleChangeRow(storage.setSubtree(source, shape, dest, depth, doMove));
        }

        @Override
        public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
            storage.foreachSuccessor(start, shape, f);
        }

        @Override
        public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
            storage.foreachParent(start, shape, f);
        }

        @Override
        public void foreach(final IntConsumer f) {
            storage.foreach(f);
        }

        @Override
        public void foreachNonNull(final IntConsumer f) {
            storage.foreachNonNull(f);
        }

        @Override
        public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f) {
            storage.joinInner(other, f);
        }

        @Override
        public Iterator<Integer> nonNullIndices() {
            return storage.nonNullIndices();
        }

        @Override
        public Storage<T> addAll(final Storage<T> source) {
            return handleChangeRow(storage.addAll(source));
        }

        @Override
        public int findFirst(final T x, final int max) {
            return storage.findFirst(x, max);
        }

        @Override
        public Storage<T> copy() {
            return storage.copy();
        }

        @Override
        public Storage<T> emptyCopy() {
            return storage.emptyCopy();
        }

        @Override
        public long bytesUsed() {
            return storage.bytesUsed();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<T> get(final int i) {
        IndexCheck.checkIndexIsNonnegative(i);
        final Mutable<Storage<T>> mgotten = new Mutable<>(null);
        this.data = this.data.computeIfAbsent(i, idx -> colsProto.emptyCopy(), mgotten);
        if (i > maxIdx) { maxIdx = i; }
        return new IdentityChangeHandlingCol(i, mgotten.x);
    }

    @Override
    public boolean has(final int i) { return this.data.has(i); }

    @Override
    public int maxColIdxOverapproximation() {
        return maxColIdx+1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<Storage<T>> set(final int l, final Storage<T> x) {
        maxIdx = l > maxIdx ? l : maxIdx;
        data.set(l, x);
        return this;
    }

    @Override
    public Storage<Storage<T>> clearAll() {
        data.clearAll();
        this.maxIdx = -1;
        return this;
    }

    @Override
    public int maxIdx() {
        return maxIdx+1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<Storage<T>> emptyCopy() {
        return new Storage2D<>(data.emptyCopy(), colsProto.emptyCopy());
    }

    @Override
    public Storage<Storage<T>> copy() {
        final Storage<Storage<T>> cp = this.emptyCopy();
        this.foreachNonNull(i -> cp.set(i, this.get(i).copy()));
        return cp;
    }

    @Override
    public int maxIdxOverapproximation() {
        return maxIdx+1;
    }

    @Override
    public long bytesUsed() {
        final AtomicLong ret = new AtomicLong(this.data.bytesUsed());
        data.foreachSuccessor(0, new Object[]{0, 1, 1}, i -> ret.addAndGet(get(i).bytesUsed()));
        return ret.get();
    }
}
