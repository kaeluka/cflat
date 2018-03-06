package com.github.kaeluka.cflat.storage;

import org.HdrHistogram.Histogram;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class TimerStorage<T> implements StatisticsStorage<T> {
    private static final long highestTrackableValue = 3600000000000L;
    private static final int numberOfSignificantValueDigits = 3;
    private long totalTime = 0;
    public enum TIMER_CATEGORY {
        GET,
        GETORELSE,
        HAS,
        HASINRANGE,
        SET,
        CLEAR,
        SIZEOVERAPPROXIMATION,
        SIZEPRECISE,
        MOVESUBTREE,
        SETSUBTREE,
        SETRANGE,
        MOVERANGE,
        COPYRANGE,
        SETORMOVESUBTREE,
        FOREACHSUCCESSOR,
        FOREACHPARENT,
        FOREACH,
        FOREACHNONNULL,
        JOININNER,
        NONNULLINDICES,
        ADDALL,
        FIND,
        COPY,
        EMPTYCOPY
    }
    private final TreeMap<TIMER_CATEGORY, Histogram> histograms = new TreeMap<>();

    private final Storage innerStorage;

    public TimerStorage(final Storage<T> inner) {
        this.innerStorage = inner;
        assert ! (innerStorage instanceof TimerStorage);
        for (TIMER_CATEGORY t : TIMER_CATEGORY.values()) {
            final Histogram h = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
            h.setTag(t.toString());
            histograms.put(t, h);
        }
    }

    private TimerStorage<T> withNewIdentity(Storage<T> newInnerStorage) {
        return new TimerStorage<>(newInnerStorage, this);
    }

    private TimerStorage(Storage<T> s, TimerStorage old) {
        for (TIMER_CATEGORY t : TIMER_CATEGORY.values()) {
            histograms.put(t, ((Histogram) old.histograms.get(t)).copy());
        }
        innerStorage = s;
    }

    public Storage<T> getInner() {
        return this.innerStorage;
    }

    @SuppressWarnings("unchecked")
    public T get(final int i) {
        final long start = System.nanoTime();
        final Object ret = innerStorage.get(i);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.GET).recordValue(end - start);
        totalTime += (end - start);
        return (T) ret;
    }

    @SuppressWarnings("unchecked")
    public T getOrElse(final int i, final Supplier<T> s) {
        final long start = System.nanoTime();
        final Object ret = innerStorage.getOrElse(i, s);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.GETORELSE).recordValue(end - start);
        totalTime += (end - start);
        return (T) ret;
    }

    public boolean has(final int i) {
        final long start = System.nanoTime();
        final boolean ret = innerStorage.has(i);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.HAS).recordValue(end - start);
        totalTime += (end - start);
        return ret;
    }

    public boolean hasInRange(final int start, final int end) {
        final long start_ = System.nanoTime();
        final boolean ret = innerStorage.hasInRange(start, end);
        final long end_ = System.nanoTime();
        histograms.get(TIMER_CATEGORY.HASINRANGE).recordValue(end_ - start_);
        totalTime += (end_ - start_);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> set(final int i, final T x) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.set(i, x);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SET).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> clearAll() {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.clearAll();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.CLEAR).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    public int sizeOverApproximation() {
        final long start = System.nanoTime();
        final int ret = innerStorage.sizeOverApproximation();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SIZEOVERAPPROXIMATION).recordValue(end - start);
        totalTime += (end - start);
        return ret;
    }

    public int sizePrecise() {
        final long start = System.nanoTime();
        final int ret = innerStorage.sizePrecise();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SIZEPRECISE).recordValue(end - start);
        totalTime += (end - start);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.moveSubtree(source, shape, dest);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.MOVESUBTREE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.setSubtree(source, shape, dest);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SETSUBTREE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setRange(final int pos, final T x, final int length) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.setRange(pos, x, length);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SETRANGE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.moveRange(source, dest, length);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.MOVERANGE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.copyRange(source, dest, length);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.COPYRANGE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.setSubtree(source, shape, dest, depth, doMove);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.SETSUBTREE).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        final long start_ = System.nanoTime();
        innerStorage.foreachSuccessor(start, shape, f);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.FOREACHSUCCESSOR).recordValue(end - start_);
        totalTime += (end - start_);
    }

    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
        final long start_ = System.nanoTime();
        innerStorage.foreachParent(start, shape, f);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.FOREACHPARENT).recordValue(end - start_);
        totalTime += (end - start_);
    }

    public void foreach(final IntConsumer f) {
        final long start = System.nanoTime();
        innerStorage.foreach(f);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.FOREACH).recordValue(end - start);
        totalTime += (end - start);
    }

    public void foreachNonNull(final IntConsumer f) {
        final long start = System.nanoTime();
        innerStorage.foreachNonNull(f);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.FOREACHNONNULL).recordValue(end - start);
        totalTime += (end - start);
    }

    @SuppressWarnings("unchecked")
    public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f) {
        final long start = System.nanoTime();
        innerStorage.joinInner(other, f);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.JOININNER).recordValue(end - start);
        totalTime += (end - start);
    }

    @SuppressWarnings("unchecked")
    public Iterator<Integer> nonNullIndices() {
        final long start = System.nanoTime();
        final Iterator ret = innerStorage.nonNullIndices();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.NONNULLINDICES).recordValue(end - start);
        totalTime += (end - start);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> addAll(final Storage<T> source) {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.addAll(source);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.ADDALL).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public int find(final T x, final int max) {
        final long start = System.nanoTime();
        final int ret = innerStorage.find(x, max);
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.FIND).recordValue(end - start);
        totalTime += (end - start);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> copy() {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.copy();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.COPY).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    @SuppressWarnings("unchecked")
    public Storage<T> emptyCopy() {
        final long start = System.nanoTime();
        final Storage ret = innerStorage.emptyCopy();
        final long end = System.nanoTime();
        histograms.get(TIMER_CATEGORY.EMPTYCOPY).recordValue(end - start);
        totalTime += (end - start);
        if (ret == innerStorage) {
            return this;
        } else {
            return (Storage<T>) this.withNewIdentity(ret);
        }
    }

    public long bytesUsed() {
        return innerStorage.bytesUsed();
    }

    public String getStatistics(final String name) {
        StringBuilder ret = new StringBuilder();
        ret.append("## ").append(name).append("\n");
        for (TIMER_CATEGORY t : TIMER_CATEGORY.values()) {
            final Histogram histogram = histograms.get(t);
            if (histogram.getTotalCount() > 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ret.append("### ").append(t.toString()).append("\n");
                    PrintStream ps = new PrintStream(baos, true, "utf-8");
                    histogram.outputPercentileDistribution(ps, 1000.0);
                    ret.append(new String(baos.toByteArray(), StandardCharsets.UTF_8)).append("\n");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret.toString();
    }

    public Class<? extends Storage> getInnerClass() {
        return innerStorage.getClass();
    }

    public void resetStatistics() {
        for (TIMER_CATEGORY t : TIMER_CATEGORY.values()) {
            this.histograms.get(t).reset();
        }
    }

    public long getTotalTime() {
        return totalTime;
    }
}
