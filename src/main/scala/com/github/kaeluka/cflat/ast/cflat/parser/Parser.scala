package com.github.kaeluka.cflat.ast.cflat.parser

import com.github.kaeluka.cflat.ast.{Alt, Rep, Star, TypeSpec}
import fastparse.all._
import fastparse.core


/**
  * Parses typespecs.
  *
  * t ::= |(l:t?, l:t? [, (l:t?)*])
  *     | n*l-l->t.
  *     | n*l-l->t:q
  *     | n*q-l->t.
  *     | n*q-l->t:q
  */
case class CflatParseError(text: String, loc: Int) {
  override def toString =
    s"""PARSE ERROR:
       |  $text
       |  ${" "*loc}^
     """.stripMargin
}
object Parser {
  def parse(text: String): Either[CflatParseError, TypeSpec] = {
    val textNoWhitespace = text.filter(!_.isWhitespace)
    val ret = parser.parse(textNoWhitespace)
    ret match {
      case Parsed.Success(v, _) => Right(v)
      case Parsed.Failure(_lastParser, index, _extra) => Left(CflatParseError(textNoWhitespace, index))
    }
  }

  def parser: P[TypeSpec] = parseAlt | parseRep | parseStar

  def parseLabel: P[String] = {
    (CharIn('A' to 'Z') | CharIn('a' to 'z')).rep(sep="").map(_.toString).!
  }

  private val parserOrLabel: P[Either[String, TypeSpec]] = parser.map(Right(_)) | parseLabel.map(Left(_))

  def parseStar: P[Star] = {
    // Star(recName : String, loop : Either[String, TypeSpec], after : TypeSpec) extends TypeSpec {
    P("*("
      ~/ parserOrLabel
//      ~/ "->" ~/ parseLabel
      ~/ ")->" ~/ parserOrLabel).map(v => Star(v._1, v._2))
  }

  def parseRep: P[TypeSpec] = {
    P("*" ~ (CharIn('0' to '9').rep(min = 1).!) ~/ "("
      ~/ parserOrLabel
      ~/ ")->" ~/ parserOrLabel
    ).map(v => Rep(Integer.decode(v._1), v._2, v._3))

  }
  def parseAlt: P[TypeSpec] = {
    val labelAndParser : P[(String, Option[TypeSpec])] = P(parseLabel ~ ((":" ~ parser).?))
    P("|("
      ~/ labelAndParser
      ~/ (","
      ~/ labelAndParser.rep(sep = ",")).?
      ~/ ")").map(v => new Alt((v._1, v._2), v._3.map(_.toList).getOrElse(List())))
  }

}
