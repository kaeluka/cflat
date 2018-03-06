package com.github.kaeluka.cflat.util;

import java.util.function.Supplier;

/**
 * A {@link Supplier}that overrides toString to show something useful.
 * This can be used for parameterised JUnit tests in order for the tests to
 * provide human-readable output.
 * @param <T> the type the supplier will provide
 */
public class NamedSupplier<T> implements Supplier<T> {
    private final Supplier<T> inner;
    private final String name;

    public NamedSupplier(final Supplier<T> inner, final String name) {
        this.inner = inner;
        this.name = name;
    }

    public NamedSupplier(final Class<? extends T> klass) {
        this(() -> {
            try {
                return klass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AssertionError("can't instantiate class "+klass.getName()+" (does it have a default constructor?)", e);
            }
        }, klass.getSimpleName());
    }

    @Override public T get() { return inner.get(); }
    @Override public String toString() { return this.name; }
}
