package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.ast.Util;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.function.IntConsumer;

public interface Storage<T> {
    public T get(int i);

    public default boolean has(int i) {
        return this.get(i) != null;
    }

    public Storage<T> set(int i, T x);

    public Storage<T> clear();

    public default int estimateSize() {
        return Integer.MAX_VALUE;
    }

    public default Storage<T> moveSubtree(final int source,
                                       final Object[] shape,
                                       final int dest,
                                       final int depth) {
        if (Util.isRep(shape) || Util.isStar(shape) &&
                ((int) shape[1] == 1) &&
                ((int) shape[2] == 1)) {
            //FIXME: only sequential this far!
            assert(dest < source);
            int diff = source - dest;
            this.foreachSuccessor(source,
                    shape,
                    succ -> this.set(succ - diff, this.get(succ)));
        } else {
            throw new UnsupportedOperationException("not yet implemented for " +
                    "complex shapes");
        }
        return this;
    }

    public default void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        if (Util.isRep(shape)) {
            //FIXME: only sequential this far!
            assert((int) shape[1] == 1);
            assert((int) shape[2] == 1);
            int cur = start;
            final int len = (int) shape[0];
            while (cur < len && has(cur)) {
                f.accept(cur++);
            }
            return;
        }
        if (Util.isStar(shape)) {
            //FIXME: only sequential this far!
            assert ((int) shape[1] == 1);
            assert ((int) shape[2] == 1);
            int cur = start;
            while (has(cur)) {
                f.accept(cur++);
            }
            return;
        }
        throw new UnsupportedOperationException("impl not done!");
    }

    public default void foreachParent(int start, Object[] shape, IntConsumer f) {
        if (Util.isRep(shape)) {
            //FIXME: only sequential this far!
            assert ((int) shape[1] == 1);
            assert ((int) shape[2] == 1);
            int cur = start;
            while (has(cur)) {
                f.accept(cur--);
            }
            return;
        }
        throw new UnsupportedOperationException("impl not done!");
    }

    public default int find(T x, int max) {
        int _max = max < 0 ? this.estimateSize() : max;
        for(int i=0; i<_max; ++i) {
            if (x == get(i)) {
                return i;
            }
        }
        return -1;
    }

    public default Storage<T> copy() {
        throw new NotImplementedException();
    }
}

