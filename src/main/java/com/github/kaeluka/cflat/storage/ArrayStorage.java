package com.github.kaeluka.cflat.storage;


import java.util.Arrays;
import java.util.stream.IntStream;
import com.github.kaeluka.cflat.ast.Util;

public class ArrayStorage<T> implements Storage<T> {
    private final static int DEFAULT_SIZE = 10;
    public Object[] data = new Object[DEFAULT_SIZE];

    @Override
    public T get(final int l) {
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
    public Storage<T> set(final int l, final T x) {
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

    @Override
    public Storage moveSubtree(final int source,
                               final Object[] shape,
                               final int dest,
                               final int depth) {
        if (Util.isRep(shape)) {
            //FIXME: only sequential this far!
            assert((int) shape[1] == 1);
            assert((int) shape[2] == 1);
            System.arraycopy(this.data, source, this.data, dest, depth);
        } else if (Util.isStar(shape)) {
            //FIXME: only sequential this far!
            assert((int) shape[1] == 1);
            assert((int) shape[2] == 1);
            System.arraycopy(this.data, source, this.data, dest, depth);
        } else {
            throw new UnsupportedOperationException("array storages can only " +
                    "move linear shapes (rest not implemented!)");
        }
        return this;
    }
}
