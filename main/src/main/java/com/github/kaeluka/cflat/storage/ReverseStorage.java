package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.Mutable;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

public class ReverseStorage<T> extends StorageWrapper<T> {
    private int locationOfZero = -1;

    public ReverseStorage() {
        this(new ArrayStorage<>());
    }

    public ReverseStorage(final Storage<T> innerStorage) {
        super(innerStorage);
    }

    private void ensureSize(int i) {
        final int innerIdx = switchIdx(i);
        if (innerIdx < 0) {
            int moveTo = -innerIdx;
            locationOfZero += moveTo;
            innerStorage.moveRange(0, moveTo, innerStorage.maxIdxOverapproximation());
        }
    }

    @Override
    protected StorageWrapper<T> withNewIdentity(final Storage<T> possiblyNewInnerStorage) {
        if (possiblyNewInnerStorage == innerStorage) {
            return this;
        } else {
            return new ReverseStorage<>(possiblyNewInnerStorage);
        }
    }

    private int switchIdx(int idx) {
        return locationOfZero - idx;
    }

    @Override
    public T get(final int i) {
        checkIndexIsNonnegative(i);
        final int p = switchIdx(i);
        if (p >= 0) {
            return innerStorage.get(p);
        } else {
            return null;
        }
    }

    @Override
    public boolean has(final int i) {
        checkIndexIsNonnegative(i);
        return switchIdx(i) >= 0 && innerStorage.has(switchIdx(i));
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        checkIndexIsNonnegative(i);
        ensureSize(i);
        final int p = switchIdx(i);
        return withNewIdentity(innerStorage.set(p, x));
    }

    @Override
    public T get2(final int i, final Mutable<T> v2) {
        checkIndexIsNonnegative(i);
        checkIndexIsNonnegative(i+1);
        // must flip the arguments to handle reverse order
        final Mutable<T> temp = new Mutable<>(null);
        v2.x = innerStorage.get2(switchIdx(switchIdx(i+1)), temp);
        return temp.x;
    }

    @Override
    public Storage<T> set2(final int i, final T x, final T y) {
        checkIndexIsNonnegative(i);
        checkIndexIsNonnegative(i+1);
        ensureSize(i+1);
        ensureSize(i);
        // must flip the arguments to maintain reverse order
        //noinspection SuspiciousNameCombination
        return withNewIdentity(innerStorage.set2(switchIdx(i+1), y, x));
    }

    @Override
    public Storage<T> clearAll() {
        return withNewIdentity(innerStorage.clearAll());
    }

    @Override
    public int maxIdxOverapproximation() {
        return this.locationOfZero + 1;
    }

//    public Storage<T> moveSubtree(final int source, final Object[] shape, final int dest)

//    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest)

    @Override
    public Storage<T> setRange(final int pos, final T x, final int length) {
        ensureSize(pos+length-1);
        return withNewIdentity(innerStorage.setRange(switchIdx(pos+length-1), x, length));
    }

    @Override
    public Storage<T> moveRange(final int source, final int dest, final int length) {
        checkIndexIsNonnegative(source);
        checkIndexIsNonnegative(dest);
        if (source < dest) {
            final int distance = dest - source;
            if (source == 0) {
                // moving to the right from the start
                locationOfZero += distance;
            } else {
                // moving to the right
                ensureSize(source+length-1);
                ensureSize(dest+length-1);
                // if the translated source index is negative, that means that
                // there's only nulls there -- we can set it to zero, adjust the
                // length, and null out that part of the destination manually
                innerStorage = innerStorage.moveRange(switchIdx(source + length - 1), switchIdx(dest + length - 1), length);
            }
        } else {
            ensureSize(source+length-1);
            ensureSize(dest+length-1);
            innerStorage = innerStorage.moveRange(switchIdx(source+length-1), switchIdx(dest+length-1), length);
        }
        return this;
    }

    @Override
    public Storage<T> copyRange(final int source, final int dest, final int length) {
        checkIndexIsNonnegative(source);
        checkIndexIsNonnegative(dest);
        if (length > 0) {
            ensureSize(source + length);   //FIXME: this could be done with less copying by skipping the ensureSize call, adapting innersrc and innerdst, and adding a setRange(.., null, ..) call
            ensureSize(dest + length);
        }
        final int innersrc = switchIdx(source + length - 1);
        final int innerdst = switchIdx(dest + length - 1);
        return withNewIdentity(innerStorage.copyRange(innersrc, innerdst, length));
    }

//    public Storage<T> setSubtree(final int source, final Object[] shape, final int dest, final int depth, final boolean doMove)

//    public void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
//        int cur = switchIdx(start);
//        while (cur >= 0) {
//            f.accept(cur--);
//        }
//    }

//    public void foreachParent(final int start, final Object[] shape, final IntConsumer f)

//    @Override
//    public void foreach(final IntConsumer f) {
//        innerStorage.foreach(i -> f.accept(switchIdx(i)));
//    }

//    @Override
//    public void foreachNonNull(final IntConsumer f) {
//        innerStorage.foreachNonNull(i -> f.accept(switchIdx(i)));
//    }

//    public <U> void joinInner(final Storage<U> other, final BiConsumer<T, U> f)

//    public Iterator<Integer> nonNullIndices()

//    public Storage<T> addAll(final Storage<T> source)

    @Override
    public int findFirst(final T x, final int max) {
        assert max == -1;
        final int idx = innerStorage.findFirst(x, max);
        if (idx == -1) {
            return -1;
        } else {
            return switchIdx(idx);
        }
    }
}
