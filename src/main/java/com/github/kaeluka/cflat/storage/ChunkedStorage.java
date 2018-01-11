package com.github.kaeluka.cflat.storage;


import com.github.kaeluka.cflat.storage.Storage;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class ChunkedStorage<T> implements Storage<T> {
    private final static int CHUNK_SIZE = 1000;
    private final ArrayList<Object[]> chunks;

    private static int chunk_no(final long i) {
        return (int)(i / CHUNK_SIZE);
    }

    private static int chunk_idx(final long i) {
        return (int)(i % CHUNK_SIZE);
    }

    private Object[] getEnsureChunk(final long i) {
        final int chunk_no = chunk_no(i);

        chunks.ensureCapacity(chunk_no+1);
        while (chunks.size() <= chunk_no) {
            chunks.add(null);
        }

        if (chunks.get(chunk_no) == null) {
            final Object[] new_chunk = new Object[CHUNK_SIZE];
            chunks.set(chunk_no, new_chunk);
            return new_chunk;
        } else {
            return chunks.get(chunk_no);
        }
    }
    private Object[] getChunk(final long i) {
        final int chunk_no = chunk_no(i);

        if (chunks.size() <= chunk_no) {
            return null;
        } else {
            return chunks.get(chunk_no);
        }
    }

    public ChunkedStorage() {
        this.chunks = new ArrayList<>();
    }

    @Override
    public T get(final long i) {
        final Object[] chunk = this.getChunk(i);
        if (chunk != null) {
            return (T) chunk[chunk_idx(i)];
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> set(final long i, final T x) {
        this.getEnsureChunk(i)[chunk_idx(i)] = x;
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.chunks.clear();
        return this;
    }
}
