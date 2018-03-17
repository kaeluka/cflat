package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.ArrayStorage;
import com.github.kaeluka.cflat.storage.Storage2D;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class Storage2DTest {
    @Test
    public void simpleGet() {
        final Storage2D<Integer> st = new Storage2D<>(new ArrayStorage<>(), new ArrayStorage<>());
        assertThat(st.get(0), not(nullValue()));
    }
}