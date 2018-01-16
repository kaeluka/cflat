package com.github.kaeluka.cflat.storage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HashMapStorage<T> implements Storage<T> {
    private final Map<Integer, Object> map = new TreeMap<>();
    private static final int INIT_CAPACITY = 1 << 10;
    private static final int INIT_BUCKET_SIZE = 1 << 4;

    private static final int MAX_CAPACITY = 1 << 30;

    private final int[][] keyBuckets = new int[INIT_CAPACITY][];
    private final Object[][] objectBuckets = new Object[INIT_CAPACITY][];

    private static int keyBucketGetIdx(final int[] keyBucket, final int i) {
        assert(keyBucket != null);
        return Arrays.binarySearch(keyBucket, i);
    }

    private int getBucketIdx(final int i) {
        final int ret = i & keyBuckets.length - 1;
        return ret;
    }

    private int[] ensureKeyBucket(final int bucketIdx) {
        int[] kbucket = this.keyBuckets[bucketIdx];
        if (kbucket == null) {
            kbucket = new int[INIT_BUCKET_SIZE];
            Arrays.fill(kbucket, Integer.MAX_VALUE);
            this.keyBuckets[bucketIdx] = kbucket;
        }
        return kbucket;
    }

    private Object[] ensureObjectBucket(final int bucketIdx) {
        Object[] obucket = this.objectBuckets[bucketIdx];
        if (obucket == null) {
            obucket = new Object[INIT_BUCKET_SIZE];
            this.objectBuckets[bucketIdx] = obucket;
        }
        return obucket;
    }

    @Override
    public T get(final int i) {
        final int bucketIdx = getBucketIdx(i);
        int[] kbucket = ensureKeyBucket(bucketIdx);
        Object[] obucket = ensureObjectBucket(bucketIdx);
        final int idx = keyBucketGetIdx(kbucket, i);
        if (idx >= 0) {
            return (T) obucket[idx];
        } else {
            return null;
        }
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        final int bucketIdx = getBucketIdx(i);
        int[] kbucket = ensureKeyBucket(bucketIdx);
        Object[] obucket = ensureObjectBucket(bucketIdx);
        final int idx = keyBucketGetIdx(kbucket, i);
        if (idx >= 0) {
            obucket[idx] = x;
        } else {
            // this position is not yet in the bucket; we have to move
            // some to the right!
            int insertionPoint = -(idx+1);
            if (insertionPoint == obucket.length) {
                //the bucket is full -- need more space!
                kbucket = Arrays.copyOf(kbucket, kbucket.length*2);
                Arrays.fill(kbucket, insertionPoint, kbucket.length, Integer.MAX_VALUE);
                obucket = Arrays.copyOf(obucket, obucket.length*2);
                keyBuckets[bucketIdx] = kbucket;
                objectBuckets[bucketIdx] = obucket;
            }
            System.arraycopy(
                    kbucket,
                    insertionPoint,
                    kbucket, insertionPoint+1,
                    kbucket.length - insertionPoint - 1);
            System.arraycopy(
                    obucket,
                    insertionPoint,
                    obucket, insertionPoint+1,
                    obucket.length - insertionPoint - 1);
            kbucket[insertionPoint] = i;
            obucket[insertionPoint] = x;
        }
        assert(get(i) == x);
        return this;
    }

    @Override
    public Storage<T> clear() {
        this.map.clear();
        return this;
    }
}
