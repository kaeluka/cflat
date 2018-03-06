package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.Mutable;

import java.util.Arrays;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class IntTrieStorage implements Storage<Integer> {
    public final int[][][][] data = new int[0x80][][][];
    private final static int EMPTY = Integer.MIN_VALUE;

    private int maxIdx = -1;

    public Integer get(final int idx) {
        checkIndexIsNonnegative(idx);
        assert idx >= 0;

        int[][][] d1;
        final int coord1 = idx >> 24 & 0x7F;
        if ((d1 = data[coord1]) == null) {
            return null;
        }
        int[][] d2;
        final int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            return null;
        }
        int[] d3;
        final int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            return null;
        }

        return readFromChunk(d3, idx & 0xFF);
    }

    private static int[] newChunk() {
        final int[] chunk = new int[0x100];
        Arrays.fill(chunk, EMPTY);
        return chunk;
    }
    private static Integer readFromChunk(int[] chunk, int idx) {
        final int ret = chunk[idx];
        if (ret == EMPTY) {
            return null;
        } else {
            return ret;
        }
    }

    @Override
    public Integer get2(final int idx, final Mutable<Integer> v2) {
        checkIndexIsNonnegative(idx);
        checkIndexIsNonnegative(idx+1);

        int coord1 = idx >> 24 & 0x7F;
        int coord2 = idx >> 16 & 0xFF;
        int coord3 = idx >> 8 & 0xFF;
        int coord4 = idx & 0xFF;

        int[][][] d1;
        int[][] d2;
        int[] d3;
        if ((d1 = data[coord1]) == null ||
                (d2 = d1[coord2]) == null ||
                (d3 = d2[coord3]) == null) {
            return null;
        }
        if (coord4 != 0xFF) {
            v2.x = readFromChunk(d3, coord4+1);
        } else {
            handleOverflow(v2, d1, coord1, d2, coord2, coord3);
        }
        return readFromChunk(d3, coord4);
    }

    private void handleOverflow(final Mutable<Integer> v2, final int[][][] d1, final int coord1, final int[][] d2, final int coord2, final int coord3) {
        if (coord3 != 0xFF) {
            v2.x = d2[coord3+1][0];
        } else {
            if (coord2 != 0xFF) {
                v2.x = readFromChunk(d1[coord2+1][0],0);
            } else {
                if (coord1 != 0xFF) {
                    v2.x = readFromChunk(data[coord1+1][0][0], 0);
                } else {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }
        }
    }

    public Storage<Integer> set(final int idx, final Integer x) {
        checkIndexIsNonnegative(idx);
        if (idx > maxIdx) {
            maxIdx = idx;
        }

        int[][][] d1;
        int[][] d2;
        int[] d3;
        int coord1 = idx >> 24 & 0xFF;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new int[0x100][][];
        }
        int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            d2 = d1[coord2] = new int[0x100][];
        }
        int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            d3 = d2[coord3] = newChunk();
        }

        d3[idx & 0xFF] = x == null ? EMPTY : x;
        return this;
    }

    public Storage<Integer> set2(final int idx, final Integer x, final Integer y) {
        checkIndexIsNonnegative(idx);
        checkIndexIsNonnegative(idx+1);
        if (idx > maxIdx) {
            maxIdx = idx;
        }

        int[][][] d1;
        int[][] d2;
        int[] d3;
        final int coord1 = idx >> 24 & 0xFF;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new int[0x100][][];
        }
        final int coord2 = idx >> 16 & 0xFF;
        if ((d2 = d1[coord2]) == null) {
            d2 = d1[coord2] = new int[0x100][];
        }
        final int coord3 = idx >> 8 & 0xFF;
        if ((d3 = d2[coord3]) == null) {
            d3 = d2[coord3] = newChunk();
        }

        final int coord4 = idx & 0xFF;
        d3[coord4] = x;
        if (coord4 != 0xFF) {
            d3[coord4+1] = y == null ? EMPTY : y;
        } else {
            set(idx+1, y);
        }
        return this;
    }

    public Storage<Integer> clearAll() {
        Arrays.fill(data, null);
        return this;
    }

    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    public Storage<Integer> emptyCopy() {
        return new HashMapStorage<>();
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    public long bytesUsed() {
        System.err.println("returning bogus value for TrieStorage::bytesUsed");
        return -1;
    }
}
