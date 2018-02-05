package com.github.kaeluka.cflat.storage;


import java.util.Arrays;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.traversal.GenericShape;

public final class ArrayStorage<T> extends Storage<T> {
    private final static int DEFAULT_SIZE = 10;
    private Object[] data;
    private int maxIdx;

    public ArrayStorage() {
        this(new Object[DEFAULT_SIZE]);
    }

    private ArrayStorage(final Object[] data) {
        this.data = data;
        this.maxIdx = 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(final int i) {
        if (i>=data.length) return null;
        return (T) this.data[i];
    }

    private void ensureSize(final int i) {
        if (this.data.length <= i) {
            final Object[] old_data = this.data;
            int newLength = old_data.length;
            while (newLength <= i) {
                newLength *= 2;
            }
            this.data = Arrays.copyOf(this.data, newLength);
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        assert i >= 0;
        maxIdx = i > maxIdx ? i : maxIdx;
        this.ensureSize(i);
        this.data[i] = x;
        return this;
    }

    @Override
    public Storage<T> clear() {
        for(int i=0; i<this.data.length; ++i) {
            this.data[i] = null;
        }
        return this;
    }

    @Override
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        this.ensureSize(dest+length);
        System.arraycopy(this.data, source, this.data, dest, length);
        return this;
    }

    @Override
    public Storage<T> setRange(final int pos, final T x, final int length) {
        ensureSize(pos+length);
        Arrays.fill(this.data, pos, pos+length, x);
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage setSubtree(final int source,
                              final Object[] shape,
                              final int dest) {
        if ((GenericShape.isRep(shape) || GenericShape.isStar(shape)) && (GenericShape.shapeSize((Object[]) shape[1]) == 1) && (GenericShape.shapeSize((Object[]) shape[2]) == 1)) {
            final int sE = sizeOverApproximation();
            int valsToCopy = sE - source;
            this.ensureSize(dest+valsToCopy);
            System.arraycopy(this.data, source, this.data, dest, valsToCopy);
            if (dest > source) {
                maxIdx += dest-source;
            }
            return this;
        } else {
            return super.setSubtree(source, shape, dest);
        }
    }

    @Override
    public Storage<T> copy() {
        return new ArrayStorage<>(Arrays.copyOf(data, data.length));
    }

    @Override
    public Storage<T> emptyCopy() {
        return new ArrayStorage<>();
    }

    @Override
    public long bytesUsed() {
        return ObjectSizes.ARRAY_SIZE(this.data);
    }
}
