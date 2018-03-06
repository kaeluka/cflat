package com.github.kaeluka.cflat.traversal;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class GenericShapeTest {
    @Test
    public void testPairingFunction() {
        for (int i = 0; i<1000; ++i) {
            for (int j = 0; j<1000; ++j) {
                final long z = GenericShape.cantorPairingFunction(i, j);
                final int[] xy = GenericShape.cantorPairingFunctionRev(z);
                final int x = xy[0];
                final int y = xy[1];
                assertThat(x, is(i));
                assertThat(y, is(j));
            }
        }
    }
}