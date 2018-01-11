//package com.github.kaeluka.cflat.ast.cflat.backend
//import java.nio.file.Paths
//
//import com.github.kaeluka.cflat.ast.cflat.ast._
//import net.bytebuddy.description.`type`.{TypeDefinition, TypeDescription}
//import net.bytebuddy.dynamic.DynamicType
//import net.bytebuddy.dynamic.DynamicType.Builder
//import net.bytebuddy.implementation.Implementation.Context
//import net.bytebuddy.implementation._
//import net.bytebuddy.implementation.bytecode.StackManipulation
//import net.bytebuddy.jar.asm.{MethodVisitor, Opcodes}
//
//class ImmutableBackend extends Backend[IdxClassBackendCtx] {
//  override def emptyCtx(x : TypeSpec, name : String) = {
//    val packge = "com.github.kaeluka.cflat.ast.cflat.compiled"
//    val kl: (String, Builder[Object]) = (name, this.addStepConstructor(BytecodeResult.emptyKl(packge, name), List()))
//
//    this.addStaticConstructor(IdxClassBackendCtx(packge, x, BytecodeResult.empty(packge, name), List(), List(), doKlassDump = false, offset = 0))
//  }
//
//  override def compile(c: IdxClassBackendCtx, name : String, t: TypeSpec) : Either[CompilerError, IdxClassBackendCtx] = {
//    val resToExtend: BytecodeResult = c.resToExtend
//    val klName = resToExtend.mainKlass._1
//    val builder = resToExtend.mainKlass._2
//    t match {
//      case Rep(loopName, n, oLoop, exitName, oAfter) => {
//        /*
//        { loop } -------+--exitName--> { after }
//         ^            |
//         |            |
//         +--loopName--+
//        */
//        val idxFieldLimit = oLoop match {
//          case Some(loop) => (math.pow(loop.getSize.get, n+1) - 1).asInstanceOf[Int]
//          case None => n
//        }
//        //        println(s"stage $name: 1")
//        val cPrime = c
//          .pushIndex(loopName, Option(n), name)
//          .mapResToExtend(_
//            .withIndexField(loopName)
//            .withStepConstructor((loopName, Some(idxFieldLimit), name) :: c.indexStack))
//
//        val ethrCtxWithLoop = oLoop match {
//          /**
//            * - when the loop is empty, we add a step along the loop dimension
//            * - when the loop is not empty, we add a recursive step (follow the
//            *   loop impl, then go back)
//            */
//          case Some(loop) => {
//            for (loopComp <- this.compile(cPrime.pushRecursion(loopName, loop.getSize, name), name, loop).right) yield {
//              cPrime.withResToExtend(loopComp.resToExtend)
//            }
//          }
//          case None =>
//            Right(cPrime.mapResToExtend(_.withRecStep(cPrime, loopName)))
//        }
//        oAfter match {
//          case Some(after) => {
//            val emptyRes = BytecodeResult
//              .empty(cPrime.packge, exitName.toUpperCase)
//              .withIndexFields(cPrime.indexStack)
//              .withStepConstructor(cPrime.indexStack)
//            //            println(s"stage $name: 2")
//            for (cPrimePrime <- ethrCtxWithLoop.right;
//                 afterComp <- compile(
//                   cPrimePrime.withResToExtend(emptyRes),
//                   exitName,
//                   after).right) yield {
//              cPrimePrime.mapResToExtend(_.withStep(cPrimePrime, exitName, 1, afterComp.resToExtend))
//            }
//          }
//          case None =>
//            ethrCtxWithLoop.right.map(_.mapResToExtend(_.withGetter(cPrime, exitName)))
//        }
//      }
//
//      case Star(recName, inner, after) => {
//        //        implicit val c2 = c.withStar(name, klName)
//        //        this.compile(c2, name, inner).right.flatMap {
//        //          case bcrs@BytecodeResult(mainKl, privKls, _ /* FIXME: use */) =>
//        //            val ret = addStepMethod(mainKl, inner, name, mainKl)(c2)
//        //            Right(bcrs.withMainKlass(ret))
//        //        }
//        ???
//      }
//
//      case Alt(lExp, rExp, rest @ _*) => {
//        var curCtx = c.withResToExtend(BytecodeResult((klName, builder), List(), List()))
//        for (labelled <- lExp :: rExp :: rest.toList) {
//          labelled._2 match {
//            case Some(exp) => {
//              val currentRes = this.compile(curCtx, name, exp)
//              currentRes match {
//                case Left(_) => return currentRes
//                case Right(bcrs) => {
//                  curCtx = curCtx.mapResToExtend(_.setMainKlass(bcrs.resToExtend.mainKlass))
//                  assert(exp.getSize.isDefined, "infinite size not yet supported")
//                  curCtx = curCtx.withIncreasedOffset(exp.getSize.get)
//                }
//              }
//            }
//            case None => {
//              if (curCtx.recursionStack.nonEmpty) {
//                // There is an open recursion, and this getter should not return
//                // an index, but go back to the starting point of the recursion!
//                curCtx = curCtx.mapMainKlass(_
//                        .defineMethod(labelled._1, BytecodeBackendUtil.getTypeDesc(curCtx.packge, curCtx.recursionStack.head._3), Opcodes.ACC_PUBLIC)
//                        .intercept(ToImpl(CreateStepObj(
//                          calleeClassName = curCtx.recursionStack.head._3,
//                          ctx = curCtx,
//                          stepIdx = Some(curCtx.recursionStack.head._1),
//                          base = curCtx.recursionStack.head._2)))
//                ).withIncreasedOffset(1)
//              } else {
//                curCtx = curCtx
//                  .mapResToExtend(_.withGetter(curCtx, labelled._1))
//                  .withIncreasedOffset(1)
//              }
//            }
//          }
//        }
//        Right(curCtx)
//      }
//    }
//  }
//
//  override def postCompile(bcrs : IdxClassBackendCtx) = {
//    val withCtor = bcrs.mapResToExtend(res => {
//      res.withStepConstructor(List())
//    }).finializeIterators()
//    if (bcrs.doKlassDump) {
//      withCtor.resToExtend.writeToFile(Paths.get(s"/tmp/${bcrs.resToExtend.mainKlass._1}.jar"))
//    }
//    Right(withCtor)
//  }
//
//  def addAllStepConstructors(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
//    var acc = toModify
//    for (i <- indexStack.indices) {
//      acc = (acc._1, addStepConstructor(acc, indexStack.drop(i)))
//    }
//    acc._2
//  }
//
//  def addStepConstructor(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
//    //    println(s"adding ctor for ${toModify._1}, ${indexStack.size} args")
//    val ctorImpl = ToImpl(new StackManipulation {
//      override def apply(mv: MethodVisitor, implCtx: Context) = {
//        //        mv.visitLdcInsn("from addStepConstructor")
//        //        mv.visitInsn(Opcodes.POP)
//        mv.visitVarInsn(Opcodes.ALOAD, 0)
//        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
//        for (idx <- indexStack.zipWithIndex) {
//          mv.visitVarInsn(Opcodes.ALOAD, 0)
//          mv.visitVarInsn(Opcodes.ILOAD, idx._2+1)
//          mv.visitFieldInsn(Opcodes.PUTFIELD, toModify._1, "idx_"+idx._1._1, "I")
//        }
//        mv.visitInsn(Opcodes.RETURN)
//        new StackManipulation.Size(0, if (indexStack.nonEmpty) { 2 } else { 1 })
//      }
//      override def isValid = true
//    })
//    val parameters = new java.util.ArrayList[TypeDefinition]()
//    for (idx <- indexStack) {
//      parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
//    }
//    toModify._2.defineConstructor(Opcodes.ACC_PUBLIC)
//      .withParameters(parameters)
//      .intercept(ctorImpl)
//  }
//
//  def addStaticConstructor(ctx : IdxClassBackendCtx) : IdxClassBackendCtx = {
//    val descr: String = (ctx.packge + "." + ctx.resToExtend.mainKlass._1).replace(".", "/")
//    val clinitManip = new StackManipulation {
//      override def apply(mv: MethodVisitor, implCtx: Context) = {
//        mv.visitTypeInsn(Opcodes.NEW, descr)
//        mv.visitInsn(Opcodes.DUP)
//        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, descr, "<init>", "()V", false)
//        mv.visitFieldInsn(Opcodes.PUTSTATIC, descr, "instance", s"L$descr;")
//        new StackManipulation.Size(0, 2)
//      }
//
//      override def isValid = true
//    }
//    val mkImpl = ToImpl(new StackManipulation {
//      override def apply(mv: MethodVisitor, implCtx: Context) = {
//        mv.visitFieldInsn(Opcodes.GETSTATIC, descr, "instance", s"L$descr;")
//        mv.visitInsn(Opcodes.ARETURN)
//        new StackManipulation.Size(0, 2)
//      }
//      override def isValid = true
//    })
//    ctx.mapMainKlass(_
//      .defineField("instance", BytecodeBackendUtil.getTypeDesc(ctx.packge, ctx.resToExtend.mainKlass._1), Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC)
//      .initializer(ToAppender(clinitManip))
//      .defineMethod("mk", BytecodeBackendUtil.getTypeDesc(ctx.packge, ctx.resToExtend.mainKlass._1), Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
//      .intercept(mkImpl))
//  }
//
//  def addIndexField(toModify : DynamicType.Builder[Object], index : (String, Option[Int], String)) : DynamicType.Builder[Object] = {
//    //    println(s"adding coordinate field $index")
//    toModify.defineField(s"idx_${index._1}", classOf[Int], Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
//  }
//
//  def addIndexFields(toModify : DynamicType.Builder[Object], indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
//    //    println(s"adding coordinate fields ${indexStack.mkString("[", ", ", "]")}")
//    indexStack.foldRight(toModify)({case (f, acc) => addIndexField(acc, f)})
//  }
//
//  def addRecursionMethods(toModify : (String, DynamicType.Builder[Object]))(implicit ctx : IdxClassBackendCtx) : (String, DynamicType.Builder[Object]) = {
//    ctx.indexStack.foldRight(toModify)({case ((name, osz, kl), acc) => {
//      println(s"==> public $kl ${toModify._1}::rec_$name() | recursion method")
//      (acc._1, acc._2
//        .defineMethod(s"rec_$name", BytecodeBackendUtil.getTypeDesc(ctx.packge, kl), Opcodes.ACC_PUBLIC)
//        .intercept(ExceptionMethod.throwing(classOf[java.lang.UnsupportedOperationException])))
//    }})
//  }
//
//}
//
