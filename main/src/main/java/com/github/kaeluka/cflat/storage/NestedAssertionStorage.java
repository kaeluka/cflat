package com.github.kaeluka.cflat.storage;

import java.util.Objects;

public class NestedAssertionStorage<T> implements NestedStorage<T> {

    public interface NestedAssertion<T> {
        public void check(int rowstart,
                          int rowend,
                          int colstart,
                          int colend,
                          NestedStorage<T> st);
    }

    private final NestedStorage<T> inner;
    private final NestedAssertion<T> asrt;

    /* If and only if the assertion storage is enabled. */
    public final static boolean enabled = AssertionStorage.enabled;

    public static <T> NestedStorage<T> withAssertion(
            final NestedStorage<T> inner,
            final NestedAssertion<T> asrt) {
        if (enabled) {
            return new NestedAssertionStorage<T>(inner, asrt);
        } else {
            return inner;
        }
    }

    private NestedAssertionStorage(
            final NestedStorage<T> inner,
            final NestedAssertion<T> f) {
        Objects.requireNonNull(inner);
        Objects.requireNonNull(f);
        this.inner = inner;
        this.asrt = f;
        // the initial check:
        f.check(0, inner.maxIdx(), 0, inner.maxColIdx(), inner);
    }

    private NestedStorage<T> handle(int rowstart,
                                    int rowend,
                                    int colstart,
                                    int colend,
                                    NestedStorage<T> ret) {
        this.asrt.check(rowstart, rowend, colstart, colend, ret);
        return ret;
    }

    @Override
    public int maxColIdxOverapproximation() {
        return inner.maxColIdxOverapproximation();
    }

    @Override
    public Storage<T> getCol(final int col) {
        return AssertionStorage.withAssertion(
                inner.getCol(col),
                (rowstart, rowend, _upd) ->
                        asrt.check(rowstart, rowend, col, col+1, this));
    }

    @Override
    public Storage<T> get(final int row) {
        return AssertionStorage.withAssertion(
                inner.get(row),
                (colstart, colend, _upd) ->
                        asrt.check(row, row+1, colstart, colend, this));
    }

    @Override
    public boolean has(final int i) {
        return this.get(i) != null;
    }

    @Override
    public Storage<Storage<T>> set(final int row, final Storage<T> x) {
        return handle(
                row, row+1,
                0, x.maxIdx(),
                (NestedStorage<T>) inner.set(row, x));
    }

    @Override
    public Storage<Storage<T>> clearAll() {
        final NestedStorage<T> newInner = (NestedStorage<T>) this.inner.clearAll();
        if (newInner == inner) {
            return this;
        } else {
            return new NestedAssertionStorage<>(inner, asrt);
        }
    }

    @Override
    public int maxIdxOverapproximation() {
        return inner.maxIdxOverapproximation();
    }

    @SuppressWarnings("unchecked") @Override
    public Storage<Storage<T>> emptyCopy() {
        return inner.emptyCopy();
    }

    @Override
    public long bytesUsed() {
        return 0;
    }
}
