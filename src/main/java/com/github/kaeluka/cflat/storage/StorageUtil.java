package com.github.kaeluka.cflat.storage;

public class StorageUtil {
    public static int LOOP_SIZE_INFINITE = 0;
    public static Iteration childIndices(final long root, final Object[] shape) {
        assert(shape.length >= 1);
        if (shape.length == 1) {
            if ((int)shape[0] == LOOP_SIZE_INFINITE) {
                return null;
            } else {
                return null; //IntStream.range(0, shape[0]).spliterator();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static <T> Storage<T> insertAt(final Storage<T> source, final long sourcePos, final Storage<T> to, final long targetPos) {
        return null;
    }
}

