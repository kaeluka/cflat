package com.github.kaeluka.cflat.storage;

public interface StatisticsStorage<T> extends Storage<T> {
    String getStatistics(final String name);

    long getTotalTime();

    void resetStatistics();
}
