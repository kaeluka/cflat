package com.github.kaeluka.cflat.util;

public class IndexCheck {
    public static void checkLengthIsNonnegative(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("length must be non-negative. Was: "+i);
        }
    }
    public static void checkIndexIsNonnegative(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("index must be non-negative. Was: "+i);
        }
    }
}
