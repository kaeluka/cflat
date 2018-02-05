package com.github.kaeluka.cflat.storage;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public interface Iteration extends Spliterators.AbstractSpliterator.OfInt {
    public static <T> Spliterator.OfInt any(final int  start, final Object[] shape, Storage<T> s) {
        if (start == 0) {
            return IntStream.range(0, s.sizeOverApproximation()).spliterator();
        } else {
            return new TreeIndexSpliterator(start, 2 /*FIXME*/);
        }
    }
}

class TreeIndexSpliterator implements Iteration {

    private int layerStart;
    private int  layerSize;
    private int  cursor;
    private final int branchingFactor;

    TreeIndexSpliterator(final int layerStart, final int branchingFactor) {
        this(layerStart, 1, branchingFactor);
    }

    TreeIndexSpliterator(final int layerStart, final int  layerSize, final int branchingFactor) {
        assert(layerStart >= 0);
        assert(layerSize > 0);
        this.layerStart = layerStart;
        this.layerSize = layerSize;
        this.cursor = this.layerStart;
        this.branchingFactor = branchingFactor;
    }

    @Override
    public OfInt trySplit() {
        if (cursor == 0) {
            layerStart = layerStart*branchingFactor + 1;
            return new TreeIndexSpliterator(
                    2,
                    branchingFactor - 1,
                    branchingFactor);
        }
        final int  leftSize = layerSize / 2;
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
        return Integer.MAX_VALUE;
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
    public boolean tryAdvance(final IntConsumer action) {
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

