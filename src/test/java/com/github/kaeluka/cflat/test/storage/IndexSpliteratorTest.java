package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.IndexSpliterator;
import org.junit.Test;

import java.util.PrimitiveIterator;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class IndexSpliteratorTest {
    private IndexSpliterator getSpliter(long start, long size, int branchingFactor) {
        return new IndexSpliterator(start, size, branchingFactor);
    }

    private IndexSpliterator getSpliter(long start, int branchingFactor) {
        return this.getSpliter(start, 1, branchingFactor);
    }

    private LongStream getStream(long start, long size, int branchingFactor) {
        IndexSpliterator spliterator = this.getSpliter(start, size, branchingFactor);
        LongStream strm = StreamSupport.longStream(
                spliterator,
                false);
        return strm;
    }

    private LongStream getStream(long start, int branchingFactor) {
        return this.getStream(start, 1, branchingFactor);
    }

    @Test
    public void fromRoot() {
        final PrimitiveIterator.OfLong iter = this.getStream(0, 2).iterator();
        for(long i=0; i<100; ++i) {
            assertThat(iter.next(), is(i));
        }
    }

    @Test
    public void fromLeftChild() {
        final PrimitiveIterator.OfLong iter = this.getStream(1, 2).iterator();
        assertThat(iter.next(), is(1L));
        assertThat(iter.next(), is(3L));
        assertThat(iter.next(), is(4L));
        assertThat(iter.next(), is(7L));
        assertThat(iter.next(), is(8L));
    }

    @Test
    public void split() {
        final IndexSpliterator all = this.getSpliter(0, 2);
        all.tryAdvance((LongConsumer) System.out::println);
        final PrimitiveIterator.OfLong split = StreamSupport.longStream(all.trySplit(), false).iterator();

        final PrimitiveIterator.OfLong expected = this.getStream(1, 2).iterator();

        for (int i=0; i<100; ++i) {
            assertThat(split.next(), is(expected.next()));
        }
    }

    @Test
    public void splitRoot() {
        final IndexSpliterator allspl = this.getSpliter(0, 2);
        final PrimitiveIterator.OfLong all = StreamSupport.longStream(allspl, false).iterator();
        final PrimitiveIterator.OfLong split = StreamSupport.longStream(allspl.trySplit(), false).iterator();

        assertThat(all.next(), is(0L));
        final PrimitiveIterator.OfLong expectedAll = this.getStream(1, 2).iterator();
        final PrimitiveIterator.OfLong expectedSplit = this.getStream(2, 2).iterator();

        for (int i=0; i<100; ++i) {
            assertThat(all.next(), is(expectedAll.next()));
        }
        for (int i=0; i<100; ++i) {
            assertThat(split.next(), is(expectedSplit.next()));
        }
    }

    /**
     * same as split root, but with a 7-ary-tree.
     */
    @Test
    public void splitRoot7() {
        final IndexSpliterator allspl = this.getSpliter(0, 7);
        final PrimitiveIterator.OfLong all = StreamSupport.longStream(allspl, false).iterator();
        final PrimitiveIterator.OfLong split = StreamSupport.longStream(allspl.trySplit(), false).iterator();

        assertThat(all.next(), is(0L));
        final PrimitiveIterator.OfLong expectedAll = this.getStream(1, 7).iterator();
        final PrimitiveIterator.OfLong expectedSplit = this.getStream(2, 6,7).iterator();

        for (int i=0; i<100; ++i) {
            assertThat(all.next(), is(expectedAll.next()));
        }

        for (int i=0; i<100; ++i) {
            assertThat(split.next(), is(expectedSplit.next()));
        }
    }
}
