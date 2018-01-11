package com.github.kaeluka.cflat.test.storage;

import com.github.kaeluka.cflat.storage.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import org.reflections.Reflections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(Parameterized.class)
public class StorageTest extends junit.framework.TestCase {
    private final Class<? extends Storage> storageklass;

    @Parameterized.Parameters(name="storage: {0}")
    public static Collection<Class<? extends Storage>> storages() {
        final Reflections reflections = new Reflections("com.github.kaeluka.cflat");

        return reflections.getSubTypesOf(Storage.class);
    }

    private Storage mkStorage() {
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
    public void test() {
        Storage storage = this.mkStorage();
        assertThat(storage, notNullValue());
        for (int i=0; i<=1000000; i++) {
            storage = storage.set(i, -i);
        }
        for (int i=0; i<=1000000; i++) {
            assertThat(storage.get(i), is(-i));
        }

//    val buf = ArrayBuffer[Int]()
//    storage.foreach(i => buf += i)
//    assertThat(buf.size, equalTo(101))
//    println(s"x = $storage")
    }
}
//    (var mkstorage : () => ScalaStorage[Int])



//          object ScalaStorageTest {
//@Parameters
//  def integers() : util.Collection[() => ScalaStorage[Int]] = {
//          val ret = new util.ArrayList[() => ScalaStorage[Int]]()
//          ret.add(() => new ListStorage[Int](new util.ArrayList(1000)))
//          ret.add(() => new ListStorage[Int](new util.LinkedList[Int]()))
//          ret
//          }
//}
