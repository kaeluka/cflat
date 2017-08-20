package com.github.kaeluka.cflat.backend

import java.lang.reflect.InvocationTargetException
import java.nio.file.{Path, Paths}
import java.util

import com.github.kaeluka.cflat.ast._
import com.github.kaeluka.cflat.util.TypeDescrFix
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.`type`.{TypeDefinition, TypeDescription}
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.{ModifierContributor, TypeManifestation, Visibility}
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.DynamicType.Builder
import net.bytebuddy.dynamic.scaffold.InstrumentedType
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.Implementation.{Context, Target}
import net.bytebuddy.implementation._
import net.bytebuddy.implementation.bytecode.{ByteCodeAppender, StackManipulation}
import net.bytebuddy.jar.asm.{Label, MethodVisitor, Opcodes}

import scala.collection.mutable.ArrayBuffer

class SingleClassBackend extends Backend[BytecodeBackendCtx] {
  override def emptyCtx(x : TypeSpec, name : String) = {
    val packge = "com.github.kaeluka.cflat.compiled"
    val kl: (String, Builder[Object]) = (name, this.addStepConstructor(BytecodeResult.emptyKl(packge, name), List()))
    BytecodeBackendCtx(packge, x, BytecodeResult.empty(packge, name), List(), List(), doKlassDump = None, offset = 0)
  }

  override def compile(c: BytecodeBackendCtx, name: String, t: TypeSpec): BytecodeBackendCtx = {
    val resToExtend: BytecodeResult = c.resToExtend
    val klName = resToExtend.mainKlass._1
    val builder = resToExtend.mainKlass._2
    t match {
      case Rep(loopName, n, oLoop, exitName, oAfter) => {
        /*
        { loop } -------+--exitName--> { after }
         ^            |
         |            |
         +--loopName--+
        */
        val idxFieldLimit = oLoop match {
          case Some(loop) => (math.pow(loop.getSize.get, n+1) - 1).asInstanceOf[Int]
          case None => n
        }
        val cPrime = c
          .pushIndex(loopName, Option(n), name)
          .mapResToExtend(_
            .withIndexField(loopName))
          .print("cPrime")

        val cPrimePrime = oLoop match {
          case Some(loop) => {
            this.compile(cPrime.pushRecursion(loopName, loop.getSize, name), name, loop)
          }
          case None =>
            cPrime.mapResToExtend(_.withMutableRecStep(cPrime, loopName)).print(s"cPrime withResToExtend 2 for $t")
        }
        oAfter match {
          case Some(after) => {
            val afterComp = compile(
              cPrimePrime, //.withResToExtend(emptyRes),
              exitName,
              after)
            afterComp.print(s"cPrimePrime for $t").mapResToExtend(_.withSwitchStageStep(cPrimePrime.packge, cPrimePrime.resToExtend.mainKlass._1, "foo", exitName)).print(s"foo after $t")
          }
          case None => {
            cPrimePrime.mapResToExtend(_.withGetter(cPrime, exitName))
          }
        }
      }

      case Star(recName, inner, after) => {
        //        implicit val c2 = c.withStar(name, klName)
        //        this.compile(c2, name, inner).right.flatMap {
        //          case bcrs@BytecodeResult(mainKl, privKls, _ /* FIXME: use */) =>
        //            val ret = addStepMethod(mainKl, inner, name, mainKl)(c2)
        //            Right(bcrs.withMainKlass(ret))
        //        }
        ???
      }

      case Alt(lExp, rExp, rest @ _*) => {
        var curCtx = c
        for (labelled <- lExp :: rExp :: rest.toList) {
          labelled._2 match {
            case Some(exp) => {
              val currentRes = this.compile(curCtx, name, exp)
              curCtx = curCtx.mapResToExtend(_.setMainKlass(currentRes.resToExtend.mainKlass))
              assert(exp.getSize.isDefined, "infinite size not yet supported")
              curCtx = curCtx.withIncreasedOffset(exp.getSize.get)
            }
            case None => {
              if (curCtx.recursionStack.nonEmpty) {
                // There is an open recursion, and this getter should not return
                // an index, but go back to the starting point of the recursion!
                curCtx = curCtx.mapMainKlass(_
                        .defineMethod(labelled._1, BackendUtils.getTypeDesc(curCtx.packge, curCtx.recursionStack.head._3), Opcodes.ACC_PUBLIC)
                        .intercept(ToImpl(MutableStep(curCtx, curCtx.recursionStack.head._1, Some(curCtx.recursionStack.head._2.get))))
                ).withIncreasedOffset(1)
              } else {
                curCtx = curCtx
                  .mapResToExtend(_.withGetter(curCtx, labelled._1))
                  .withIncreasedOffset(1)
              }
            }
          }
        }
        curCtx.print(s"after $t")
      }
    }
  }

  override def postCompile(bcrs : BytecodeBackendCtx) = {
    val withCtor = bcrs.mapResToExtend(res => {
      res.withStepConstructor(List())
    }).finializeIterators()
    bcrs.doKlassDump match {
      case Some(jarName) =>
        withCtor.resToExtend.writeToFile(Paths.get(s"/tmp/${jarName}.jar"))
      case None => ()
    }
    withCtor
  }

  def addAllStepConstructors(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    var acc = toModify
    for (i <- indexStack.indices) {
      acc = (acc._1, addStepConstructor(acc, indexStack.drop(i)))
    }
    acc._2
  }

  def addStepConstructor(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    //    println(s"adding ctor for ${toModify._1}, ${indexStack.size} args")
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context) = {
        //        mv.visitLdcInsn("from addStepConstructor")
        //        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        for (idx <- indexStack.zipWithIndex) {
          mv.visitVarInsn(Opcodes.ALOAD, 0)
          mv.visitVarInsn(Opcodes.ILOAD, idx._2+1)
          mv.visitFieldInsn(Opcodes.PUTFIELD, toModify._1, "idx_"+idx._1._1, "I")
        }
        mv.visitInsn(Opcodes.RETURN)
        new StackManipulation.Size(0, if (indexStack.nonEmpty) { 2 } else { 1 })
      }
      override def isValid = true
    })
    val parameters = new java.util.ArrayList[TypeDefinition]()
    for (idx <- indexStack) {
      parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
    }
    toModify._2.defineConstructor(Opcodes.ACC_PUBLIC)
      .withParameters(parameters)
      .intercept(ctorImpl)
  }

  def addIndexField(toModify : DynamicType.Builder[Object], index : (String, Option[Int], String)) : DynamicType.Builder[Object] = {
    //    println(s"adding coordinate field $index")
    toModify.defineField(s"idx_${index._1}", classOf[Int], Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
  }

  def addIndexFields(toModify : DynamicType.Builder[Object], indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    //    println(s"adding coordinate fields ${indexStack.mkString("[", ", ", "]")}")
    indexStack.foldRight(toModify)({case (f, acc) => addIndexField(acc, f)})
  }

  def addRecursionMethods(toModify : (String, DynamicType.Builder[Object]))(implicit ctx : BytecodeBackendCtx) : (String, DynamicType.Builder[Object]) = {
    ctx.indexStack.foldRight(toModify)({case ((name, osz, kl), acc) => {
      println(s"==> public $kl ${toModify._1}::rec_$name() | recursion method")
      (acc._1, acc._2
        .defineMethod(s"rec_$name", BackendUtils.getTypeDesc(ctx.packge, kl), Opcodes.ACC_PUBLIC)
        .intercept(ExceptionMethod.throwing(classOf[java.lang.UnsupportedOperationException])))
    }})
  }

}
