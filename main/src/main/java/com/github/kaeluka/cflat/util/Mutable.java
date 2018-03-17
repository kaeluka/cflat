package com.github.kaeluka.cflat.util;

public class Mutable<T> {
    public T x;
    public Mutable(T x) { this.x = x; };

    @Override
    public String toString() {
        return "Mutable(" + "x=" + x + ')';
    }
};
