package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.NestedStorage;
import com.github.kaeluka.cflat.storage.Storage;
import com.github.kaeluka.cflat.util.Storages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SparseStorageTest {

    @Parameterized.Parameter
    public Supplier<NestedStorage<Integer>> supplier;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Supplier<NestedStorage<Integer>>> sparseStorages() {
        return Storages.nestedStorages();
    }

    @Test
    public void sizes() {
        final Storage<Storage<Integer>> st = supplier.get();
        // input data from wikipedia example:
        //    https://en.wikipedia.org/wiki/Sparse_matrix#Compressed_sparse_column_(CSC_or_CCS)
        assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(0)); // rows 0,1
        assertThat(st.maxIdx(), is(0));

        st.get(1).set(0, 5);
        assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(2)); // rows 0,1
        assertThat(st.maxIdx(), is(2)); // rows 0,1

        st.get(3).set(1, 6);
        assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(4)); // rows 0,1,2,3
        assertThat(st.maxIdx(), is(4)); // rows 0,1,2,3

        st.get(2).set(1, 6);
        assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(4)); // nothing changed
        assertThat(st.maxIdx(), is(4)); // nothing changed
    }

    @Test
    public void set() {
        final Storage<Storage<Integer>> st = supplier.get();
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
        assertThat(st.maxIdxOverapproximation(), is(4)); // rows 0,1,2,3
    }

    @Test
    public void copyNestedTest() {
        final NestedStorage<Integer> st = supplier.get();
        final int N = 100;
        // construct an "identity matrix"
        for (int i = 0; i < N; i++) {
            st.get(i).set(i, 1);
        }

        final NestedStorage<Integer> cp = st.copyNested();

        // "multiply" by 2:
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (st.get(row).has(col)) {
                    st.get(row).set(col, 2);
                }
            }
        }
        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (row == col) {
                    assert st.get(row).has(col);
                    assertThat("cp must be defined at diagonal ["+row+","+col+"]",
                            cp.get(row).has(col), is(true));
                    assertThat(st.get(row).get(col), is(2));
                    assertThat(cp.get(row).get(col), is(1));
                } else {
                    assertThat("st must be undefined off the diagonal ["+row+","+col+"]",
                            st.get(row).has(col), is(false));
                    assertThat("cp must be undefined off the diagonal ["+row+","+col+"]",
                            cp.get(row).has(col), is(false));
                }
            }
        }
    }

    @Test
    public void regressionSetColumnReverseTest() {
        // used to hit a bug in the CSR storage
        final NestedStorage<Integer> st = supplier.get();
        final int N = 20;

        for (int i = 0; i < N; i++) {
            System.out.println("setting [" + (N-i-1) + ", 10]");
            st
                    .get(N-i-1)
                    .set(10, 10);
        }
        for (int i = 0; i < N; i++) {
            assertThat("checking ["+i+", 10]", st.get(i).get(10), is(10));
        }
    }

    @Test
    public void regression1() {
        final Storage<Storage<Integer>> st = supplier.get();
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

    @Test
    public void regression2() {
        final Storage<Storage<Integer>> st = supplier.get();
        int v = 1;
        // init:
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                System.out.print(v + " ");
                final Storage<Integer> rowS = st.get(row);
                rowS.set(col, v++);
            }
            System.out.println("");
        }
        // check:
        v = 1;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                assertThat(st.get(row).get(col), is(v++));
            }
        }
        // change row 1:
        for (int col = 0; col < 3; col++) {
            final Storage<Integer> rowS = st.get(1);
            rowS.set(col, 10);
        }
        v = 1;
        for (int row = 0; row < 2; row++) {
            if (row == 1) {
                //the changed row
                for (int col = 0; col < 3; col++) {
                    assertThat(st.get(row).get(col), is(10));
                }
                v++;
            } else {
                for (int col = 0; col < 3; col++) {
                    assertThat(st.get(row).get(col), is(v++));
                }
            }
        }

    }

}