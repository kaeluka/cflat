package com.github.kaeluka.cflat.storage.size;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.TreeMap;

public  class ObjectSizes {
    public final static long OBJ_HEADER = 4;
    public final static long CHAR       = 1;
    public final static long BOOLEAN    = 1;
    public final static long INT        = 4;
    public final static long FLOAT      = 4;
    public final static long LONG       = 8;
    public final static long DOUBLE     = 8;
    public final static long REFERENCE  = 8;

    public static String humanReadable(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIdx = 0;
        double sz = size;
        while (sz > 1024 && unitIdx < units.length - 1) {
            sz /= 1024;
            unitIdx++;
        }

        final DecimalFormat df = new DecimalFormat("#.###");

        return df.format(sz)+units[unitIdx];
    }

    private static long ARRAY_SIZE(long COMPONENT_SIZE, int N) {
        return REFERENCE + INT + N*COMPONENT_SIZE;
    }
    public static long ARRAY_SIZE(char[] a) {
        return ARRAY_SIZE(CHAR, a.length);
    }
    public static long ARRAY_SIZE(boolean[] a) {
        return ARRAY_SIZE(BOOLEAN, a.length);
    }
    public static long ARRAY_SIZE(int[] a) {
        return ARRAY_SIZE(INT, a.length);
    }
    public static long ARRAY_SIZE(float[] a) {
        return ARRAY_SIZE(FLOAT, a.length);
    }
    public static long ARRAY_SIZE(long[] a) {
        return ARRAY_SIZE(LONG, a.length);
    }
    public static long ARRAY_SIZE(double[] a) {
        return ARRAY_SIZE(DOUBLE, a.length);
    }
    public static long ARRAY_SIZE(Object[] a) {
        return ARRAY_SIZE(REFERENCE, a.length);
    }

    public static long TINTOBJECTHASHMAP_SIZE(TIntObjectHashMap t) {
        try {
            final Field values;
            values = t.getClass().getDeclaredField("_values");
            values.setAccessible(true);
            return ObjectSizes.ARRAY_SIZE((Object[])values.get(t));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static long TREEMAP_SIZE(TreeMap t) {
        // the tree contains N nodes, each having key, value, left, right, parent
        // pointer fields, and a boolean for colour (most likely one byte)
        return t.size()* (OBJ_HEADER + REFERENCE * 4 + BOOLEAN);
    }

    public static long HASHTABLE_SIZE(Hashtable t) {
        long size = 0;
        try {
            final Field tableF = Hashtable.class.getDeclaredField("table");
            tableF.setAccessible(true);
            size += ARRAY_SIZE((Object[])tableF.get(t));
            final long HASHMAP_ENTRY_SIZE = OBJ_HEADER + INT + 3*REFERENCE;
            size += HASHMAP_ENTRY_SIZE * t.size();
            return size;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
