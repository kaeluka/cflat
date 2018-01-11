package com.github.kaeluka.cflat.ast

import scala.collection.mutable.ArrayBuffer

sealed trait TypeSpec {
  def getSize : Option[Int] = {
    println(s"shape(${this}) = ${this.shape().toList}")
    val sz = Util.shapeSize(this.shape())
    println(s"shapeSize(${this}) = ${sz}")
    sz
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
    assert(!Util.isSimpleAlternative(ret))
    assert(Util.isRep(ret))
    assert(!Util.isAlt(ret))
    assert(!Util.isStar(ret))
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
    val ret = Array[Object](new Integer(0), loopShape, afterShape)

    assert(!Util.isSimpleAlternative(ret))
    assert(!Util.isRep(ret))
    assert(!Util.isAlt(ret))
    assert(Util.isStar(ret))
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
//  override def getSize = {
//    (a :: rest).foldRight(Option(0))(
//      {
//        case (t, oacc) => for (acc <- oacc) yield acc + t._2.map(_.getSize).getOrElse(1).asInstanceOf[Int]
//      }
//    )
//  }
  override def orderedChildren = a._2.toList ++ rest.flatMap(_._2.toList)

  override def shape = {
    val ret = (-1 ::
      (a :: rest)
        .map(_._2)
        .map(_.map(_.shape()))
        .map(_.getOrElse(null)))
      .toArray.asInstanceOf[Array[Object]]
    assert(!Util.isSimpleAlternative(ret))
    assert(!Util.isRep(ret))
    assert(Util.isAlt(ret))
    assert(!Util.isStar(ret))
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

object Util {
  def isStar(shape : Array[Object]): Boolean = {
    shape(0).isInstanceOf[Integer] && shape(0).asInstanceOf[Integer] == 0
  }

  def isAlt(shape : Array[Object]): Boolean = {
    shape(0).isInstanceOf[Integer] && shape(0).asInstanceOf[Integer] == -1
  }

  def isRep(shape : Array[Object]): Boolean = {
    shape(0).isInstanceOf[Integer] && shape(0).asInstanceOf[Integer] > 0 && shape.length > 1
  }

  def isSimpleAlternative(shape : Array[Object]): Boolean = {
    shape == null
  }

  def shapeSize(shape : Array[Object]): Option[Int] = {
    if (isSimpleAlternative(shape)) {
      Option(1)
    } else {
      shape(0) match {
        case first: Integer =>
          if (isStar(shape)) {
            None
          } else {
            println(shape(0))
            if (isAlt(shape)) {
              Option(shape.drop(1)
                .map(_.asInstanceOf[Array[Object]])
                .map(Util.shapeSize(_).get)
                .sum)
            } else {
              assert(isRep(shape))
              Option(first * shape.drop(1)
                .map(_.asInstanceOf[Array[Object]])
                .map(Util.shapeSize(_).get)
                .sum)
            }
          }
        case _ =>
          assert(shape(0).isInstanceOf[Array[Object]])
          Option(shape.map(_.asInstanceOf[Array[Object]])
            .map(Util.shapeSize(_).get)
            .sum)
      }
    }
  }

  def nchildren(shape : Array[Object]): Int = {
    if (isSimpleAlternative(shape)) {
      1
    } else if (isAlt(shape)) {
      shape.length - 1
    } else if (isRep(shape)) {
      nchildren(shape(1).asInstanceOf[Array[Object]]) + 1
    } else {
      assert(isStar(shape))
      0

    }

  }
}
