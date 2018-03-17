package com.github.kaeluka.cflat.storage;


import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.util.Mutable;

import java.util.Arrays;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

public class ChunkedStorage<T> implements Storage<T> {
    private final static int CHUNK_SIZE = 128;
    private final static int DEFAULT_CHUNKS_SIZE = 8;
    private Object[][] chunks = new Object[DEFAULT_CHUNKS_SIZE][];
    private int maxIdx = 0;

    private int idxOfChunk(final int i) {
        final int lz = Integer.numberOfLeadingZeros(CHUNK_SIZE);
        return i >> (31 - lz);
    }

    public ChunkedStorage() {
        this(new Object[DEFAULT_CHUNKS_SIZE][]);
    }

    private ChunkedStorage(Object[][] data) {
        this.chunks = data;
    }

    private static int idxWithinChunk(final int i) {
        // since CHUNK_SIZE is a power of two, we can speed the
        // modulo operation up!
        return (i & (CHUNK_SIZE - 1));
    }

    private Object[] getEnsureChunk(final int i) {
        final int chunk_no = idxOfChunk(i);

        if (chunks.length <= chunk_no) {
            final int lz = Integer.numberOfLeadingZeros(i);
            //next power of two
            int newLength = 1 << (32 - lz);
            chunks = Arrays.copyOf(chunks, newLength);
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
        final int chunk_no = idxOfChunk(i);

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
            return (T) chunk[idxWithinChunk(i)];
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get2(final int i, final Mutable<T> v2) {
        final Object[] chunk = this.getChunk(i);
        if (chunk != null) {
            final int idxWithin = idxWithinChunk(i);
            if (idxWithin < CHUNK_SIZE) {
                v2.x = (T) chunk[idxWithin+1];
            } else {
                v2.x = get(i+1);
            }
            return (T) chunk[idxWithin];
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        checkIndexIsNonnegative(i);
        if (x == null && !has(i)) { return this; }
        maxIdx = i > maxIdx ? i : maxIdx;
        this.getEnsureChunk(i)[idxWithinChunk(i)] = x;
        return this;
    }

    @Override
    public Storage<T> set2(final int i, final T x, final T y) {
        checkIndexIsNonnegative(i);
        checkIndexIsNonnegative(i+1);
        maxIdx = i+1 > maxIdx ? i+1 : maxIdx;
        final int idxWithin = idxWithinChunk(i);
        final Object[] chunk = this.getEnsureChunk(i);
        chunk[idxWithin] = x;
        if(idxWithin+1 < chunk.length) {
            chunk[idxWithin+1] = y;
        } else {
            //noinspection SuspiciousNameCombination
            set(i+1, y);
        }
        return this;
    }

    @Override
    public Storage<T> clearAll() {
        this.chunks = new Object[chunks.length][];
        return this;
    }

    @Override
    public int maxIdxOverapproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> copy() {
        return new ChunkedStorage<>(Arrays.copyOf(chunks, chunks.length));
    }

    @Override
    public int findFirst(final T x, final int max) {
        assert max == -1;
        for (int i = 0; i < chunks.length; i++) {
            final Object[] chunk = chunks[i];
            if (chunk != null) {
                for (int j = 0; j < chunk.length; j++) {
                    if (chunk[j] != null && chunk[j].equals(x)) {
                        return chunk.length *i+j;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public Storage<T> copyRange(final int source, final int dest, final int length) {

        final int sourceChunkIdx = idxOfChunk(source);
        final int destChunkIdx   = idxOfChunk(dest);

        if (sourceChunkIdx == idxOfChunk(source+length-1)
                && destChunkIdx == idxOfChunk(dest+length-1)) {
            if ((sourceChunkIdx >= chunks.length || chunks[sourceChunkIdx] == null)
                    && (destChunkIdx >= chunks.length || chunks[destChunkIdx] == null)) {
                return this;
            }
            final Object[] sourceChunk = this.getChunk(source);
            final Object[] destChunk   = this.getChunk(dest);
            if (sourceChunk != null && destChunk != null) {
                System.arraycopy(
                        sourceChunk, idxWithinChunk(source),
                        destChunk, idxWithinChunk(dest),
                        length);
            } else {
                if (sourceChunk == null && destChunk != null) {
                    Arrays.fill(destChunk,
                            idxWithinChunk(dest),
                            idxWithinChunk(dest+length),
                            null);
                    return this;
                } else if (sourceChunk != null && destChunk == null) {
                    final Object[] newDestChunk = getEnsureChunk(dest);
                    System.arraycopy(
                        sourceChunk, idxWithinChunk(source),
                        newDestChunk, idxWithinChunk(dest),
                        length);
                    return this;
                }
                throw new AssertionError();
            }
            return this;
        } else {
            return Storage.super.copyRange(source, dest, length);
        }
    }

    @Override
    public Storage<T> emptyCopy() {
        return new ChunkedStorage<>(new Object[chunks.length][]);
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
