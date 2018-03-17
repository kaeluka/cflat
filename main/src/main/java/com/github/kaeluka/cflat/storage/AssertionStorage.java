package com.github.kaeluka.cflat.storage;

import java.util.Objects;

public class AssertionStorage<T> implements Storage<T> {

    public interface Assertion<T> {
        public void check(int start, int end, Storage<T> st);
    }

    private final Storage<T> inner;
    private final Assertion<T> asrt;

    public final static boolean enabled = AssertionStorage.class.desiredAssertionStatus();

    public static <T> Storage<T> withAssertion(final Storage<T> inner, final Assertion<T> asrt) {
        if (enabled) {
            return new AssertionStorage<T>(inner, asrt);
        } else {
            return inner;
        }
    }

    private AssertionStorage(
            final Storage<T> inner,
            final Assertion<T> f) {
        Objects.requireNonNull(inner);
        Objects.requireNonNull(f);
        this.inner = inner;
        this.asrt = f;
        // the initial check:
        f.check(0, inner.maxIdx(), inner);
    }

    private Storage<T> handle(int start, int end, Storage<T> ret) {
        this.asrt.check(start, end, ret);
        if (ret == this.inner) {
            return this;
        } else {
            return AssertionStorage.withAssertion(ret, asrt);
        }
    }

    @Override
    public T get(final int i) { return inner.get(i); }

    @Override
    public Storage<T> set(final int i, final T x) { return handle(i, i+1, inner.set(i, x)); }

    @Override
    public Storage<T> clearAll() { return handle(0, inner.maxIdx(), inner.clearAll()); }

    @Override
    public int maxIdxOverapproximation() { return inner.maxIdxOverapproximation(); }

    @Override
    public Storage<T> emptyCopy() { return inner.emptyCopy(); }

    @Override
    public long bytesUsed() { return inner.bytesUsed(); }
}
