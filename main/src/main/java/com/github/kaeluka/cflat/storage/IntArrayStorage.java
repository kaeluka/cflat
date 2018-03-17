package com.github.kaeluka.cflat.storage;


import com.github.kaeluka.cflat.traversal.GenericShape;

import java.util.Arrays;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

public final class IntArrayStorage implements Storage<Integer> {
    private final static int DEFAULT_SIZE = 10;
    private int[] data;
    private int maxIdx;

    public IntArrayStorage() {
        this.data = new int[DEFAULT_SIZE];
        Arrays.fill(this.data, 0, this.data.length, Integer.MIN_VALUE);
        this.maxIdx = 0;
    }

    private IntArrayStorage(final int[] data) {
        this.data = data;
        this.maxIdx = 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer get(final int i) {
        checkIndexIsNonnegative(i);
        if (i>=this.data.length || Integer.MIN_VALUE == this.data[i]) {
            return null;
        } else {
            return this.data[i];
        }
    }

    private void ensureSize(final int i) {
        if (this.data.length <= i) {
//            final int[] old_data = this.data;
//            int newLength = old_data.length;
//            while (newLength <= i) {
//                newLength *= 2;
//            }
            int oldLength = this.data.length;
            int newLength = i + (i >> 1);
            this.data = Arrays.copyOf(this.data, newLength);
            Arrays.fill(this.data, oldLength, newLength, Integer.MIN_VALUE);
        }
    }

    @Override
    public Storage<Integer> set(final int i, final Integer x) {
        checkIndexIsNonnegative(i);
        int xi = (x == null) ? Integer.MIN_VALUE : x;
        maxIdx = i > maxIdx ? i : maxIdx;
        this.ensureSize(i);
        this.data[i] = xi;
        return this;
    }

    @Override
    public Storage<Integer> clearAll() {
        for(int i=0; i<this.data.length; ++i) {
            this.data[i] = 0;
        }
        return this;
    }

    @Override
    public Storage<Integer> copyRange(final int source, final int dest, final int length) {
        this.ensureSize(dest+length);
        this.ensureSize(source+length);
        System.arraycopy(this.data, source, this.data, dest, length);
        return this;
    }

    @Override
    public Storage<Integer> setRange(final int pos, final Integer x, final int length) {
        ensureSize(pos+length);
        Arrays.fill(this.data, pos, pos+length, x == null ? Integer.MIN_VALUE : x);
        return this;
    }

    @Override
    public int maxIdxOverapproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage setSubtree(final int source,
                              final Object[] shape,
                              final int dest) {
        if ((GenericShape.isRep(shape) || GenericShape.isStar(shape)) && (GenericShape.shapeSize((Object[]) shape[1]) == 1) && (GenericShape.shapeSize((Object[]) shape[2]) == 1)) {
            final int sE = maxIdxOverapproximation();
            int valsToCopy = sE - source;
            this.ensureSize(dest+valsToCopy);
            System.arraycopy(this.data, source, this.data, dest, valsToCopy);
            if (dest > source) {
                maxIdx += dest-source;
            }
            return this;
        } else {
            return Storage.super.setSubtree(source, shape, dest);
        }
    }

    @Override
    public Storage<Integer> copy() {
        return new IntArrayStorage(Arrays.copyOf(data, data.length));
    }

    @Override
    public boolean has(final int i) {
        if (i>=this.data.length || Integer.MIN_VALUE == this.data[i]) {
            return false;
        } else {
            return true;
        }
//        try {
//            return this.get(i) != Integer.MIN_VALUE;
//        } catch (NullPointerException e) {
//            System.out.println("hello");
//            return this.data[i] != Integer.MIN_VALUE;
//        }
    }

    @Override
    public Storage<Integer> emptyCopy() {
        return new IntArrayStorage();
    }

    @Override
    public long bytesUsed() {
        return this.data.length*32;
    }
}
