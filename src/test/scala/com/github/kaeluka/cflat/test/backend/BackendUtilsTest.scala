package com.github.kaeluka.cflat.test.backend

import com.github.kaeluka.cflat.ast.cflat.backend.BackendUtils
import com.github.kaeluka.cflat.ast.{Alt, Rep, TypeSpec}
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.{Assert, Test}

class BackendUtilsTest {
  def foo_or_bar = Alt(("foo", None), ("bar", None))
  def foo_or_bar_or_baz = Alt(("foo", None), ("bar", None), ("baz", None))

  def ten_times_eps = Rep(10, Left("col"), Left("ok"))

  def assertSize(term : TypeSpec, n : Option[Int]) = {
    Assert.assertEquals(s"type spec $term must have size $n", n, term.getSize)
  }

  @Test
  def testPairingFunction() {
    def testPairingFunction(f: (Long, Long) => Long, frev: Long => (Long, Long)) = {
      for (i <- 0L to 100L; j <- 0L to 100L) {
        assertThat(frev(f(i, j)), equalTo((i, j)))
      }
    }
    testPairingFunction(BackendUtils.cantorPairingFunction, BackendUtils.cantorPairingFunctionRev)
  }
}