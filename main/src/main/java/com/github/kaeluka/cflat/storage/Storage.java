package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.traversal.GenericShape;
import com.github.kaeluka.cflat.util.IndexCheck;
import com.github.kaeluka.cflat.util.Mutable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public interface Storage<T> {
    public abstract T get(int i);

    public default T get2(int i, Mutable<T> v2) {
        final T v = get(i);
        v2.x = get(i+1);
        return v;
    }

    public default T getOrElse(int i, Supplier<T> s) {
        final T g = get(i);
        if (g != null) {
            return g;
        } else {
            return s.get();
        }
    }

    public default boolean has(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("illegal index: "+i);
        }
        return this.get(i) != null;
    }

    public default boolean hasInRange(int start, int end) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("illegal index range arguments ["+start+", "+end+")");
        }
        for (int i=start; i<end; ++i) {
            if (has(i)) return true;
        }
        return false;
    }

    public Storage<T> set(int i, T x);

    public default Storage<T> set2(int i, T x, T y) {
        IndexCheck.checkIndexIsNonnegative(i);
        IndexCheck.checkIndexIsNonnegative(i+1);
        return this
                .set(i, x)
                .set(i + 1, y);
    }

    public default Storage<T> computeIfAbsent(int i, Function<Integer, T> f, Mutable<T> result) {
        result.x = this.get(i);
        if (result.x == null) {
            result.x = f.apply(i);
            return this.set(i, result.x);
        } else {
            return this;
        }
    }

    public Storage<T> clearAll();

    public int sizeOverApproximation();

//    public Storage<T> replaceOrSet(T old, int i, T x) {
//        int idx = find(old, -1);
//        if (idx < 0) {
//            return set(i, x);
//        } else {
//            return set(idx, x);
//        }
//    }

    public default int sizePrecise() {
        int estimate = sizeOverApproximation();
        assert(estimate >= 0);
//        assert(!has(estimate));
        while (estimate > 0 && !has(estimate-1)) { estimate --; }
        return estimate;
    }

    public default Storage<T> moveSubtree(final int source,
                                         final Object[] shape,
                                         final int dest) {
        return setSubtree(source, shape, dest, -1, true);
    }

    public default Storage<T> setSubtree(final int source,
                                            final Object[] shape,
                                            final int dest) {
        return setSubtree(source, shape, dest, -1, false);
    }

    public default Storage<T> setRange(final int pos, T x, final int length) {
        final int end = pos + length;
        Storage<T> ret = this;
        for (int i = pos; i< end; ++i) {
            ret = ret.set(i, x);
        }
        return ret;
    }

    public default Storage<T> moveRange(final int source, final int dest, final int length) {
        Storage<T> ret = this.copyRange(source, dest, length);
        if (source < dest) {
            // moving to the right
            final int toNullify = Math.min(length, dest-source);
            ret = ret.setRange(source, null, toNullify);
        } else {
            final int deleteFrom = Math.max(dest+length, source);
            ret = ret.setRange(deleteFrom, null, source +length - deleteFrom);
        }
        return ret;
    }

    public default Storage<T> copyRange(final int source, final int dest, final int length) {
        IndexCheck.checkIndexIsNonnegative(source);
        IndexCheck.checkIndexIsNonnegative(dest);
        IndexCheck.checkLengthIsNonnegative(length);
        Storage<T> ret = this;
        if (dest < source) {
            // copying to the left
            for (int i=0; i<length; ++i) {
                final int sourceI = source+i;
                final int destI = dest+i;
                ret = ret.set(destI, this.get(sourceI));
            }
        } else {
            // copying to the right
            for (int i=length-1; i>=0; --i) {
                final int sourceI = source+i;
                final int destI = dest+i;
                ret = ret.set(destI, this.get(sourceI));
            }
        }
        return ret;
    }

    default Storage<T> setSubtree(final int source,
                                  final Object[] shape,
                                  final int dest,
                                  final int depth,
                                  boolean doMove) {
        assert(dest != source);
        assert(depth == -1);
        if (GenericShape.isRep(shape) || GenericShape.isStar(shape)) {
            final int branchingFactor = GenericShape.shapeSize(shape[1]);
            Storage<T> st = this;
            if (branchingFactor == 1 && (int) shape[2] == 1) {
                final int diff = source - dest;
//                st = st.moveRange(source, dest, st.sizeOverApproximation() - source);
                int cur = source;
                if (dest < source) {
                    T x = get(cur);
                    while (x != null) {
                        st = st.set(cur - diff, x);
                        cur++;
                        x = get(cur);
                    }
                } else {
//                     find the first null value:
                    while (has(cur)) {
                        cur++;
                    }
                    while (cur >= source) {
                        st = st.set(cur - diff, get(cur));
                        cur--;
                    }
                }
                return st;
            } else if ((int) shape[2] == 1 || new Random().nextInt() == Integer.MIN_VALUE) {
                if (GenericShape.isParentOfTransitive(source, dest, shape)) {
                    int sourceRangeStart = source;
                    int destRangeStart = dest;
                    int rangeWidth = 1;

                    // 1. find level until which the move must go
                    boolean continueMoving = true;
                    while (continueMoving) {
                        continueMoving = false;
                        for (int child = 0; child < rangeWidth; ++child) {
                            if (st.has(sourceRangeStart + child)) {
                                continueMoving = true;
                                break;
                            }
                        }
                        sourceRangeStart = sourceRangeStart * branchingFactor + 1;
                        destRangeStart = destRangeStart * branchingFactor + 1;
                        rangeWidth *= branchingFactor;
                    }
                    sourceRangeStart = sourceRangeStart * branchingFactor + 1;
                    destRangeStart = destRangeStart * branchingFactor + 1;
                    rangeWidth *= branchingFactor;

                    // 2. copy the layers, starting with the lowest
                    while (rangeWidth > 1) {
                        if (doMove) {
                            st = st.moveRange(sourceRangeStart, destRangeStart, rangeWidth);
                        } else {
                            st = st.copyRange(sourceRangeStart, destRangeStart, rangeWidth);
                        }
                        sourceRangeStart = GenericShape.parentOf(sourceRangeStart, shape);
                        destRangeStart   = GenericShape.parentOf(destRangeStart,   shape);
                        rangeWidth /= branchingFactor;
                    }
                    st = st.set(dest, st.get(source));
                    if (doMove) {
                        st = st.set(source, null);
                    }

                    return st;
                } else {
                    // moving up!
                    int sourceRangeStart = source;
                    int destRangeStart = dest;
                    int rangeWidth = 1;

                    boolean continueMoving = true;
                    while (continueMoving) {
                        continueMoving = hasInRange(sourceRangeStart, sourceRangeStart+rangeWidth);
                        st = st.moveRange(sourceRangeStart, destRangeStart, rangeWidth);
//                        continueMoving = false;
//                        for (int child = 0; child < rangeWidth; ++child) {
//                            final T gotten = st.get(sourceRangeStart+child);
//                            if (gotten != null) {
//                                continueMoving = true;
//                            }
//                            st = st.set(destRangeStart + child, gotten);
//                            if (doMove) {
//                                st = st.set(sourceRangeStart + child, null);
//                            }
//                        }
                        sourceRangeStart = sourceRangeStart * branchingFactor + 1;
                        destRangeStart = destRangeStart * branchingFactor + 1;
                        rangeWidth *= branchingFactor;
                    }
                    return st;
                }
            }
        }
        throw new UnsupportedOperationException("not yet implemented for " +
                "complex shapes");
    }

    public default void foreachSuccessor(final int start, final Object[] shape, final IntConsumer f) {
        if (GenericShape.isRep(shape)) {
            if ((int) shape[1] == 1) {
                assert ((int) shape[2] == 1);
                int cur = start;
                final int len = (int) shape[0];
                while (cur < len && has(cur)) {
                    f.accept(cur++);
                }
                return;
            } else {
                //n-ary tree
                assert ((int) shape[1] > 1);
                assert ((int) shape[2] == 1);
                if (has(start)) {
                    int maxDepth = (int) shape[0];
                    int innerSize = (int) shape[1];
                    int levelStart = start;
                    int levelEnd = start+1;
                    for (int level=0; level<maxDepth; ++level) {
                        for (int child=levelStart; child<levelEnd; ++child) {
                            if (has(child)) {
                                f.accept(child);
                            }
                        }
                        levelStart = levelStart* innerSize + 1;
                        levelEnd   = levelEnd  * innerSize + 1;
                    }
//                    for (int child = 0; child < innerSize; ++child) {
//                        foreachSuccessor(start * innerSize + child + 1, shape, f);
//                    }
                }
                return;
            }
        }
        if (GenericShape.isStar(shape)) {
            if ((int) shape[1] == 1) {
                assert ((int) shape[2] == 1);
                int cur = start;
                while (has(cur)) {
                    f.accept(cur++);
                }
                return;
            } else {
                //n-ary tree
                assert ((int) shape[1] > 1);
                assert ((int) shape[2] == 1);
                if (has(start)) {
                    f.accept(start);
                    final int innerSize = (int) shape[1];
                    for (int child = 0; child < innerSize; ++child) {
                        foreachSuccessor(start * innerSize + child + 1, shape, f);
                    }
                }
                return;
            }
        }
        throw new UnsupportedOperationException("impl not done!");
    }

    public default void foreachParent(int start, Object[] shape, IntConsumer f) {
        if (GenericShape.isRep(shape)) {
            //FIXME: only sequential this far!
            assert ((int) shape[1] == 1);
            assert ((int) shape[2] == 1);
            int cur = start;
            while (has(cur)) {
                f.accept(cur--);
            }
            return;
        }
        throw new UnsupportedOperationException("impl not done!");
    }

    public default void foreach(IntConsumer f) {
        final int max = this.sizePrecise();
        if (max < 0) {
            throw new AssertionError("sizePrecise < 0 (is "+this.sizePrecise()+") for "+this);
        }
        for (int i=0; i<max; ++i) {
            f.accept(i);
        }
    }

    public default void foreachNonNull(IntConsumer f) {
        final int max = this.sizePrecise();
        if(max < 0) {
            throw new AssertionError("sizePrecise < 0 (is "+this.sizePrecise()+") for "+this);
        }
        for (int i=0; i<=max; ++i) {
//            System.out.println(get(i));
            if (has(i)) {
                f.accept(i);
            }
        }
    }

    public default <U> void joinInner(Storage<U> other, BiConsumer<T,U> f) {
        final Iterator<Integer> thisIdxs = this.nonNullIndices();
        final Iterator<Integer> otherIdxs = other.nonNullIndices();
        while (thisIdxs.hasNext() && otherIdxs.hasNext()) {
            final int i = thisIdxs.next();
            final int j = otherIdxs.next();
            if (i == j) {
                f.accept(this.get(i), other.get(j));
            }
        }
    }

    public default Iterator<Integer> nonNullIndices() {
        ArrayList<Integer> list = new ArrayList<>(this.sizeOverApproximation());
        foreachNonNull(list::add);
        list.trimToSize();
        return list.iterator();
    }

    public default Storage<T> addAll(final Storage<T> source) {
        final Mutable<Storage<T>> ret = new Mutable<>(source);
        source.foreachNonNull(i -> ret.x = set(i, source.get(i)));
        return ret.x;
    }

    public default int find(T x, int max) {
        int _max = max < 0 ? this.sizeOverApproximation() : max;
        for(int i=0; i<_max; ++i) {
            if (x.equals(get(i))) {
                return i;
            }
        }
        return -1;
    }

    public default Storage<T> copy() {
        final Storage<T> copy = emptyCopy();
        copy.addAll(this);
        return copy;
    }

    public Storage<T> emptyCopy();

    public long bytesUsed();

//    @Override
//    public default String toString() {
//        int end = Math.min(15, this.sizePrecise());
//        if (end == 0) {
//            return "[]";
//        } else {
//            StringBuilder ret = new StringBuilder("[");
//            for (int i = 0; i < end - 1; ++i) {
//                ret.append(get(i)).append(", ");
//            }
//            ret.append(get(end - 1)).append("]");
//            return ret.toString();
//        }
//    }
}

