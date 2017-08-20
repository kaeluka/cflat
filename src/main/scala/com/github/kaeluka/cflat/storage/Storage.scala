package com.github.kaeluka.cflat.storage

import java.util

trait Storage[t] {
  def get(i : Long) : t
  def set(i : Long, x : t) : Storage[t]

  def mapT(f : t => t) : Storage[t]
  def map[u](f : t => u) : Storage[u]
  def foreach(f : t => Unit) : Unit
}

final class ListStorage[t](private val data : util.List[t]) extends Storage[t] {

  override def get(i: Long) = data.get(i.asInstanceOf[Int])

  override def set(i: Long, x: t) = {
    data.set(i.asInstanceOf[Int], x)
    this
  }

  override def mapT(f: (t) => t) = {
    val size: Int = data.size()
    var i = 0
    while (i < size) {
      data.set(i, f(data.get(i)))
      i = i + 1
    }
    this
  }

  override def map[u](f: (t) => u) = {
    val size: Int = data.size()
    val s = new ListStorage[u](new util.ArrayList(size))
    var i = 0
    while (i < size) {
      s.set(i, f(data.get(i)))
      i = i + 1
    }
    s
  }

  override def foreach(f: (t) => Unit) = ???
}

final class MapStorage[t](private val data : util.Map[Long, t]) extends Storage[t] {
  override def get(i: Long) = data.get(i)

  override def set(i: Long, x: t) = {
    data.put(i, x)
    this
  }

  override def mapT(f: (t) => t) = ???

  override def map[u](f: (t) => u) = ???

  override def foreach(f: (t) => Unit) = ???
}
