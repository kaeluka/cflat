package com.github.kaeluka.cflat.ast.cflat.util

import java.util

import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.description.`type`.TypeDescription.Generic
import net.bytebuddy.description.annotation.AnnotationList

class TypeDescrFix(name: String, modifiers: Int, superClass: TypeDescription.Generic, interfaces: java.util.List[_ <: TypeDescription.Generic]) extends TypeDescription.Latent(name, modifiers, superClass, interfaces) {

  def this(name : String) = this(name, 0, null, new util.LinkedList[Generic]())

  override def getSegmentCount: Int = {
    0
  }

  override def getDeclaringType = null

  override def getDeclaredAnnotations: AnnotationList = {
    new AnnotationList.Empty
  }
}