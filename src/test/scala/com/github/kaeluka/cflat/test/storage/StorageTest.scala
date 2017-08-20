package com.github.kaeluka.cflat.test.ast

import java.util
import java.util.Spliterator
import java.util.function.{Consumer, Predicate}
import java.util.stream.StreamSupport

import com.github.kaeluka.cflat.storage.{ListStorage, Storage}
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[Parameterized])
class StorageTest(var mkstorage : () => Storage[Int]) extends junit.framework.TestCase {
  @Test
  def test(): Unit = {
//    var storage = mkstorage()
//    assertThat(storage, notNullValue())
//    for (i <- 0 to 1000 by 10) {
//      storage = storage.set(i, i)
//    }
//
//    val buf = ArrayBuffer[Int]()
//    storage.foreach(i => buf += i)
//    assertThat(buf.size, equalTo(101))
//    println(s"x = $storage")
  }
}

object StorageTest {
  @Parameters
  def integers() : util.Collection[() => Storage[Int]] = {
    val ret = new util.ArrayList[() => Storage[Int]]()
    ret.add(() => new ListStorage[Int](new util.ArrayList(1000)))
    ret.add(() => new ListStorage[Int](new util.LinkedList[Int]()))
    ret
  }
}
