package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import com.github.kaeluka.cflat.util.Mutable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import static com.github.kaeluka.cflat.util.IndexCheck.checkIndexIsNonnegative;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class TrieStorage<T> extends SizedStorage<T> {
    public final Object[][][][] data = new Object[0x80][][][];

    @Override
    public T get(final int idx) {
        checkIndexIsNonnegative(idx);

        Object[][][] d1;
        if ((d1 = data[getCoord1(idx)]) == null) {
            return null;
        }
        Object[][] d2;
        if ((d2 = d1[getCoord2(idx)]) == null) {
            return null;
        }
        Object[] d3;
        final int coord3 = getCoord3(idx);
        if ((d3 = d2[coord3]) == null) {
            return null;
        }

        return (T) d3[getCoord4(idx)];
    }

    private static int getCoord1(final int idx) {
        return idx >> 24 & 0x7F; // 0x7f because we nullify the sign bit
    }

    private static int getCoord2(final int idx) {
        return idx >> 16 & 0xFF;
    }

    private static int getCoord3(final int idx) {
        return idx >> 8 & 0xFF;
    }

    private static int getCoord4(final int idx) {
        return idx & 0xFF;
    }


    @Override
    public T get2(final int idx, final Mutable<T> v2) {
        checkIndexIsNonnegative(idx);
        checkIndexIsNonnegative(idx+1);
        assert idx >= 0;

        int coord1 = getCoord1(idx);
        int coord2 = getCoord2(idx);
        int coord3 = getCoord3(idx);
        int coord4 = getCoord4(idx);

        Object[][][] d1;
        Object[][] d2;
        Object[] d3;
        if ((d1 = data[coord1]) == null ||
                (d2 = d1[coord2]) == null ||
                (d3 = d2[coord3]) == null) {
            return null;
        }
        if (coord4 != 0xFF) {
            v2.x = (T) d3[coord4+1];
        } else {
            handleOverflow(v2, d1, coord1, d2, coord2, coord3);
        }
        return (T) d3[coord4];
    }

    private void handleOverflow(final Mutable<T> v2, final Object[][][] d1, final int coord1, final Object[][] d2, final int coord2, final int coord3) {
        if (coord3 != 0xFF) {
            v2.x = (T) d2[coord3+1][0];
        } else {
            if (coord2 != 0xFF) {
                v2.x = (T) d1[coord2+1][0][0];
            } else {
                if (coord1 != 0xFF) {
                    v2.x = (T) data[coord1+1][0][0][0];
                } else {
                    throw new ArrayIndexOutOfBoundsException();
                }
            }
        }
    }

    @Override
    public Storage<T> set(final int idx, final T x) {
        checkIndexIsNonnegative(idx);
        if (x == null && !has(idx)) { return this; }
        updateMaxIdx(idx);

        final Object[] d3 = ensureLvl3(idx, ensureLvl2(idx, ensureLvl1(idx)));
        d3[getCoord4(idx)] = x;
        return this;
    }

    private Object[][][] ensureLvl1(final int idx) {
        Object[][][] d1;
        if ((d1 = data[getCoord1(idx)]) == null) {
            d1 = data[getCoord1(idx)] = new Object[0x100][][];
        }
        return d1;
    }

    private Object[][] ensureLvl2(final int idx, final Object[][][] d1) {
        Object[][] d2;
        if ((d2 = d1[getCoord2(idx)]) == null) {
            d2 = d1[getCoord2(idx)] = new Object[0x100][];
        }
        return d2;
    }

    private Object[] ensureLvl3(final int idx, final Object[][] d2) {
        Object[] d3;
        if ((d3 = d2[getCoord3(idx)]) == null) {
            d3 = d2[getCoord3(idx)] = new Object[0x100];
        }
        return d3;
    }

    @Override
    public Storage<T> set2(final int idx, final T x, final T y) {
        checkIndexIsNonnegative(idx);
        updateMaxIdx(idx+1);

        Object[][][] d1;
        Object[][] d2;
        Object[] d3;
        final int coord1 = idx >> 24 & 0xFF;
        if ((d1 = data[coord1]) == null) {
            d1 = data[coord1] = new Object[0x100][][];
        }
        final int coord2 = getCoord2(idx);
        if ((d2 = d1[coord2]) == null) {
            d2 = d1[coord2] = new Object[0x100][];
        }
        final int coord3 = getCoord3(idx);
        if ((d3 = d2[coord3]) == null) {
            d3 = d2[coord3] = new Object[0x100];
        }

        final int coord4 = getCoord4(idx);
        d3[coord4] = x;
        if (coord4 != 0xFF) {
            d3[coord4+1] = y;
        } else {
            set(idx+1, y);
        }
        return this;
    }

    @Override
    public int findFirst(final T x, final int max) {
        assert max == -1;
        for (int c1 = 0; c1 < data.length; c1++) {
            final Object[][][] d1 = data[c1];
            if (d1 != null) {
                for (int c2 = 0; c2 < d1.length; c2++) {
                    final Object[][] d2 = d1[c2];
                    if (d2 != null) {
                        for (int c3 = 0; c3 < d2.length; c3++) {
                            final Object[] d3 = d2[c3];
                            if (d3 != null) {
                                for (int c4 = 0; c4 < d3.length; c4++) {
                                    if (d3[c4] != null && d3[c4].equals(x)) {
                                        return c4 + c3*0x100 + c2*0x100 + c1*0x100*0x80;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public void foreachNonNull(final IntConsumer f) {
        //I hate my life ;-)
        for (int c1 = 0; c1 < data.length; c1++) {
            final Object[][][] d1 = data[c1];
            if (d1 != null) {
                for (int c2 = 0; c2 < d1.length; c2++) {
                    final Object[][] d2 = d1[c2];
                    if (d2 != null) {
                        for (int c3 = 0; c3 < d2.length; c3++) {
                            final Object[] d3 = d2[c3];
                            if (d3 != null) {
                                for (int c4 = 0; c4 < d3.length; c4++) {
                                    if (d3[c4] != null) {
                                        f.accept(c4 + c3*0x100 + c2*0x100*0x100 + c1*0x100*0x100*0x80);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Storage<T> setRange(final int start, final T x, final int length) {
        final int end = start + length;
        updateMaxIdx(end);
        int cur = start;

        while (cur < end) {
            Object[] lvl3 = ensureLvl3(cur, ensureLvl2(cur, ensureLvl1(cur)));
            assert lvl3.length == 0x100;
            for (int i = getCoord4(cur); i < lvl3.length; i++) {
                lvl3[i] = x;
                cur++;
                if(cur >= end) { return this; }
            }
        }

        return this;
    }

    @Override
    public Storage<T> clearAll() {
        Arrays.fill(data, null);
        return this;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new HashMapStorage<>();
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    @Override
    public long bytesUsed() {
        long sz = ObjectSizes.ARRAY_SIZE(data) + ObjectSizes.INT;
        for (final Object[][][] lvl1 : data) {
            if (lvl1 != null) {
                sz += ObjectSizes.ARRAY_SIZE(lvl1);
                for (final Object[][] lvl2 : lvl1) {
                    if (lvl2 != null) {
                        sz += ObjectSizes.ARRAY_SIZE(lvl2);
                        for (final Object[] lvl3 : lvl2) {
                            if (lvl3 != null) {
                                sz += ObjectSizes.ARRAY_SIZE(lvl3);
                            }
                        }
                    }
                }
            }
        }
        return sz;
    }
}
