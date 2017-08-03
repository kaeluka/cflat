package com.github.kaeluka.cflat.test

import com.github.kaeluka.cflat.ast._
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.{Assert, Test}

class TypeSpecTest {
  def foo_or_bar = Alt(Value("foo"), Value("bar"))
  def foo_or_bar_or_baz = Alt(Value("foo"), Value("bar"), Value("baz"))

  def ten_times_eps = Rep("col", 10, Eps(), "bar", Value("get"))

  def assertSize(term : TypeSpec, n : Option[Int]) = {
    Assert.assertEquals(s"type spec $term must have size $n", n, term.getSize)
  }

  @Test
  @throws[Exception]
  def testGetSize() {
    assertSize(foo_or_bar, Some(2))
    assertSize(ten_times_eps, Some(10))
    assertSize(Star("com/github/kaeluka/cflat/ast/test", ten_times_eps, Value("done")), None)
  }

  @Test
  @throws[Exception]
  def testDfsBfs() = {
    assertThat(List(), equalTo(Value("x").bfs()))
    assertThat(List(), equalTo(Value("x").dfs()))
    assertThat(List(Value("foo"), Value("bar")), equalTo(foo_or_bar.bfs()))
    assertThat(List(Value("foo"), Value("bar")), equalTo(foo_or_bar.dfs()))

    val BCD = Alt(Value("c"), Value("d"))
    def ABCDX = Alt(BCD, Value("x"))

    assertThat(List(BCD, Value("x"), Value("c"), Value("d")), equalTo(ABCDX.bfs().toList))
    assertThat(List(BCD, Value("c"), Value("d"), Value("x")), equalTo(ABCDX.dfs().toList))
  }

  @Test
  def testBfsIndexOf() = {
    assertThat(0, equalTo(foo_or_bar.bfsIndexOf(Value("foo"))))
    assertThat(1, equalTo(foo_or_bar.bfsIndexOf(Value("bar"))))

    assertThat(0, equalTo(foo_or_bar_or_baz.bfsIndexOf(Value("foo"))))
    assertThat(1, equalTo(foo_or_bar_or_baz.bfsIndexOf(Value("bar"))))
    assertThat(2, equalTo(foo_or_bar_or_baz.bfsIndexOf(Value("baz"))))

    assertThat(0, equalTo(Alt(foo_or_bar_or_baz, Value("qux")).bfsIndexOf(Value("foo"))))
    assertThat(1, equalTo(Alt(foo_or_bar_or_baz, Value("qux")).bfsIndexOf(Value("bar"))))
    assertThat(2, equalTo(Alt(foo_or_bar_or_baz, Value("qux")).bfsIndexOf(Value("baz"))))
    assertThat(3, equalTo(Alt(foo_or_bar_or_baz, Value("qux")).bfsIndexOf(Value("qux"))))

    val boolSpec = Alt(Value("FALSE"), Value("TRUE"))
    assertThat(0, equalTo(boolSpec.bfsIndexOf(Value("FALSE"))))
    assertThat(1, equalTo(boolSpec.bfsIndexOf(Value("TRUE"))))
  }

//  @Test
//  def testGetLocalDimensions() = {
//    val chunk = Rep("next", 10, Value("ok"), Value("done"))
//    assertThat(chunk.getLocalDimensionsOf(Value("ok")), equalTo[Seq[_]](List[String]("next")))
//
//    assertThat(Star("next", Value("ok")).getLocalDimensionsOf(Value("ok")), equalTo[Seq[_]](List[String]("next")))
//
//    val matr10x10: Rep = Rep("col", 10, Rep("row", 10, Value("get"), Value("here")), Value("there"))
//    assertThat(matr10x10.getLocalDimensionsOf(Rep("row", 10, Value("get"), Value("done"))), equalTo[Seq[_]](List[String]("col")))
//    assertThat(matr10x10.getLocalDimensionsOf(Value("get")), equalTo[Seq[_]](List[String]("col", "row")))
//
//    val chunkedList = Star("nextchunk", chunk)
//    assertThat(chunkedList.getLocalDimensionsOf(chunk), equalTo[Seq[_]](List("nextchunk")))
//  }
}