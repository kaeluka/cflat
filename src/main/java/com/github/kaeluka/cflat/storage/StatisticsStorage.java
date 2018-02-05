package com.github.kaeluka.cflat.storage;

public abstract class StatisticsStorage<T> extends Storage<T> {
    public abstract String getStatistics(final String name);

    public abstract long getTotalTime();

    public abstract void resetStatistics();
}
