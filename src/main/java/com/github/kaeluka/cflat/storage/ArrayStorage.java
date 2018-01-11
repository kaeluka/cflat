package com.github.kaeluka.cflat.storage;


import java.util.Arrays;
import java.util.stream.IntStream;

public class ArrayStorage<T> implements Storage<T> {
    private final static int DEFAULT_SIZE = 10;
    private Object[] data = new Object[DEFAULT_SIZE];

    @Override
    public T get(final long l) {
        final int i = Math.toIntExact(l);
        ensureSize(i);
        return (T) this.data[i];
    }

    private void ensureSize(final int i) {
        if (this.data.length <= i) {
            final Object[] old_data = this.data;
            int newLength = old_data.length;
            while (newLength <= i) {
                newLength *= 2;
            }
            this.data = Arrays.copyOf(this.data, newLength);
        }
    }

    @Override
    public Storage<T> set(final long l, final T x) {
        final int i = Math.toIntExact(l);
        this.ensureSize(i);
        this.data[i] = x;
        return this;
    }

    @Override
    public Storage<T> clear() {
        for(int i=0; i<this.data.length; ++i) {
            this.data[i] = null;
        }
        return this;
    }
}
