package com.github.kaeluka.cflat.storage;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface Iteration extends Spliterators.AbstractSpliterator.OfLong {
    public static <T> Spliterator.OfLong any(final long start, final Object[] shape, Storage<T> s) {
        if (start == 0) {
            return LongStream.range(0, s.estimateSize()).spliterator();
        } else {
            return new TreeIndexSpliterator(start, 2 /*FIXME*/);
        }
    }
}

class TreeIndexSpliterator implements Iteration {

    private long layerStart;
    private long layerSize;
    private long cursor;
    private final int branchingFactor;

    TreeIndexSpliterator(final long layerStart, final int branchingFactor) {
        this(layerStart, 1, branchingFactor);
    }

    TreeIndexSpliterator(final long layerStart, final long layerSize, final int branchingFactor) {
        assert(layerStart >= 0);
        assert(layerSize > 0);
        this.layerStart = layerStart;
        this.layerSize = layerSize;
        this.cursor = this.layerStart;
        this.branchingFactor = branchingFactor;
    }

    @Override
    public OfLong trySplit() {
        if (cursor == 0) {
            layerStart = layerStart*branchingFactor + 1;
            return new TreeIndexSpliterator(
                    2,
                    branchingFactor - 1,
                    branchingFactor);
        }
        final long leftSize = layerSize / 2;
        layerSize = layerSize - leftSize;
        final TreeIndexSpliterator ret = new TreeIndexSpliterator(
                layerStart,
                leftSize,
                branchingFactor);
        layerStart = layerStart + leftSize;
        return ret;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return DISTINCT | IMMUTABLE;
        //Unfortunately, it is NOT ordered (splitting would need to return
        //a prefix of `this`, and that can't be easily done)
    }

    private void moveDown() {
        layerSize *= branchingFactor;
        layerStart = layerStart*branchingFactor + 1;
        cursor = layerStart;
    }

    @Override
    public boolean tryAdvance(final LongConsumer action) {
        action.accept(cursor);
        cursor++;
        if (cursor >= layerStart+layerSize) {
            this.moveDown();
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"(start="+layerStart+", size="+layerSize+", branching="+branchingFactor+")";
    }
}

