package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.*;
import com.github.kaeluka.cflat.traversal.GenericShape;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.reflections.Reflections;
import scala.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(Parameterized.class)
public class StorageTest extends junit.framework.TestCase {
    private final Class<? extends Storage> storageklass;

    @Parameterized.Parameters(name="storage: {0}")
    public static Collection<Class<? extends Storage>> storages() {
        final Reflections reflections = new Reflections("com.github.kaeluka.cflat");

        final Set<Class<? extends Storage>> subtypes = reflections.getSubTypesOf(Storage.class);
        subtypes.removeIf(clazz -> ! hasSimpleCtor(clazz));
        subtypes.remove(EmptyStorage.class);
        subtypes.add(IndexedStorage.class);

        // CSRStorage can not handle the integers used in these tests:
        subtypes.remove(SparseStorage.class);
        return subtypes;
    }

    private static boolean hasSimpleCtor(Class<? extends Storage> clazz) {
        try {
            clazz.newInstance();
            return true;
        } catch (InstantiationException | IllegalAccessException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Storage<Integer> mkStorage() {
        try {
            return this.storageklass.newInstance();
        } catch (InstantiationException e) {
            fail("Storage class can not be instantiated:\n"+e.getMessage());
            return null;
        } catch (IllegalAccessException e) {
            fail("Storage class can not be accessed:\n"+e.getMessage());
            return null;
        }
    }

    public StorageTest(Class<? extends Storage> storageklass) {
        this.storageklass = storageklass;
    }

    @Test
    public void simpleSetAndGetTest() {
        Storage<Integer> st = mkStorage();
        assert st != null;
        st.foreachNonNull(i -> {
            throw new AssertionError("index "+i+" should not be defined!");
        });

        final Random random = new Random(12345L);
        final int N = 100;
        for (int i=0; i<N; ++i) {
            st = st.set(i, i);
        }
        for (int i=0; i<N; ++i) {
            assert(st.has(i));
            assertThat(st.get(i), is(i));
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
    public void forEachSuccessorRepTest() {
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
        st = st.moveSubtree(3, GenericShape.mkStar(2,1), 1);

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
        System.out.println(st);
        st = st.set(0, 0);
        st = st.set(1, 1);
        st = st.set(3, 2);
        st = st.set(7, 3);
        st = st.set(8, 4);

        st = st.moveSubtree(1, GenericShape.mkStar(2,1), 2);
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
    public void moveStorageSize() {
        Storage<Integer> st = mkStorage();
        assert(st != null);
        st = st.set(0, 100);
        assertThat(st.sizePrecise(), is(1));
        st = st.moveSubtree(0, GenericShape.mkStar(1,1), 1);
        assertThat(st.sizePrecise(), is(2));
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
        st.foreachNonNull(i -> System.out.println(i+" -> "+thSt.get(i)));

        st = st.moveSubtree(1, GenericShape.mkStar(2,1), 3);
        final Storage<Integer> thSt2 = st;
        st.foreachNonNull(i -> System.out.println(i+" -> "+thSt2.get(i)));

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
    public void test() {
        Storage<Integer> storage = this.mkStorage();
        assert(storage != null);
        assertThat(storage, notNullValue());
        for (int i=0; i<=100000; i++) {
            storage = storage.set(i, -i);
        }
        for (int i=0; i<=100000; i++) {
            if (storage.has(i)) {
                assertThat(storage.get(i), is(-i));
            }
        }
    }
}
