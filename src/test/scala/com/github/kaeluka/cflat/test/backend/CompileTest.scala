package com.github.kaeluka.cflat.test.backend

import java.util

import com.github.kaeluka.cflat.ast._
import com.github.kaeluka.cflat.backend._
import com.github.kaeluka.cflat.storage.{ListStorage, Storage}
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[Parameterized])
class CompileTest(backend : Backend[BytecodeBackendCtx]) {
  val backendNo = {
    CompileTest.cnt = CompileTest.cnt + 1
    CompileTest.cnt
  }
  lazy val shortListClass = compileSpec("SHORTLIST", Rep("next", 3000, None, "ok", None))
  lazy val matr3x3Class  = compileSpec("M3x3", Rep("right", 3, None, "turndown", Some(Rep("down", 3, None, "ok", None))))
  lazy val matr300x300Class  = compileSpec("M300x300", Rep("right", 300, None, "turn", Some(Rep("down", 300, None, "ok", None))))
  lazy val boolClass  = compileSpec("BOOL", Alt(("FALSE", None), ("TRUE", None)))
  lazy val smallTreeClass  = compileSpec("SMALLTREE", Rep("child", 8, Some(Alt(("l", None), ("r", None))), "ok", None))

  def compileSpec(name : String, t : TypeSpec) : Class[_] = {
    val namePrime: String = s"${name}${backend.getClass.getSimpleName.toUpperCase}"
    println(s"compiling spec into $namePrime")
    try {
      this.backend.compileProgram(namePrime, t, this.backend.emptyCtx(t, namePrime).withKlassDump(namePrime)).resToExtend.getLoaded
    } catch {
      case e: IllegalStateException => {
        this.getClass.getClassLoader.loadClass("com.github.kaeluka.cflat.compiled."+namePrime)
      }
    }
  }

  def mkInstance(klass : Class[_]) : Any = {
    klass.newInstance()
  }

  @Test
  def testBool() {
    val boolInstance = mkInstance(boolClass)
    assertThat(BackendUtils.getPath(boolInstance, "FALSE"), equalTo(0))
    assertThat(BackendUtils.getPath(boolInstance, "TRUE"), equalTo(1))
  }

  @Test
  def fooBazFrobBar() {
    val programSpec = Alt(("baz", None), ("frob", None), ("bar", None))
    val instance = mkInstance(backend.compileProgram("FOOBARBAZ", programSpec).resToExtend.getLoaded)
    assert(BackendUtils.getPath(instance, "baz") == 0)
    assert(BackendUtils.getPath(instance, "frob") == 1)
    assert(BackendUtils.getPath(instance, "bar") == 2)
  }

  @Test
  def shortList() {
    val shortListInstance = mkInstance(shortListClass)
//    assertThat(BytecodeBackendUtil.getPath(shortListInstance, "ok",                print = true), equalTo(0))
//    assertThat(BytecodeBackendUtil.getPath(shortListInstance, "next.ok",           print = true), equalTo(1))
//    assertThat(BytecodeBackendUtil.getPath(shortListInstance, "next.next.ok",      print = true), equalTo(2))
//    assertThat(BytecodeBackendUtil.getPath(shortListInstance, "next.next.next.ok", print = true), equalTo(3))
  }

  @Test
  def shortListShortAccWorks() {
    val shortListInstance = mkInstance(shortListClass)
    BackendUtils.getPath(shortListInstance, "next.next.next.ok", print = true)
  }

  @Test(expected = classOf[NullPointerException])
  def testShortListLongAccGivesNullPointer() {
    val shortListInstance = mkInstance(shortListClass)
    BackendUtils.getPath(shortListInstance, "next.next.next.next.ok", print = true)
  }

  @Test
  def nestedRep() = {
    for (r <- 0 until 3; d <- 0 until 3) {
      val matr3x3Instance = mkInstance(matr3x3Class)
      val rights = Stream.continually("right").take(r)
      val downs = Stream.continually("down").take(d)
      val path = (rights ++ Stream("turndown") ++ downs ++ Stream("ok")).mkString(".")
      assertThat(s"$path should evaluate to ${r*3+d}", BackendUtils.getPath(matr3x3Instance, path, print = true), equalTo(r*3+d))
      println("=====")
    }
  }

  @Test
  def largeNestedRep() = {
    val matr300x300Instance = mkInstance(matr300x300Class)
    println(matr300x300Instance)
  }

  @Test
  def smallTree() = {
    val buf = ArrayBuffer[Int]()
    val result: Int = BackendUtils.getPath(mkInstance(smallTreeClass), "ok")
    buf += result
      println(s"len=0,  -> ${result}")
    var paths = List[List[Char]](List())
    for (i <- 1 to 8) {
      paths = paths.flatMap(p => List(p ++ List('l'), p ++ List('r')))
      for (p <- paths) {
        val path: String = p.mkString("", ".", ".ok").replace('0', 'l').replace('1', 'r')
        val result = BackendUtils.getPath(mkInstance(smallTreeClass), path)
        println(s"len=${p.length}, $path -> ${result}")
        buf += result
      }
    }
    val noOfValues: Int = math.pow(2, 8+1).asInstanceOf[Int] - 1
    assertThat(buf.toSet.size, equalTo(noOfValues))
    assertThat(buf.min, equalTo(0))
    assertThat(buf.max, equalTo(noOfValues-1))
  }
}

object CompileTest {
  var cnt = 0
  @Parameters
  def backends() : util.Collection[Backend[BytecodeBackendCtx]] = {
    val ret = new util.ArrayList[Backend[BytecodeBackendCtx]]()
//    ret.add(new ImmutableBackend)
    ret.add(new SingleClassBackend)
    ret
  }
}