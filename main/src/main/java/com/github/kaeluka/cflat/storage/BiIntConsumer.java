package com.github.kaeluka.cflat.storage;

@FunctionalInterface
public interface BiIntConsumer {
    public void accept(int row, int col);
}
