package com.github.kaeluka.cflat.test

import java.nio.file.Paths

import com.github.kaeluka.cflat.ast._
import com.github.kaeluka.cflat.backend._
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.{Assert, BeforeClass, Test}

object CompileTest {
  var backend : BytecodeBackend = _
  var shortListInstance : Any = _
  var matr3x3Instance : Any = _

  @BeforeClass
  def init() {
    this.backend = new BytecodeBackend
    val shortListSpec: Rep = Rep("next", 3, Eps(), "ok", Value("get"))
    this.shortListInstance = this.backend.compileProgram("SHORTLIST", shortListSpec, this.backend.emptyCtx(shortListSpec, "SHORTLIST").withKlassDump()).right.get.getLoaded.newInstance()
    val matx3x3Spec: Rep = Rep("right", 3, Eps(), "godown", Rep("down", 3, Eps(), "m3x3ok", Value("get")))
    this.matr3x3Instance = this.backend.compileProgram("M3x3", matx3x3Spec, this.backend.emptyCtx(matx3x3Spec, "M3x3").withKlassDump()).right.get.getLoaded.newInstance()
  }
}

class CompileTest {
  @Test
  def testBool() {
    val boolSpec = Alt(Value("FALSE"), Value("TRUE"))
    val instance = CompileTest.backend.compileProgram("BOOL", boolSpec).right.get.getLoaded.newInstance()
    assertThat(BytecodeBackendUtil.getPath(instance, "FALSE"), equalTo(0))
    assertThat(BytecodeBackendUtil.getPath(instance, "TRUE"), equalTo(1))
  }

  @Test
  def testFooBazFrobBar() {
    val programSpec = Alt(Alt(Value("baz"), Value("frob")), Value("bar"))
    val instance = CompileTest.backend.compileProgram("FOOBARBAZ", programSpec).right.get.getLoaded.newInstance()
    assert(BytecodeBackendUtil.getPath(instance, "baz") == 0)
    assert(BytecodeBackendUtil.getPath(instance, "frob") == 1)
    assert(BytecodeBackendUtil.getPath(instance, "bar") == 2)
  }

  @Test
  def testShortList() {
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "ok.get",                print = true), equalTo(0))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.ok.get",           print = true), equalTo(1))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.ok.get",      print = true), equalTo(2))
    assertThat(BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.next.ok.get", print = true), equalTo(3))
  }

  @Test(expected = classOf[NullPointerException])
  def testShortListNullPointer() {
    BytecodeBackendUtil.getPath(CompileTest.shortListInstance, "next.next.next.next.ok.get", print = true)
  }

  @Test
  def testNestedRep() = {
    for (r <- 0 until 3; d <- 0 until 3) {
      val rights = Stream.continually("right").take(r)
      val downs = Stream.continually("down").take(d)
      val path = (rights ++ Stream("godown") ++ downs ++ Stream("m3x3ok", "get")).mkString(".")
      println(s"path=$path")
      assertThat(BytecodeBackendUtil.getPath(CompileTest.matr3x3Instance, path), equalTo(r*3+d))
    }
  }
}