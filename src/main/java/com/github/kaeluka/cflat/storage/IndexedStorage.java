package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.lang.reflect.Field;
import java.util.*;

public class IndexedStorage<T> extends Storage<T> {
//    private Map<Integer, T> data = new Hashtable<>();
    private Hashtable<T, ArrayList<Integer>> dataReverse = new Hashtable<>();
//    private TObjectIntMap dataReverse = new TObjectIntHashMap();
    private final TIntObjectHashMap data = new TIntObjectHashMap();
    private int maxIdx = -1;

    @SuppressWarnings("unchecked")
    @Override
    public T get(final int i) {
        return (T) data.get(i);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<T> set(final int i, final T x) {
        if (x == null) {
            final T oldX = (T) data.remove(i);
            removeReverseBinding(i, oldX);
        } else {
            if (i > maxIdx) {
                maxIdx = i;
            }
            final T oldX = (T) data.put(i, x);
            dataReverse
                    .computeIfAbsent(x, k -> new ArrayList<>(1))
                    .add(i);
            removeReverseBinding(i, oldX);
        }
        return this;
    }

    private void removeReverseBinding(final int i, final T oldX) {
        if (oldX != null) {
            final ArrayList<Integer> oldPositions = dataReverse.get(oldX);
            if (oldPositions.size() > 1) {
                oldPositions.remove((Integer)i);
            } else {
                dataReverse.remove(oldX);
            }
        }
    }

    @Override
    public int find(final T x, final int max) {
        final ArrayList<Integer> positions = dataReverse.get(x);
        if (positions != null && !positions.isEmpty()) {
            return positions.get(0);
        } else {
            return -1;
        }
    }

    @Override
    public Storage<T> clear() {
        this.data.clear();
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public int sizePrecise() {
        int max = -1;
        for (int idx : data.keys()) {
            if (idx > max) {
                max = idx;
            }
        }
        return max+1;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new IndexedStorage<>();
    }

    @Override
    public long bytesUsed() {
        long size = ObjectSizes.HASHTABLE_SIZE(this.dataReverse);
        try {
            final Field values;
            values = data.getClass().getDeclaredField("_values");
            values.setAccessible(true);
            size += ObjectSizes.ARRAY_SIZE((Object[])values.get(data));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return size;
    }
}
