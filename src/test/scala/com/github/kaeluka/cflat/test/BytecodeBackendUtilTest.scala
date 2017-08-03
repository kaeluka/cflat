package com.github.kaeluka.cflat.test

import com.github.kaeluka.cflat.ast._
import com.github.kaeluka.cflat.backend.BytecodeBackendUtil
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.{Assert, Test}

class BytecodeBackendUtilTest {
  def foo_or_bar = Alt(Value("foo"), Value("bar"))
  def foo_or_bar_or_baz = Alt(Value("foo"), Value("bar"), Value("baz"))

  def ten_times_eps = Rep("col", 10, Value("col"), "ok", Value("ok"))

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
    testPairingFunction(BytecodeBackendUtil.cantorPairingFunction, BytecodeBackendUtil.cantorPairingFunctionRev)
  }
}