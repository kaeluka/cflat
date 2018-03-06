package com.github.kaeluka.cflat.ast

import com.github.kaeluka.cflat.traversal.{GenericShape}

import scala.collection.mutable.ArrayBuffer

sealed trait TypeSpec {
  def getSize : Option[Int] = {
    val sz = GenericShape.shapeSize(this.shape())
    if (sz<0) {
      None
    } else {
      Option(sz)
    }
  }

  def orderedChildren : List[TypeSpec]

  final def dfs() : Seq[TypeSpec] = {
    val res = new ArrayBuffer[TypeSpec]()
    for (child <- this.orderedChildren) {
      res.append(child)
      res.appendAll(child.dfs())
    }
    res
  }

  final def bfs() : Seq[TypeSpec] = {
    val res = new ArrayBuffer[TypeSpec]()
    res.appendAll(this.orderedChildren)
    for (child <- this.orderedChildren) {
      res.appendAll(child.orderedChildren)
    }
    res
  }

  def shape() : Array[Object]

  def pretty(): String

  def contains(t : TypeSpec) : Boolean = (this :: this.dfs().toList).contains(t)
}

case class Rep(n : Int, loop : Either[String, TypeSpec], after : Either[String, TypeSpec]) extends TypeSpec {

  override def orderedChildren = loop.right.toOption.toList ++ after.right.toSeq

  override def getSize = {
    Some(n * loop.right.toOption.flatMap(_.getSize).getOrElse(1) * after.right.toOption.flatMap(_.getSize).getOrElse(1))
  }

  override def shape = {
//    val loopShape = this.loop.right.map(_.shape()).right.getOrElse(() => 1)
    val loopShape = this.loop match {
      case Left(_) => new Integer(1)
      case Right(subexpr) => subexpr.shape()
    }
    val afterShape = this.after match {
      case Left(_) => new Integer(1)
      case Right(subexpr) => subexpr.shape()
    }
    val ret = Array[Object](new Integer(n), loopShape, afterShape)
    assert(!GenericShape.isSimpleAlternative(ret))
    assert(GenericShape.isRep(ret))
    assert(!GenericShape.isAlt(ret))
    assert(!GenericShape.isStar(ret))
    ret
  }

  override def pretty(): String = {
    val prettyLoop = loop match {
      case Left(label) => label
      case Right(subexpr) => subexpr.pretty()
    }
    val prettyAfter = after match {
      case Left(label) => label
      case Right(subexpr) => subexpr.pretty()
    }
    s"*${n}(${prettyLoop})->${prettyAfter}"
  }
}

case class Star(loop : Either[String, TypeSpec], after : Either[String, TypeSpec]) extends TypeSpec {
  override def getSize = None

  override def orderedChildren = loop.right.toOption.toList ++ after.right.toSeq

  override def shape = {
    val loopShape = this.loop match {
      case Left(_) => new Integer(1)
      case Right(subexpr) => subexpr.shape()
    }
    val afterShape = this.after match {
      case Left(_) => new Integer(1)
      case Right(subexpr) => subexpr.shape()
    }
    val ret = GenericShape.mkStar(loopShape, afterShape)

    assert(!GenericShape.isSimpleAlternative(ret))
    assert(!GenericShape.isRep(ret))
    assert(!GenericShape.isAlt(ret))
    assert(GenericShape.isStar(ret))
    ret
  }

  override def pretty(): String = {
    val prettyLoop = loop match {
      case Left(label) => label
      case Right(subexpr) => subexpr.pretty()
    }
    val prettyAfter = after match {
      case Left(label) => label
      case Right(subexpr) => subexpr.pretty()
    }
    s"*(${prettyLoop})->${prettyAfter}"
  }
}

object Alt {
  def apply(a : (String, Option[TypeSpec]), rest : (String, Option[TypeSpec])*) = {
    new Alt(a, rest.toList)
  }
}

case class Alt(a : (String, Option[TypeSpec]), rest : List[(String, Option[TypeSpec])]) extends TypeSpec {
  override def getSize = {
    (a :: rest).foldRight(Option(0))(
      {
        case (t, oacc) => for (acc <- oacc) yield acc + t._2.map(_.getSize).getOrElse(1).asInstanceOf[Int]
      }
    )
  }
  override def orderedChildren = a._2.toList ++ rest.flatMap(_._2.toList)

  override def shape = {
    val ret = (-1 ::
      (a :: rest)
        .map(_._2)
        .map(_.map(_.shape()))
        .map(_.getOrElse(null)))
      .toArray.asInstanceOf[Array[Object]]
    assert(!GenericShape.isSimpleAlternative(ret))
    assert(!GenericShape.isRep(ret))
    assert(GenericShape.isAlt(ret))
    assert(!GenericShape.isStar(ret))
    ret
  }

  override def pretty(): String = {
    def altToPretty(alt: (String, Option[TypeSpec])): String = {
      alt._2 match {
        case None => alt._1
        case Some(subexpr) => s"${alt._1}:${subexpr.pretty()}"
      }
    }
    s"|(${(a :: rest).map(altToPretty).mkString(",")})"
  }
}


