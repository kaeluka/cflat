package com.github.kaeluka.cflat.test.backend

import java.io.FileWriter
import java.util

import com.github.kaeluka.cflat.ast.cflat.backend._
import com.github.kaeluka.cflat.ast.{Alt, Rep, TypeSpec}
import org.apache.commons.io.output.WriterOutputStream
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.Test
import org.junit.runners.Parameterized.Parameters

import scala.collection.mutable.ArrayBuffer

class CompileTest {
  val packge: String = "com.github.kaeluka.cflat.test.backend"
  val backend = new IdxClassBackend
  lazy val shortListClass = compileSpec("SHORTLIST", Rep(3000, Left("next"), Left("ok")))
  lazy val matr3x3Class  = compileSpec("M3x3", Rep(3, Left("right"), Right(Rep(3, Left("down"), Left("ok")))))
  lazy val matr300x300Class  = compileSpec("M300x300", Rep(300, Left("right"), Right(Rep(300, Left("down"), Left("ok")))))
  lazy val boolClass  = compileSpec("BOOL", Alt(("FALSE", None), ("TRUE", None)))
  lazy val smallTreeClass  = compileSpec("SMALLTREE", Rep(8, Right(Alt(("l", None), ("r", None))), Left("ok")))

  def compileSpec(name : String, t : TypeSpec) : Class[_] = {
    val namePrime: String = name //s"${name}${backend.getClass.getSimpleName.toUpperCase}"
    val os = new WriterOutputStream(new FileWriter("/tmp/"+namePrime))
    println(s"compiling spec into $namePrime")
    try {
      this.backend.compileProgram(packge, namePrime, t, this.backend.emptyCtx(t, packge, namePrime).withKlassDump(os)).resToExtend.getLoaded
    } catch {
      case e: IllegalStateException => {
        this.getClass.getClassLoader.loadClass(packge+"."+namePrime)
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
    val instance = mkInstance(backend.compileProgram(packge, "FOOBARBAZ", programSpec).resToExtend.getLoaded)
    assert(BackendUtils.getPath(instance, "baz") == 0)
    assert(BackendUtils.getPath(instance, "frob") == 1)
    assert(BackendUtils.getPath(instance, "bar") == 2)
  }

  @Test
  def shortList() {
    assertThat(BackendUtils.getPath(mkInstance(shortListClass), "ok",                print = true), equalTo(0))
    assertThat(BackendUtils.getPath(mkInstance(shortListClass), "next.ok",           print = true), equalTo(1))
    assertThat(BackendUtils.getPath(mkInstance(shortListClass), "next.next.ok",      print = true), equalTo(2))
    assertThat(BackendUtils.getPath(mkInstance(shortListClass), "next.next.next.ok", print = true), equalTo(3))
  }

  @Test
  def shortListShortAccWorks() {
    val shortListInstance = mkInstance(shortListClass)
    BackendUtils.getPath(shortListInstance, "next.next.next.ok", print = true)
  }

//  @Test(expected = classOf[NullPointerException])
//  def testShortListLongAccGivesNullPointer() {
//    val shortListInstance = mkInstance(shortListClass)
//    BackendUtils.getPath(shortListInstance, "next.next.next.next.ok", print = true)
//  }

  @Test
  def nestedRep() = {
    for (r <- 0 until 3; d <- 0 until 3) {
      val matr3x3Instance = mkInstance(matr3x3Class)
      val rights = Stream.continually("right").take(r)
      val downs = Stream.continually("down").take(d)
      val path = (rights ++ downs ++ Stream("ok")).mkString(".")
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
