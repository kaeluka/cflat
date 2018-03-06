package com.github.kaeluka.cflat.ast.cflat.backend

import com.github.kaeluka.cflat.ast._
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.DynamicType.Builder
import net.bytebuddy.jar.asm.Opcodes

class IdxClassBackend extends Backend[IdxClassBackendCtx] {
  override def emptyCtx(x : TypeSpec, packge: String, name: String): IdxClassBackendCtx = {
    val kl: (String, Builder[Object]) = (name, BytecodeResult.emptyKl(packge, x, name)._2)
    IdxClassBackendCtx(packge, x, BytecodeResult.empty(packge, x, name), List(), List(), doKlassDump = None, offset = 0)
  }

  override def compile(c: IdxClassBackendCtx, t: TypeSpec): IdxClassBackendCtx = {
    val resToExtend: BytecodeResult = c.resToExtend
    val name = resToExtend.mainKlass._1
    val builder = resToExtend.mainKlass._2
    t match {
      case Rep(n, ethrLoop, ethrAfter) => {
        /*
        { loop } -------+--exitName--> { after }
         ^            |
         |            |
         +--loopName--+
        */
        val idxFieldLimit = ethrLoop match {
          case Right(loop) => (math.pow(loop.getSize.get, n+1) - 1).asInstanceOf[Int]
          case Left(_) => n
        }
        val loopName: String = ethrLoop.left.toOption.getOrElse(s"loop_${t.toString.hashCode.toString.replace("-", "m")}")
        val cPrime = c
          .pushIndex(loopName, Option(n))
          .mapResToExtend(_
            .withIndexField(loopName))

        val cPrimePrime = ethrLoop match {
          case Right(loop) => {
            this.compile(cPrime.pushRecursion(loopName, loop.getSize, name), loop)
          }
          case Left(_) =>
            cPrime.mapResToExtend(_.withMutableRecStep(cPrime, loopName, Some(n)))
        }
        ethrAfter match {
          case Right(after) => {
            val afterComp = compile(
              cPrimePrime,
              after)
            afterComp //.mapResToExtend(_.withSwitchStageStep(cPrimePrime.packge, cPrimePrime.resToExtend.mainKlass._1, "foo", exitName))
          }
          case Left(exitName) => {
            cPrimePrime.mapResToExtend(_.withGetter(cPrime, exitName))
          }
        }
      }

      case Star(inner, after) => {
        this.compile(c, Rep(Integer.MAX_VALUE, inner, after))
      }

      case Alt(lExp, rest) => {
        var curCtx = c
        for (labelled <- lExp :: rest) {
          labelled._2 match {
            case Some(exp) => {
              val currentRes = this.compile(curCtx, exp)
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
                  .defineMethod(s"${labelled._1}_back", BackendUtils.getTypeDesc(curCtx.packge, curCtx.recursionStack.head._3), Opcodes.ACC_PUBLIC)
                  .intercept(ToImpl(MutableBackwardsStep(curCtx, curCtx.recursionStack.head._1, Some(curCtx.recursionStack.head._2.get))))
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

  override def postCompile(ctx: IdxClassBackendCtx): IdxClassBackendCtx = {
    val withCtor = ctx.mapResToExtend(res => {
      res
        .withStepConstructor(List())
    }).withCopyMethod()
    ctx.doKlassDump.foreach(withCtor.resToExtend.writeTo(_, ctx.packge))
    withCtor
  }

  def addIndexField(toModify : DynamicType.Builder[Object], index : (String, Option[Int], String)) : DynamicType.Builder[Object] = {
    //    println(s"adding coordinate field $index")
    toModify.defineField(s"idx_${index._1}", classOf[Int], Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
  }

//  def addIndexFields(toModify : DynamicType.Builder[Object], indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
//        //println(s"adding coordinate fields ${indexStack.mkString("[", ", ", "]")}")
//    indexStack.foldRight(toModify)({case (f, acc) => addIndexField(acc, f)})
//  }

//  def addRecursionMethods(toModify : (String, DynamicType.Builder[Object]))(implicit ctx : IdxClassBackendCtx) : (String, DynamicType.Builder[Object]) = {
//    ctx.indexStack.foldRight(toModify)({case ((name, osz, kl), acc) => {
//      println(s"==> public $kl ${toModify._1}::rec_$name() | recursion method")
//      (acc._1, acc._2
//        .defineMethod(s"rec_$name", BackendUtils.getTypeDesc(ctx.packge, kl), Opcodes.ACC_PUBLIC)
//        .intercept(ExceptionMethod.throwing(classOf[java.lang.UnsupportedOperationException])))
//    }})
//  }

}
