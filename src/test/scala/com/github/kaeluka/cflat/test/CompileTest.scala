package com.github.kaeluka.cflat.test

import java.nio.file.Paths

import com.github.kaeluka.cflat.ast._
import com.github.kaeluka.cflat.backend._
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.{Assert, BeforeClass, Test}

object CompileTest {
  lazy val backend : BytecodeBackend = new BytecodeBackend
  lazy val shortListInstance : Any = mkInstance("SHORTLIST", Rep("next", 3, None, "ok", None))
  lazy val matr3x3Instance : Any = mkInstance("M3x3", Rep("right", 3, None, "down", Some(Rep("down", 3, None, "ok", None))))
  lazy val boolInstance : Any = mkInstance("BOOL", Alt(("FALSE", None), ("TRUE", None)))

  def mkInstance(name : String, t : TypeSpec) : Any = {
    this.backend.compileProgram(name, t, this.backend.emptyCtx(t, name).withKlassDump()).right.get.getLoaded.newInstance()
  }
}

class CompileTest {
  @Test
  def testBool() {
    assertThat(BytecodeBackendUtil.getPath(CompileTest.boolInstance, "FALSE"), equalTo(0))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.boolInstance, "TRUE"), equalTo(1))
  }

  @Test
  def fooBazFrobBar() {
    val programSpec = Alt(("baz", None), ("frob", None), ("bar", None))
    val instance = CompileTest.backend.compileProgram("FOOBARBAZ", programSpec).right.get.getLoaded.newInstance()
    assert(BytecodeBackendUtil.getPath(instance, "baz") == 0)
    assert(BytecodeBackendUtil.getPath(instance, "frob") == 1)
    assert(BytecodeBackendUtil.getPath(instance, "bar") == 2)
  }

  @Test
  def shortList() {
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "ok",                print = true), equalTo(0))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.ok",           print = true), equalTo(1))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.ok",      print = true), equalTo(2))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.next.ok", print = true), equalTo(3))
  }

  @Test
  def shortListShortAccWorks() {
    BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.next.ok", print = true)
  }

  @Test(expected = classOf[NullPointerException])
  def testShortListLongAccGivesNullPointer() {
    BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.next.next.ok", print = true)
  }

  @Test
  def nestedRep() = {
    for (r <- 0 until 3; d <- 0 until 3) {
      val rights = Stream.continually("right").take(r)
      val downs = Stream.continually("down").take(d)
      val path = (rights ++ Stream("down") ++ downs ++ Stream("ok")).mkString(".")
      assertThat(BytecodeBackendUtil.getPath(CompileTest.matr3x3Instance, path), equalTo(r*3+d))
    }
  }
}