package com.github.kaeluka.pma;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class PMATest {

    @Test
    public void testInsert() {
        //inputs from https://github.com/dhruvbird/packed-memory-array
        PMA<String> p1 = new PMA<>(16);
        final List<Integer> keys = Arrays.asList(80, 50, 70, 90, 65, 85, 10, 21, 22, 20, 24, 15, 17, 23);
        final List<String> values = keys.stream().map(Object::toString).collect(Collectors.toList());

        for (int i = 0; i < keys.size(); i++) {
            p1.put(keys.get(i), values.get(i));

            for (int j = 0; j <= i; j++) {
                assertThat("after inserting val #"+i+": "+values.get(i) + ", val #"+j+" must still be there",
                        p1.get(keys.get(j)), is(values.get(j)));
            }
        }
    }

    @Test
    public void testRandomMapInserts() {
        Hashtable<Integer, String> t = new Hashtable<>(16);
        final int N = 1000000;
        Random random = new Random(12345L);
        String[] vals = new String[N];

        for (int i = 0; i < N; i++) {
            if (i % (N/100) == 0) {
                System.out.println("storing: "+i / (N / 100) + "%");
            }
            final int key = random.nextInt(N);
            final String val = Integer.toString(random.nextInt());
            t.put(key, val);
            vals[key] = val;
            assertThat(t.get(key), is(vals[key]));
        }

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) {
                assertThat(String.format("%d -> %s", i, vals[i]), t.get(i), is(vals[i]));
            }
        }
    }
    @Test
    public void testRandomInserts() {
        PMA<String> p1 = new PMA<>(16);
        final int N = 1000000;
        Random random = new Random(12345L);
        String[] vals = new String[N];

        for (int i = 0; i < N; i++) {
            if (i % (N/100) == 0) {
                System.out.println("storing: "+i / (N / 100) + "%");
                System.out.println(p1.getDensityMap());
            }
            final int key = random.nextInt(N);
            final String val = Integer.toString(random.nextInt());
            p1.put(key, val);
            vals[key] = val;
            assertThat(p1.get(key), is(vals[key]));
        }
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) {
                assertThat(String.format("%d -> %s", i, vals[i]), p1.get(i), is(vals[i]));
            }
        }
    }

    @Test
    public void testConsecutiveInserts() {
        PMA<Integer> p1 = new PMA<>(16);
        final int N = 1000000;

        for (int i = 0; i < N; i++) {
            if (i % ((N-1)/100) == 0) {
                System.out.println("storing: "+i / ((N-1) / 100) + "%");
                System.out.println(p1.getDensityMap());
            }
            p1.put(i, i);
        }
        for (int i = 0; i < N; i++) {
            if (i % ((N-1)/100) == 0) {
                System.out.println("reading: "+i / ((N-1) / 100) + "%");
            }
            assertThat(p1.get(i), is(i));
        }
    }
}