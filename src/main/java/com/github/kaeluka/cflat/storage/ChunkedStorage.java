package com.github.kaeluka.cflat.storage;


import com.github.kaeluka.cflat.storage.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class ChunkedStorage<T> implements Storage<T> {
    private final static int CHUNK_SIZE = 1024;
    private final static int CHUNK_AMOUNT = 64;
    private Object[][] chunks = new Object[CHUNK_AMOUNT][];

    private static int chunk_no(final int i) {
        return (int)(i / CHUNK_SIZE);
    }

    private static int chunk_idx(final int i) {
        // since CHUNK_SIZE is always a power of two, we can speed the
        // modulo operation up!
        return ((int)i & CHUNK_SIZE - 1);
    }

    private Object[] getEnsureChunk(final int i) {
        final int chunk_no = chunk_no(i);

        if (chunks.length <= chunk_no) {
            chunks = Arrays.copyOf(chunks, chunk_no*2);
        }

        if (chunks[chunk_no] == null) {
            final Object[] new_chunk = new Object[CHUNK_SIZE];
            chunks[chunk_no] = new_chunk;
            return new_chunk;
        } else {
            return chunks[chunk_no];
        }
    }

    private Object[] getChunk(final int i) {
        final int chunk_no = chunk_no(i);

        if (chunks.length <= chunk_no) {
            return null;
        } else {
            return chunks[chunk_no];
        }
    }

    @Override
    public T get(final int i) {
        final Object[] chunk = this.getChunk(i);
        if (chunk != null) {
            return (T) chunk[chunk_idx(i)];
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        final Object[] ensuredChunk = this.getEnsureChunk(i);
        ensuredChunk[chunk_idx(i)] = x;
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.chunks = new Object[CHUNK_AMOUNT][];
        return this;
    }
}
