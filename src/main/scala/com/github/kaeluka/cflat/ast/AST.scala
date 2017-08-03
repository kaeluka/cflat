package com.github.kaeluka.cflat.ast

import scala.collection.mutable.ArrayBuffer

sealed trait TypeSpec {
  def getSize : Option[Int]
  def orderedChildren : List[TypeSpec]
//  def nameHint : String

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

  def contains(t : TypeSpec) : Boolean = (this :: this.dfs().toList).contains(t)

  def bfsIndexOf(t : TypeSpec) : Int = {
    if (this == t) {
      0
    } else {
      assert(this.contains(t))
      val childIdx = this.orderedChildren.indexWhere(_.contains(t))
      val (previous, after) = this.orderedChildren.splitAt(childIdx)
      assert(previous.forall(_.getSize.nonEmpty))
      previous.map(_.getSize.get).sum + (if (after.nonEmpty) { after.head.bfsIndexOf(t) } else { 0 })
    }
  }

//  def getLocalDimensionsOf(t : TypeSpec) : Seq[String] = {
//    val ret = new ArrayBuffer[String]()
//    if (this == t) {
//      ret
//    } else {
//      assert(this.contains(t))
//      for (child <- this::this.orderedChildren.takeWhile(!_.contains(t))) {
//        assert(child == this || !child.contains(t))
//        child.getSize match {
//          case Some(1) => ()
//          case _ => ret += child.nameHint
//        }
//      }
//      ret.appendAll(this.orderedChildren.find(_.contains(t)).get.getLocalDimensionsOf(t))
//      ret
//    }
//  }
}

case class Eps() extends TypeSpec {
  override def getSize = Some(0)
  override def orderedChildren = List()
}

case class Value(name : String) extends TypeSpec {
  override def getSize = Some(1)
  override def orderedChildren = List()
}

case class Rep(loopName : String, n : Int, loop : TypeSpec, afterName : String, after : TypeSpec) extends TypeSpec {
  override def getSize = for (i <- loop.getSize) yield n * i
  override def orderedChildren = List(loop, after)
}

case class Star(recName : String, inner : TypeSpec, after : TypeSpec) extends TypeSpec {
  override def getSize = None
  override def orderedChildren = List(inner, after)
}

case class Alt(a : TypeSpec, b : TypeSpec, rest : TypeSpec*) extends TypeSpec {
  override def getSize = {
    (a :: b :: rest.toList).foldRight(Option(0).asInstanceOf[Option[Int]])(
      {
        case (t, oacc) => for (acc <- oacc; s <- t.getSize) yield acc + s
      }
    )
  }
  override def orderedChildren = a :: b :: rest.toList
}

