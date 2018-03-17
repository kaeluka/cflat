package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class TimersStorage<T> implements StatisticsStorage<T> {
    private static final long highestTrackableValue = 3600000000000L;
    private static final int numberOfSignificantValueDigits = 3;
    private final TimerStorage<T>[] innerStorages;
    private final String name;
    private double timeWeight= 3;
    private double bytesWeight= 1;

    public void setPriority(double timeWeight, double bytesWeight) {
        assert timeWeight >= 0.0;
        assert bytesWeight >= 0.0;
        this.timeWeight= timeWeight;
        this.bytesWeight= bytesWeight;
    }

    @SuppressWarnings("unchecked")
    public static <T> TimersStorage<T> defaults(final String name) {
        return new TimersStorage<>(name,
                new ArrayStorage<>(),
                new HashMapStorage<>(),
                new ChunkedStorage<>(),
                new TrieStorage<>(),
                new ShallowTrieStorage<>(),
                new IndexedStorage<>());
    }

    @SuppressWarnings("unchecked")
    public TimersStorage(final String name, final Storage<T>... inner) {
        this.name = name;
        this.innerStorages = new TimerStorage[inner.length];
        for (int i=0; i<inner.length; ++i) {
            this.innerStorages[i] = new TimerStorage(inner[i]);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(final int i) {
        T ret = null;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.get(i);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public T getOrElse(final int i, final Supplier<T> s) {
        T ret = null;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.getOrElse(i, s);
        }
        return ret;
    }

    public boolean has(final int i) {
        boolean ret = false;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.has(i);
        }
        return ret;
    }

    public boolean hasInRange(final int start, final int end) {
        boolean ret = false;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.hasInRange(start, end);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> set(final int i, final T x) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage)innerStorages[s].set(i, x);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> clearAll() {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage)innerStorages[s].clearAll();
        }
        return this;
    }

    public int maxIdxOverapproximation() {
        int ret = 0;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.maxIdxOverapproximation();
        }
        return ret;
    }

    public int maxIdx() {
        int ret = 0;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.maxIdx();
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].moveSubtree(source, shape, dest);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].setSubtree(source, shape, dest);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setRange(final int pos, final T x, final int length) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].setRange(pos, x, length);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].moveRange(source, dest, length);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].copyRange(source, dest, length);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage) innerStorages[s].setSubtree(source, shape, dest, depth, doMove);
        }
        return this;
    }

    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.foreachSuccessor(start, shape, f);
        }
    }

    public void foreachParent(final int start, final Object[] shape, final IntConsumer f) {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.foreachParent(start, shape, f);
        }
    }

    public void foreach(final IntConsumer f) {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.foreach(f);
        }
    }

    public void foreachNonNull(final IntConsumer f) {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.foreachNonNull(f);
        }
    }

    @SuppressWarnings("unchecked")
    public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f) {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.joinInner(other, f);
        }
    }

    @SuppressWarnings("unchecked")
    public Iterator<Integer> nonNullIndices() {
        Iterator<Integer> ret = null;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.nonNullIndices();
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> addAll(final Storage<T> source) {
        for (int s=0; s<innerStorages.length; ++s) {
            innerStorages[s] = (TimerStorage<T>)innerStorages[s].addAll(source);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public int findFirst(final T x, final int max) {
        int ret = -1;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret = innerStorage.findFirst(x, max);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Storage<T> copy() {
        final TimerStorage<T>[] newInnerStorages = new TimerStorage[innerStorages.length];
        for (int s=0; s<innerStorages.length; ++s) {
            newInnerStorages[s] = (TimerStorage<T>)innerStorages[s].copy();
        }
        return new TimersStorage<>(this.name+" copy", newInnerStorages);
    }

    @SuppressWarnings("unchecked")
    public Storage<T> emptyCopy() {
        final Storage<T>[] newInnerStorages = new Storage[innerStorages.length];
        for (int s=0; s<innerStorages.length; ++s) {
            newInnerStorages[s] = innerStorages[s].getInner().emptyCopy();
        }
        return new TimersStorage<>(this.name+" empty copy", newInnerStorages);
    }

    public long bytesUsed() {
        long ret = 0;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret += innerStorage.bytesUsed();
        }
        return ret;
    }

    public String getStatistics(final String name) {
        StringBuilder ret = new StringBuilder();
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret.append("# ").append(name).append("\n");
            ret.append(innerStorage.getStatistics(innerStorage.getInnerClass().getSimpleName())).append("\n");
        }
        ret.append("RECOMMENDATION for "+name+": ");
        final Class<? extends Storage> recommendation = this.recommendation();
        ret.append(recommendation == null ? "NONE" : recommendation.getSimpleName());
        ret.append("\n");

        ret.append("Inners were:\n");
        for (final TimerStorage<T> innerStorage : innerStorages) {
            ret.append(innerStorage.getInnerClass().getSimpleName()).append("\n");
            ret.append(" - time used:   ").append(String.format("%.2e", innerStorage.getTotalTime() / 1000.0)).append(" µs\n");
            ret.append(" - memory used: ").append(ObjectSizes.humanReadable(innerStorage.bytesUsed())).append("\n");
            ret.append("\n");
        }
        return ret.toString();
    }

    public long getTotalTime() {
        long total = 0;
        for (final TimerStorage<T> innerStorage : innerStorages) {
            total += innerStorage.getTotalTime();
        }
        return total;
    }

    public void resetStatistics() {
        for (final TimerStorage<T> innerStorage : innerStorages) {
            innerStorage.resetStatistics();
        }
    }

    public Class<? extends Storage> recommendation() {
        if (getTotalTime() == 0) {
            return null;
        } else {
            long minTime = Long.MAX_VALUE;
            long minBytes = Long.MAX_VALUE;
            Class<? extends Storage> minClass = null;
            for (final TimerStorage<T> innerStorage : innerStorages) {
                if (innerStorage.getTotalTime() < minTime) {
                    minTime = innerStorage.getTotalTime();
                }
                if (innerStorage.bytesUsed() < minBytes) {
                    minBytes = innerStorage.bytesUsed();
                }
            }

            double bestScore = Double.MAX_VALUE;
            Class<? extends Storage> bestClass = null;

            for (final TimerStorage<T> innerStorage : innerStorages) {
                final long bytesUsed = innerStorage.bytesUsed();
                final long totalTime = innerStorage.getTotalTime();
                if(bytesUsed < 0) {
                    throw new AssertionError(
                            "inner storage of type "+
                                    innerStorage.getInnerClass().getSimpleName()+
                                    " has negative stepSize");
                }
                final double bytesScore = bytesUsed *1.0 / minBytes;
                final double timeScore  = totalTime *1.0 / minTime;
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": bytes =" + bytesUsed);
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": bytes (HR) =" + ObjectSizes.humanReadable(bytesUsed));
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": time =" + (totalTime/1000)+"us");
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": bytesScore =" + bytesScore);
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": timeScore  =" + timeScore);
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": bytesScore corrected =" + bytesScore * bytesWeight);
//                System.out.println(innerStorage.getInnerClass().getSimpleName() + ": timeScore corrected  =" + timeScore * timeWeight);
                final double score = bytesWeight * bytesScore + timeWeight * timeScore;

                if (score < bestScore) {
                    bestScore = score;
                    bestClass = innerStorage.getInnerClass();
                }

            }

            return bestClass;
        }
    }
}
