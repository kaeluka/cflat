package com.github.kaeluka.cflat.storage;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class StorageUtil {
    public static int INFINITE = 0;
    public static IndexSpliterator childIndices(final long root, final Object[] shape) {
        assert(shape.length >= 1);
        if (shape.length == 1) {
            if ((int)shape[0] == INFINITE) {
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

