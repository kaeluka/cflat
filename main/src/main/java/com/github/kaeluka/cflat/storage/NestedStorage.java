package com.github.kaeluka.cflat.storage;

import java.util.Iterator;
import java.util.function.IntConsumer;

/**
 *
 * Nested storages differ from normal storages in some ways:
 *
 *   - Nested storages maintain their own rows (of type {@code Storage<T>},
 *     when {@code get(int)} is called on a cell that has no row, one must be
 *     initialised.
 *
 *   - nested storages handle identity changes of rows, such that code like
 *     {@code nestedStorage.get(row).set(col, x)} will insert {@code x} into the
 *     nested storage, even when the set operation is identity-changing. In
 *     practise, this means that the nested storage will return a wrapper object
 *     for the row that handles updating of the nested storage on an identity
 *     change. See {@link Storage2D} for an example implementation. When
 *     {@code get} is called on a nested storage's row for the first time, it
 *     must always create a row and return that -- never {@code null}.
 *
 *   - Since {@code get} never returns {@code null}, the meaning of {@code has}
 *     changes: nested storages are free to return false for {@code has}, if
 *     the row has never been accessed or is empty. Sparse storages must take
 *     care to not allocate extra memory as result of a {@code has} call.
 *
 * @param <T> the contained type
 */
@SuppressWarnings("unused")
public interface NestedStorage<T> extends Storage<Storage<T>> {

    default boolean has(final int i) {
        throw new UnsupportedOperationException(
                "method has is not implemented.");
    }

    default boolean hasCol(final int i) {
        final Iterator<Integer> integerIterator = nonNullIndices();
        while (integerIterator.hasNext()) {
            if (get(integerIterator.next()).has(i)) {
                return true;
            }
        }
        return false;
    }

    public int maxColIdxOverapproximation();

    public default int maxColIdx() {
        int estimate = maxColIdxOverapproximation();
        assert(estimate >= 0);
        while (estimate > 0 && !hasCol(estimate-1)) { estimate --; }
        return estimate;
    }

    public default void foreachColNonNull(IntConsumer f) {
        final int max = this.maxColIdx();
        for (int i=0; i<=max; ++i) {
            if (hasCol(i)) {
                f.accept(i);
            }
        }
    }

    default Storage<T> getCol(int col) {
        class DefaultColStorage implements Storage<T> {

            @Override
            public T get(final int row) {
                return NestedStorage.this.get(row).get(col);
            }

            @Override
            public Storage<T> set(final int row, final T x) {
                NestedStorage.this
                        .get(row)
                        .set(col, x);
                return this;
            }

            @Override
            public Storage<T> clearAll() {
                this.foreachNonNull(i -> this.set(i, null));
                return this;
            }

            @Override
            public int maxIdxOverapproximation() {
                // as large is the number of rows:
                return NestedStorage.this.maxIdxOverapproximation();
            }

            @Override
            public Storage<T> emptyCopy() {
                return NestedStorage.this.get(col).emptyCopy();
            }

            @Override
            public long bytesUsed() {
                return -1;
            }
        }

        return new DefaultColStorage();
    }

    default NestedStorage<T> copyNested() {
        final NestedStorage<T> cp = (NestedStorage<T>) emptyCopy();
//        this.foreachNonNull(row -> cp.get(row).addAll(this.get(row)) );
        final int MAX_ROW = this.maxIdx();
        for (int r = 0; r < MAX_ROW; r++) {
            final Storage<T> row = this.get(r);
            final int MAX_COL = row.maxIdx();
            for (int c = 0; c < MAX_COL; c++) {
                final T gotten = get(r).get(c);
                if (gotten != null) {
                    cp.get(r).set(c, gotten);
                }
            }
        }
        return cp;
    }
}
