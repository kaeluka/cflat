package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.SparseStorage;
import com.github.kaeluka.cflat.storage.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SparseStorageTest {

    @Parameterized.Parameter
    public Storage<Storage<Integer>> st;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Storage<Storage<Integer>>> sparseStorages() {
        ArrayList<Storage<Storage<Integer>>> ret = new ArrayList<>();
        for (SparseStorage.USAGE u : SparseStorage.USAGE.values()) {
            ret.add(SparseStorage.getFor(u));
        }
//        ret.add(SparseStorage.getFor(SparseStorage.USAGE.SIZE));
        return ret;
    }

    @Test
    public void sizes() {
        st.clearAll();
        // input data from wikipedia example:
        //    https://en.wikipedia.org/wiki/Sparse_matrix#Compressed_sparse_column_(CSC_or_CCS)
        assertThat(st.sizeOverApproximation(), greaterThanOrEqualTo(0)); // rows 0,1
        assertThat(st.sizePrecise(), is(0));

        st.get(1).set(0, 5);
        assertThat(st.sizeOverApproximation(), greaterThanOrEqualTo(2)); // rows 0,1
        assertThat(st.sizePrecise(), is(2)); // rows 0,1

        st.get(3).set(1, 6);
        assertThat(st.sizeOverApproximation(), greaterThanOrEqualTo(4)); // rows 0,1,2,3
        assertThat(st.sizePrecise(), is(4)); // rows 0,1,2,3

        st.get(2).set(1, 6);
        assertThat(st.sizeOverApproximation(), greaterThanOrEqualTo(4)); // nothing changed
        assertThat(st.sizePrecise(), is(4)); // nothing changed
    }

    @Test
    public void set() {
        st.clearAll();
        // input data from wikipedia example:
        //    https://en.wikipedia.org/wiki/Sparse_matrix#Compressed_sparse_column_(CSC_or_CCS)
        st.get(1).set(0, 5);
        st.get(1).set(1, 8);
        st.get(2).set(2, 3);
        st.get(3).set(1, 6);
        System.out.println(st);
        assertThat(st.get(1).get(0), is(5));
        assertThat(st.get(1).get(1), is(8));
        assertThat(st.get(2).get(2), is(3));
        assertThat(st.get(3).get(1), is(6));
        assertThat(st.sizeOverApproximation(), is(4)); // rows 0,1,2,3
    }

    @Test
    public void regression1() {
        st.clearAll();
        System.out.println(st);
        System.out.println("setting 0,0 = 0");
        st.get(0).set(0, 1);
        System.out.println(st);
        System.out.println("setting 0,1 = 2");
        st.get(0).set(1, 2);
        System.out.println(st);
        System.out.println("setting 0,2 = 3");
        st.get(0).set(2, 3);
        System.out.println(st);
    }

}