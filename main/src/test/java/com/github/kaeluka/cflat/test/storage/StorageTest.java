package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.Storage;
import com.github.kaeluka.cflat.traversal.GenericShape;
import com.github.kaeluka.cflat.util.Mutable;
import com.github.kaeluka.cflat.util.Storages;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import scala.util.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(Parameterized.class)
public class StorageTest extends junit.framework.TestCase {
    private final Supplier<Storage> storageSupplier;

    @Parameterized.Parameters(name="{0}")
    public static List<Supplier<Storage>> storages() {
        return Storages.genericStorages();
    }

    @SuppressWarnings("unchecked")
    private Storage<Integer> mkStorage() {
        return storageSupplier.get();
    }

    public StorageTest(Supplier<Storage> storageSupplier) {
        this.storageSupplier = storageSupplier;
    }

    @Test
    public void simpleSetAndGetTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.foreachNonNull(i -> {
            throw new AssertionError("index "+i+" should not be defined!");
        });

        final int N = 10;
        for (int i=0; i<N; ++i) {
            st = st.set(i, i);
        }
        for (int i=0; i<N; ++i) {
            assert(st.has(i));
            assertThat("at "+i, st.get(i), is(i));
        }

        for (int i=0; i<N/2; ++i) {
            st = st.set(i, null);
        }
        for (int i=0; i<N/2; ++i) {
            assert(!st.has(i));
            assertThat(st.get(i), nullValue());
        }
    }

    @Test
    public void emptyCopyTest() {
        Storage<Integer> st1 = mkStorage();
        final int N = 100;
        for (int i = 0; i < N; i++) {
            st1 = st1.set(i, i);
        }

        Storage<Integer> st2 = st1.emptyCopy();
        for (int i = 0; i < N; i++) {
            assertThat(st1.get(i), is(i));
            assertThat(st2.get(i), nullValue());
        }
    }

    @Test
    public void foreachNonNullTest() {
        Storage<Integer> st = mkStorage();
        final int N = 10;
        final Random random1 = new Random(12345L);
        final TIntHashSet keys = new TIntHashSet();

        for (int i = 0; i< N; ++i) {
            final int key = random1.nextInt(N);
            System.out.println("storing " + key);
            keys.add(key);
            st = st.set(key, key);
        }

        Mutable<Integer> cnt = new Mutable<>(0);

        final Random random2 = new Random(12345L);
        st.foreachNonNull(i -> {
            cnt.x++;
            final int key = random2.nextInt(N);
            System.out.println("reading "+key);
            assertThat("key="+key, keys.contains(key));
        });
        assertThat("total number of indices is correct", cnt.x, is(keys.size()));
    }

    @Test
    public void setRangeTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st = st.setRange(254, 123, 10);
        for (int i = 0; i < 20000; i++) {
            if (i<254 || i >= 264) {
                assertThat("i="+i, !st.has(i));
            } else {
                assert(st.has(i));
                assertThat("at index "+i, st.get(i), is(123));
            }
        }

        st = st.setRange(257, null, 3);
        for (int i = 0; i < 20000; i++) {
            if (i >= 257 && i < 260) {
                assertThat("i="+i, !st.has(i));
            } else if (i<254 || i >= 264) {
                assertThat("i="+i, !st.has(i));
            } else {
                assertThat("i="+i, st.has(i));
                assertThat("i="+i, st.get(i), is(123));
            }
        }
    }

    @Test
    public void randomSetAndGetTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        Random random = new Random(12345L);
        st.foreachNonNull(i -> {
            throw new AssertionError("index "+i+" should not be defined!");
        });

        final int N = 20;
        for (int i=0; i<N; ++i) {
            final int r = random.nextInt(1000);
            st = st.set(r, r);
            for (int j=0; j<i; ++j) {
                Random tmprandom = new Random(12345);
                final int s = tmprandom.nextInt(1000);
                assertThat("i="+i+", j="+j, st.has(s));
                assertThat("i="+i+", j="+j, st.get(s), is(s));
            }
        }
        random = new Random(12345);
        for (int i=0; i<N; ++i) {
            final int r = random.nextInt(1000);
            assertThat("i="+i+": r="+r, st.has(r));
            assertThat("i="+i+": r="+r, st.get(r), is(r));
        }

        random = new Random(12345L);
        for (int i=0; i<N/2; ++i) {
            final int r = random.nextInt(1000);
            st = st.set(r, null);
        }
        random = new Random(12345L);
        for (int i=0; i<N/2; ++i) {
            final int r = random.nextInt(1000);
            assertThat("#"+i+": r="+r, !st.has(r));
            assertThat(st.get(r), nullValue());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void moveRangeThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.moveRange(0, -1, 10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyRangeThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.copyRange(0, -1, 10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get2ThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.get2(-1, new Mutable<>(0));
    }


    @Test
    public void nonNullIndicesTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;

        final int SIZE = 1000;

        final Random random = new Random(12345L);
        int idx = 0;
        for (int i = 0; i < SIZE; i++) {
            idx += random.nextInt(99)+1;
            System.out.println("#"+i+": setting " + idx + " -> " + i);
            st = st.set(idx, i);
        }

        final Iterator<Integer> idxIter = st.nonNullIndices();
        idx = 0;
        for (int i = 0; i < SIZE; i++) {
            idx += random.nextInt(99)+1;
            assertThat("#"+i, st.get(idxIter.next()), is(i));
        }
        assertFalse(idxIter.hasNext());
    }

    @Test
    public void findTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;

        for (int i = 0; i < 10; i++) {
            st = st.set(i, i);
        }

        for (int i = 0; i < 10; i++) {
            assertThat("i="+i, st.findFirst(i, -1), is(i));
        }

        for (int i = 0; i < 10; i++) {
            assertThat(st.findFirst(10+i, -1), is(-1));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.set(-1, 12);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void set2ThrowsTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.set2(-1, 12, 13);
    }

    @Test
    public void copyRangeTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;

        final int OFFSET = 10;

        for (int i=0; i<10; ++i) {
            st = st.set(i+OFFSET, i);
        }

        for (int i=0; i<10; ++i) {
            assertThat(st.get(i+OFFSET), is(i));
        }
        // pos:
        // offset+0 1 2 3 4 5 6 7 8 9
        // -------------------------------
        // ...... 0 1 2 3 4 5 6 7 8 9 .......
        // ...... 0 1 2 0 1 2 3 4 8 9 .......
        st = st.copyRange(OFFSET, OFFSET+3, 5);
        for (int i=0; i<100; ++i) {
            System.out.println(i);
            switch (i) {
                case OFFSET:
                case OFFSET+1:
                case OFFSET+2:
                    assertThat("at pos OFFSET+"+(i-OFFSET), st.get(i), is(i - OFFSET));
                    break;
                case OFFSET+3:
                case OFFSET+4:
                case OFFSET+5:
                case OFFSET+6:
                case OFFSET+7:
                    assertThat("at pos OFFSET+"+(i-OFFSET), st.get(i), is(i - OFFSET - 3));
                    break;
                case OFFSET+8:
                case OFFSET+9:
                    assertThat("at pos OFFSET+"+(i-OFFSET), st.get(i), is(i - OFFSET));
                    break;
                default:
                    assert !st.has(i);
            }
        }
    }

    @Test
    public void simpleMoveRangeTest() {
        Storage<Integer> st = mkStorage();
        assert(st != null);

        final int N = 10;
        for (int i = 0; i < N; i++) {
            st = st.set(i, i);
        }
        for (int i = 0; i < N; i++) {
            assertThat(st.get(i), is(i));
        }

        System.out.println(st);
        st = st.moveRange(N / 2, (N/2) + 2, 5);
        System.out.println(st);

        for (int i = 0; i < N / 2; i++) {
            assertThat("st.get("+i+") = "+i, st.get(i), is(i));
        }
        assertThat(st.get((N/2)), nullValue());
        assertThat(st.get((N/2)+1), nullValue());

        for (int i = N/2; i < N; i++) {
            assertThat(st.get(i+2), is(i));
        }
    }

    @Test
    public void simpleCopyRangeTest() {
        Storage<Integer> st = mkStorage();
        assert(st != null);

        final int N = 10;
        for (int i = 0; i < N; i++) {
            st = st.set(i, i);
        }
        for (int i = 0; i < N; i++) {
            assertThat(st.get(i), is(i));
        }

        st = st.copyRange(N / 2, (N/2) + 2, 5);

        for (int i = 0; i < N / 2; i++) {
            assertThat(st.get(i), is(i));
        }
        assertThat(st.get((N/2)), is(N/2));
        assertThat(st.get((N/2)+1), is((N/2)+1));

        for (int i = N/2; i < N; i++) {
            assertThat(st.get(i+2), is(i));
        }
    }

    @Test
    public void farCopyRangeTest() {
        Storage<Integer> st = mkStorage();
        assert(st != null);

        final int N = 10;
        for (int i = 0; i < N; i++) {
            st = st.set(i, i);
        }
        st = st.copyRange(0, 1000, 10);

        for (int i = 0; i < N / 2; i++) {
            assertThat(st.get(i), is(i));
            assertThat(st.get(i+1000), is(i));
        }
    }

    @Test
    public void forEachSuccessorRepTest() {
        Storage<Integer> st = mkStorage();
        assert(st != null);
        final int DEPTH = 4;
        final int limit = (int) Math.pow(2, DEPTH);
        for (int i = 0; i<limit; ++i) { // fill the first N layers
            st = st.set(i, i);
        }
        assertThat(st.maxIdxOverapproximation(), is(limit));
//        st = st.set(limit + 5, limit + 5);
        for (int i = 0; i<limit; ++i) {
            assertTrue(st.has(i));
        }
        for (int i = limit; i<1000; ++i) {
            assertTrue(!st.has(i) || i == limit+5);
        }

        ArrayList<Integer> successors = new ArrayList<>();
        //successors in a binary tree of depth 2
        st.foreachSuccessor(0, new Object[]{2, 2, 1}, successors::add);
        assertThat(successors.size(), is(3));
        for (int i=0; i<3; ++i) {
            assertThat(i, isIn(successors));
        }
    }

    @Test
    public void forEachSuccessorStartTest() {
        Storage<Integer> st = mkStorage();
        assert(st != null);
        final int DEPTH = 4;
        final int limit = (int) Math.pow(2, DEPTH);
        for (int i = 0; i<limit; ++i) { // fill the first N layers
            st = st.set(i, i);
        }
        st = st.set(limit + 5, limit + 5);
        for (int i = 0; i<limit; ++i) {
            assertTrue(st.has(i));
        }
        for (int i = limit; i<1000; ++i) {
            assertTrue(!st.has(i) || i == limit+5);
        }

        ArrayList<Integer> successors = new ArrayList<>();
        //successors in a binary tree of unlimited depth
        st.foreachSuccessor(0, GenericShape.mkStar(2, 1), successors::add);
        assertThat(successors.size(), is(limit+1));
        for (int i=0; i<limit; ++i) {
            assertThat(i, isIn(successors));
        }
    }

    @Test
    public void sizeTest() {
        Storage<Integer> st = mkStorage();
        int max = Integer.MIN_VALUE;
        final Random random = new Random(12345L);
        for (int i = 0; i < 100; i++) {
            final int idx = random.nextInt(10000);
            if (max < idx) {
                max = idx;
            }

            st = st.set(idx, i);
            assertThat("at i="+i+", max="+max+", idx="+idx, st.maxIdxOverapproximation(), is(max+1));
            assertThat("at i="+i+", max="+max+", idx="+idx, st.maxIdx(), is(max+1));
        }
    }

    @Test
    public void moveRangeTest() {
        Storage<Integer> st = mkStorage();
        final int N = 500;
        for (int i = 0; i < N; i++) {
            st = st.moveRange(0, 1, i);
            assertThat("at index "+i, st.get(0), nullValue());
            st = st.set(0, i);
            assertThat("at index "+i, st.get(0), is(i));
        }

        for (int i = 0; i < N; i++) {
            assertThat(st.get(i), is(N-i-1));
        }
    }

    @Test
    public void moveSubtreeUpTest() {
        /*
        transform:
                0:0            0:0
                /              /
             1:-1            1:1
              /     ==>      /
            3:1            3:2
            /              / \
           7:2            7:3 8:4
          /   \
        15:3 16:4
         */
        Storage<Integer> st = mkStorage();
        assert(st != null);
        st = st.set(0, 0);
        st = st.set(1, -1);
        st = st.set(3, 1);
        st = st.set(7, 2);
        st = st.set(15, 3);
        st = st.set(16, 4);

        for (int i=0; i<100; ++i) {
            switch (i) {
                case 0: assertThat(st.get(i), is(0)); break;
                case 1: assertThat(st.get(i), is(-1)); break;
                case 3: assertThat(st.get(i), is(1)); break;
                case 7: assertThat(st.get(i), is(2)); break;
                case 15: assertThat(st.get(i), is(3)); break;
                case 16: assertThat(st.get(i), is(4)); break;
                default: assertTrue(!st.has(i));
            }
        }
        System.out.println(st);
        st = st.moveSubtree(3, GenericShape.mkStar(2,1), 1);
        System.out.println(st);

        for (int i=0; i<100; ++i) {
            switch (i) {
                case 0: assertThat(st.get(i), is(0)); break;
                case 1: assertThat(st.get(i), is(1)); break;
                case 3: assertThat(st.get(i), is(2)); break;
                case 7: assertThat(st.get(i), is(3)); break;
                case 8: assertThat(st.get(i), is(4)); break;
                default: assertTrue(!st.has(i));
            }
        }
    }

    @Test
    public void moveSubtreeRightStarTest() {
        /*
        transform:
              0:0              0:0
              /                 \
            1:1                2:1
            /       ==>        /
          3:2                5:2
          / \               /   \
        7:3 8:4           11:3 12:4
         */
        Storage<Integer> st = mkStorage();
        assert(st != null);
        st = st.set(0, 0);
        st = st.set(1, 1);
        st = st.set(3, 2);
        st = st.set(7, 3);
        st = st.set(8, 4);

        System.out.println(st);
        st = st.moveSubtree(1, GenericShape.mkStar(2,1), 2);
        System.out.println(st);
        final Storage<Integer> thSt = st;
        st.foreachNonNull(i -> System.out.println(i+" -> "+thSt.get(i)));

        for (int i=0; i<100; ++i) {
            switch (i) {
                case 0: assertThat(st.get(i), is(0)); break;
                case 2: assertThat(st.get(i), is(1)); break;
                case 5: assertThat(st.get(i), is(2)); break;
                case 11: assertThat(st.get(i), is(3)); break;
                case 12: assertThat(st.get(i), is(4)); break;
                default: assertTrue(!st.has(i));
            }
        }
    }

    @Test
    public void set2Size() {
        Storage<Integer> st = mkStorage();
        st = st.set2(10, 1, 2);
        assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(12));
        assertThat(st.maxIdx(), is(12));
    }

    @Test
    public void moveStorageSize() {
        final int SIZE = 5000;
        Storage<Integer> st = mkStorage();
        assert(st != null);
        for (int i = 0; i < SIZE; i++) {
            st = st.set(i*2, i);
            assertThat(st.maxIdxOverapproximation(), greaterThanOrEqualTo(i));
            assertThat(st.maxIdx(), is(i*2+1));
        }
    }

    @Test
    public void moveSubtreeDownTest() {
        /*
        transform:
              0:0             0:0
              /               /
            1:1             null
            /       ==>     /
          3:2             3:1
          / \             /
        7:3 8:4         7:2
                        /   \
                      15:3 16:4
         */
        Storage<Integer> st = mkStorage();
        assert(st != null);
        st = st.set(0, 0);
        st = st.set(1, 1);
        st = st.set(3, 2);
        st = st.set(7, 3);
        st = st.set(8, 4);
        final Storage<Integer> thSt = st;
        thSt.foreachNonNull(i -> System.out.println(i+" -> "+thSt.get(i)));

        st = st.moveSubtree(1, GenericShape.mkStar(2,1), 3);
        final Storage<Integer> thSt2 = st;
        thSt2.foreachNonNull(i -> System.out.println(i+" -> "+thSt2.get(i)));

        for (int i=0; i<100; ++i) {
            switch (i) {
                case 0: assertThat(st.get(i), is(0)); break;
                case 2: assertTrue(!st.has(i)); break;
                case 3: assertThat(st.get(i), is(1)); break;
                case 7: assertThat(st.get(i), is(2)); break;
                case 15: assertThat(st.get(i), is(3)); break;
                case 16: assertThat(st.get(i), is(4)); break;
                default: assertTrue(!st.has(i));
            }
        }
    }

    @Test
    public void joinInnerTest() {
        Storage<Integer> st1 = this.mkStorage();
        Storage<Integer> st2 = this.mkStorage();

        for (int i = 0; i < 1000; i++) {
            st1 = st1.set(2*i, 2*i);
            st2 = st2.set(3*i, 3*i);
        }

        ArrayList<Integer> res = new ArrayList<>();
        int expectedLength = 0;
        for (int i = 0; i < 3000; i++) {
            if (st1.has(i) && st2.has(i)) {
                expectedLength++;
            }
        }


        st1.joinInner(st2,
                (v1, v2) -> res.add(v1*v2));

        assertThat(res, hasSize(expectedLength));

        for (final Integer v : res) {
            assert v % 2 == 0;
            assert v % 3 == 0;
        }

    }

    @Test
    public void test() {
        Storage<Integer> storage = this.mkStorage();
        assert(storage != null);
        assertThat(storage, notNullValue());
        for (int i=0; i<=10000; i++) {
            storage = storage.set(i, -i);
        }
        for (int i=0; i<=10000; i++) {
            if (storage.has(i)) {
                assertThat(storage.get(i), is(-i));
            }
        }
    }
}
