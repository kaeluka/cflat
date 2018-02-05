package com.github.kaeluka.cflat.storage;


import com.github.kaeluka.cflat.storage.Storage;
import com.github.kaeluka.cflat.storage.size.ObjectSizes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class ChunkedStorage<T> extends Storage<T> {
    private final int CHUNK_SIZE;
    private Object[][] chunks;
    private int maxIdx = 0;

    public ChunkedStorage() {
        this(8);
    }

    public ChunkedStorage(final int chunkSize) {
        this(chunkSize, new Object[512][]);
    }

    private ChunkedStorage(final int chunkSize,
                           final Object[][] chunks) {
        this.CHUNK_SIZE = chunkSize;
        this.chunks = chunks;
    }

    private int chunk_no(final int i) {
        return (int)(i / CHUNK_SIZE);
    }

    private int chunk_idx(final int i) {
        // since CHUNK_SIZE is always a power of two, we can speed the
        // modulo operation up!
        return (i & CHUNK_SIZE - 1);
    }

    private Object[] getEnsureChunk(final int i) {
        final int chunk_no = chunk_no(i);

        if (chunks.length <= chunk_no) {
            chunks = Arrays.copyOf(chunks, chunk_no + (chunk_no >> 1));
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
        assert(i >= 0);
        maxIdx = i > maxIdx ? i : maxIdx;
//        System.out.println("maxIdx of C.S. now = " + maxIdx);
        this.getEnsureChunk(i)[chunk_idx(i)] = x;
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.chunks = new Object[chunks.length][];
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> copy() {
        return new ChunkedStorage<>(this.CHUNK_SIZE, Arrays.copyOf(chunks, chunks.length));
    }

    @Override
    public Storage<T> emptyCopy() {
        return new ChunkedStorage<>(this.CHUNK_SIZE, new Object[chunks.length][]);
    }

    @Override
    public long bytesUsed() {
        long size = ObjectSizes.ARRAY_SIZE(this.chunks);

        for (final Object[] chunk : this.chunks) {
            if (chunk != null) {
                size += ObjectSizes.ARRAY_SIZE(chunk);
            }
        }
        return size;
    }
}
