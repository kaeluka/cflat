package com.github.kaeluka.cflat.test.parser

import com.github.kaeluka.cflat.ast.{Alt, Rep, Star, TypeSpec}
import com.github.kaeluka.cflat.ast.cflat.parser.{CflatParseError, Parser}
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.junit.Test

class ParserTest {
  def assertIsRight(text: String, t: TypeSpec) = {
    val parsed: Either[CflatParseError, TypeSpec] = Parser.parse(text)
    if (! parsed.isRight) {
      assert(parsed.isRight,
        s"""$text must parse, but didn't:
           |${parsed.left.get}
         """.stripMargin)
    }
    assertThat(parsed.right.get, equalTo(t))
  }
  def assertIsLeft(text: String) = {
    assert(Parser.parse(text).isLeft, s"$text must not parse")
  }

  @Test
  def parseStar() {
    assertIsRight("*(next)->done", Star(Left("next"), Left("done")))
    assertIsRight(
      """*(right)
        |  ->*(down)->done""".stripMargin,
      Star(Left("right"), Right(
        Star(Left("down"), Left("done")))))

    assertIsRight("*(|(left,right))->done",
      Star(Right(Alt(("left", None), List(("right", None)))), Left("done")))
  }

  @Test
  def parseRep() {
    assertIsRight("*10(next)->done", Rep(10, Left("next"), Left("done")))
    assertIsRight(
      """*10(right)
        |  ->*10(down)->done""".stripMargin,
      Rep(10, Left("right"), Right(
        Rep(10, Left("down"), Left("done")))))

    assertIsRight("*10(|(left,right))->done",
      Rep(10, Right(Alt(("left", None), ("right", None))), Left("done")))
  }

  @Test
  def parseAlt() {
    assertIsRight("|(a)", Alt(("a", None)))
    assertIsRight("|(a,b)", Alt(("a", None), ("b", None)))
    assertIsRight("|(a,b,c)", Alt(("a", None), ("b", None), ("c", None)))
    assertIsRight("|(a,b,c:|(ca,cb))", Alt(("a", None), ("b", None), ("c", Some(Alt(("ca", None), ("cb", None))))))
  }

  @Test
  @throws[Exception]
  def simpleRep() {
    assertThat(Parser.parse(""), notNullValue())
  }
}