package com.github.kaeluka.cflat.ast.cflat.backend

import java.io._
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Files, Paths}
import java.util

import com.github.kaeluka.cflat.ast.TypeSpec
import com.github.kaeluka.cflat.ast.cflat.util.TypeDescrFix
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.Advice.Return
import net.bytebuddy.description.`type`.{TypeDefinition, TypeDescription}
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.{ModifierContributor, TypeManifestation, Visibility}
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.DynamicType.Builder
import net.bytebuddy.dynamic.scaffold.InstrumentedType
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.Implementation.{Context, Target}
import net.bytebuddy.implementation._
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory
import net.bytebuddy.implementation.bytecode.member.{FieldAccess, MethodReturn, MethodVariableAccess}
import net.bytebuddy.implementation.bytecode.{ByteCodeAppender, Duplication, StackManipulation}
import net.bytebuddy.jar.asm.{Label, MethodVisitor, Opcodes}
import net.bytebuddy.matcher.ElementMatchers._
import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

case class IdxClassBackendCtx(packge: String, wholeSpec: TypeSpec, resToExtend: BytecodeResult, indexStack: List[(String, Option[Int])], recursionStack: List[(String, Option[Int], String)], doKlassDump: Option[OutputStream], offset: Int) {
  def print(subj: String): IdxClassBackendCtx = {
    println(s"########### $subj ###########")
    println(s"main class    : ${this.resToExtend.mainKlass._1}")
    println(s"hidden classes: ${this.resToExtend.privateKlasses.map(_._1).mkString(", ")}")
    println(s"end classes   : ${this.resToExtend.endKlasses.map(_._1).mkString(", ")}")
    this
  }

  def withStar[t](recName: String, kl: String): IdxClassBackendCtx = {
    IdxClassBackendCtx(packge, wholeSpec, resToExtend, (recName, None) :: indexStack, recursionStack, doKlassDump, offset)
  }

  def pushStride[t](indexName: String, n: Option[Int]): IdxClassBackendCtx = {
    IdxClassBackendCtx(packge, wholeSpec, resToExtend, (indexName, n) :: indexStack, recursionStack, doKlassDump, offset = 0)
  }

  def pushRecursion[t](recName: String, innerSize: Option[Int], kl: String): IdxClassBackendCtx = {
    IdxClassBackendCtx(packge, wholeSpec, resToExtend, indexStack, (recName, innerSize, kl) :: recursionStack, doKlassDump, offset = 0)
  }

  def withResToExtend(res: BytecodeResult): IdxClassBackendCtx = {
    IdxClassBackendCtx(packge, wholeSpec, res, indexStack, recursionStack, doKlassDump, offset)
  }

  def mapResToExtend(f: BytecodeResult => BytecodeResult): IdxClassBackendCtx = {
    this.withResToExtend(f(this.resToExtend))
  }

  def mapMainKlass(f: Builder[Object] => Builder[Object]): IdxClassBackendCtx = {
    this.mapResToExtend(_.setMainKlass(this.resToExtend.mainKlass._1, f(this.resToExtend.mainKlass._2)))
  }

  def withKlassDump(target: OutputStream): IdxClassBackendCtx = {
    IdxClassBackendCtx(packge, wholeSpec, resToExtend, indexStack, recursionStack, doKlassDump = Some(target), offset)
  }

  def withIncreasedOffset(deltaOffset: Int): IdxClassBackendCtx = {
    //    println(s"offset is now: ${offset+1}")
    IdxClassBackendCtx(packge, wholeSpec, resToExtend, indexStack, recursionStack, doKlassDump, offset + deltaOffset)
  }

  def withCopyMethod(): IdxClassBackendCtx = {
    this.mapMainKlass(builder => {
      builder
        .defineMethod("copy", BackendUtils.getTypeDesc(this.packge, this.resToExtend.mainKlass._1), Opcodes.ACC_PUBLIC)
          .intercept(new Implementation {
            override def appender(implementationTarget: Target): ByteCodeAppender = {
              new ByteCodeAppender {
                override def apply(mv: MethodVisitor, iCtx: Context, instrumentedMethod: MethodDescription): ByteCodeAppender.Size = {
                  val instanceCreateStackManip = new StackManipulation {
                    override def apply(mv: MethodVisitor, iCtx: Context): StackManipulation.Size = {
                      mv.visitTypeInsn(Opcodes.NEW, iCtx.getInstrumentedType.getDescriptor)
                      mv.visitInsn(Opcodes.DUP)
                      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, iCtx.getInstrumentedType.getDescriptor, "<init>", "()V", false)
                      new StackManipulation.Size(1, 2)
                    }
                    override def isValid: Boolean = true
                  }
                  val manips =
                    instanceCreateStackManip ::
                      iCtx.getInstrumentedType.getDeclaredFields.filter(not(isStatic())).flatMap(
                        fld => List(
                          Duplication.SINGLE,
                          MethodVariableAccess.loadThis(),
                          FieldAccess.forField(fld).read(),
                          FieldAccess.forField(fld).write()
                        )
                      ).toList ++ List(MethodReturn.REFERENCE)
                  val size = new StackManipulation.Compound(manips)
                    .apply(mv, iCtx)
                  new ByteCodeAppender.Size(
                    size.getMaximalSize,
                    instrumentedMethod.getStackSize)
                }
              }
            }

            override def prepare(instrumentedType: InstrumentedType): InstrumentedType = instrumentedType
          })
//        .intercept(ExceptionMethod.throwing(classOf[NotImplementedError], "copy method not implemented"))
    })
  }
}

object BackendUtils {

  def getTypeDesc(packge: String, kl: String): TypeDescription = {
    new TypeDescrFix(packge+"."+kl)
  }

  def getPath(instance: Any, path: String, print: Boolean = false): Int = {
    def printObj(obj: Any) = {
      obj.getClass.getDeclaredFields.map(fld => s"${fld.getName}=${fld.get(obj)}").mkString(s"${obj.getClass.getSimpleName}(", ", ", ")")
    }
    val segments = path.split("\\.")
    var cur = instance
    var lastMethdName = "..."
    for (seg <- segments) {
      val mthd = try {
        cur.getClass.getMethod(seg)
      } catch {
        case e: NoSuchMethodException =>
          throw new RuntimeException(s"available methods were:\n - ${cur.getClass.getDeclaredMethods.mkString("\n - ")}", e)
      }
      if (print) {
        println(s"$lastMethdName -> ${printObj(cur)}")
      }
      try {
        lastMethdName = seg
        cur = cur.getClass.getMethod(seg).invoke(cur)
      } catch {
        case e: InvocationTargetException =>
          throw e.getTargetException
      }
    }
    cur.asInstanceOf[Int]
  }

  def loadDynamicClass(kl: (String, DynamicType.Builder[Object])): Class[_] = {
    try {
      kl._2.make().load(getClass.getClassLoader).getLoaded
    } catch {
      case e: IllegalStateException => {
        println(s"error loading class ${kl._1}: $e")
        getClass.getClassLoader.loadClass(kl._1.toUpperCase)
      }
    }
  }

  def mergeLayerDescs(x: List[Option[Int]], y: List[Option[Int]]): List[Option[Int]] = {
    val (short, long) = if (x.size < y.size) { (x, y) } else { (y, x) }
    short.zip(long).map({case (oa,ob) =>
      for (a <- oa; b <- ob) yield a + b
    }) ++ long.drop(short.size)
  }

}

object BytecodeResult {

  def emptyKl(packge: String, x: TypeSpec, name: String): (String, Builder[Object]) = {
    (name, new ByteBuddy()
      .subclass(classOf[Object], ConstructorStrategy.Default.NO_CONSTRUCTORS)
      .name(packge+"."+name)
      .modifiers(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL).resolve())
//      .defineField("stage", classOf[Int], Opcodes.ACC_PUBLIC)
      .defineField("expr", classOf[String], Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
      .value(x.pretty())
      .defineField("shape", classOf[Array[Object]], Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
      .initializer(new ByteCodeAppender {
        def shapeManipulation(shape: Array[Object]): StackManipulation = {
          val inners = shape.toList
            .map(sub => if (sub == null) 1 else sub)
            .map {
            case i: Integer =>
              new StackManipulation {
                override def apply(methodVisitor: MethodVisitor, implementationContext: Context): StackManipulation.Size = {
                  methodVisitor.visitLdcInsn(i)
                  methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                  new StackManipulation.Size(1, 1)
                }
                override def isValid: Boolean = true
              }
            case subshape: Array[Object] => shapeManipulation(subshape)
            case other => throw new RuntimeException(other.toString)
          }
          ArrayFactory.forType(new TypeDescription.ForLoadedType(classOf[Object]).asGenericType()).withValues(inners)
        }
        override def apply(methodVisitor: MethodVisitor, implementationContext: Context, instrumentedMethod: MethodDescription): ByteCodeAppender.Size = {
          val  size: StackManipulation.Size = new StackManipulation.Compound(
            shapeManipulation(x.shape()),
            FieldAccess.forField(implementationContext.getInstrumentedType.getDeclaredFields.filter(named("shape")).getOnly).write()
          ).apply(methodVisitor, implementationContext)
          new ByteCodeAppender.Size(size.getMaximalSize, instrumentedMethod.getStackSize)
        }
      })
    )
  }

  def empty(packge: String, x: TypeSpec, name: String): BytecodeResult = {
    BytecodeResult(emptyKl(packge, x: TypeSpec, name), List(), List())
  }
}
case class BytecodeResult(mainKlass: (String, DynamicType.Builder[Object]), privateKlasses: List[(String, DynamicType.Builder[Object])], endKlasses: List[(String, DynamicType.Builder[Object])]) {

  def writeTo(target: OutputStream, packge: String): Unit = {
//    val jarFile = jar.toFile
    var total: DynamicType.Unloaded[_] = null

    for (kl <- this.privateKlasses) {
      if (total != null) {
        total = total.include(kl._2.make())
      } else {
        total = kl._2.make()
      }
    }

    if (total != null) {
      total = this.mainKlass._2.make().include(total)
    } else {
      total = this.mainKlass._2.make()
    }
    val tmp = Files.createTempDirectory("temp") //;("temp", System.nanoTime().toString())
    println(s"saving into temp file: ${tmp}")
    tmp.toFile.mkdirs()
//    tmp.toFile.deleteOnExit()
    total.saveIn(tmp.toFile)
    val fr = new FileInputStream(Paths.get(tmp.toString, packge.replace(".","/")+"/"+mainKlass._1+".class").toString)
    IOUtils.copy(fr, target)
    target.flush()
    target.close()
    fr.close()
  }

  def getLoaded: Class[_] = {
    for (p <- this.privateKlasses.map(_._2)) {
      try {
        val x = p.make.load(this.getClass.getClassLoader).getLoaded
      } catch {
        case e: IllegalStateException => System.err.println(s"error loading private class: $e")
      }
    }
    this.mainKlass._2.make.load(this.getClass.getClassLoader).getLoaded
  }
  def withSwitchStageStep(packge: String, className: String, currentStage: String, nextStage: String): BytecodeResult = {
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context) = {
        val classDesc: String = implCtx.getInstrumentedType.asErasure().getName.replace(".", "/")
        //FIXME implement:
        mv.visitLdcInsn(s"FIXME: Implement! from withSwitchStageStep($packge, $className, $currentStage, $nextStage)")
        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitInsn(Opcodes.ARETURN)
        new StackManipulation.Size(0, 1)
      }
      override def isValid = true
    })
    setMainKlass((mainKlass._1, mainKlass._2
      .defineMethod(nextStage, BackendUtils.getTypeDesc(packge, className), Opcodes.ACC_PUBLIC)
      .intercept(ctorImpl)))
  }

  def withStepConstructor(coordinates: List[(String, Option[Int], String)]): BytecodeResult = {
    //    println(s"adding ctor for ${mainKlass._1}, ${coordinates.stepSize} args")
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context): StackManipulation.Size = {
        mv.visitLdcInsn(s"from withStepConstructor, coordinates=${coordinates}")
        mv.visitInsn(Opcodes.POP)
        // visit the parameters to generate debug information:
        for (coord <- coordinates) {
          //FIXME: the names seem to be ignored
          mv.visitParameter(coord._1, Opcodes.ACC_FINAL)
        }
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        var crash: Option[Label] = None
        for (idx <- coordinates.zipWithIndex) {
          if (idx._1._2.isDefined) {
            if (crash.isEmpty) {
              crash = Option(new Label())
            }
            //FIXME: bounds check:
//            mv.visitVarInsn(Opcodes.ILOAD, idx._2+1)
//            mv.visitLdcInsn(idx._1._2.get)
//            mv.visitJumpInsn(Opcodes.IF_ICMPGT, crash.get)
          }

          mv.visitVarInsn(Opcodes.ALOAD, 0)
          mv.visitVarInsn(Opcodes.ILOAD, idx._2+1)
          val klassDescr = implCtx.getInstrumentedType.getName.replace(".", "/")
          mv.visitFieldInsn(Opcodes.PUTFIELD, klassDescr, "idx_"+idx._1._1, "I")
        }
        mv.visitInsn(Opcodes.RETURN)
        crash match {
          case Some(lbl) => {
            //FIXME start using bounds checks again if possible!
//            mv.visitLabel(lbl)
//            val locals = new Array[Object](1+coordinates.stepSize)
//            locals(0) = implCtx.getInstrumentedType.getName.replace(".", "/")
//            for (i <- 1 to coordinates.stepSize) {
//              locals(i) = Opcodes.INTEGER
//            }
//            mv.visitFrame(Opcodes.F_FULL, 2, locals, 0, new Array[Object](0))
//            mv.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException")
//            mv.visitInsn(Opcodes.DUP)
//            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V", false)
//            mv.visitInsn(Opcodes.ATHROW)
          }
          case None => ()
        }
        new StackManipulation.Size(0, if (coordinates.nonEmpty) { 2 } else { 1 })
      }
      override def isValid = true
    })
    val parameters = new java.util.ArrayList[TypeDefinition]()
    for (idx <- coordinates) {
      parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
    }

    setMainKlass((mainKlass._1, mainKlass._2.defineConstructor(Opcodes.ACC_PUBLIC)
      .withParameters(parameters)
      .intercept(ctorImpl)))
  }
  def withIndexField(index: String): BytecodeResult = {
    //    println(s"adding coordinate field $index")
    this.setMainKlass((this.mainKlass._1, this.mainKlass._2.defineField(s"idx_${index}", classOf[Int], Opcodes.ACC_PUBLIC)))
  }

  def withIndexFields(indexStack: List[(String, Option[Int], String)]): BytecodeResult = {
    //    println(s"adding coordinate fields ${coordinates.mkString("[", ", ", "]")}")
    indexStack.foldRight(this)({case (f, acc) => acc.withIndexField(f._1)})
  }

  def setMainKlass(kl: (String, Builder[Object])): BytecodeResult = {
    BytecodeResult(kl, privateKlasses, endKlasses)
  }

  def mapAllClasses(f: Builder[Object] => Builder[Object]): BytecodeResult = {
    def fLabelled(labelled: (String, Builder[Object])): (String, Builder[Object]) = {
      (labelled._1, f(labelled._2))
    }
    BytecodeResult((mainKlass._1, f(mainKlass._2)), privateKlasses.map(fLabelled), endKlasses.map(fLabelled))
  }

  def getSizeOfStep(ctx: IdxClassBackendCtx, name: String): Int = {
    ctx.recursionStack.find({ case (sname, _, _) => sname.equals(name) }) match {
      case Some((_, sz, _)) => sz.get
      case None => 1
    }
  }

  /**
    * Add a recursive step to a result.
    *
    * This means that all end classes will be given a recursion method that
    * leads back to the result's main class.
    * @param name the name of the recursion method
    * @param ctx the context
    * @return the modified result
    */
  def withMutableRecStep(ctx: IdxClassBackendCtx, name: String, max: Option[Int]): BytecodeResult = {
    def addStep(kl: (String, Builder[Object])): Builder[Object] = {
      //      println(s"adding recursive step: ${kl._1} --$name--> ${mainKlass._1}")
//      val stepSize = getSizeOfStep(ctx, name)
      val random_acc_parameters = new util.ArrayList[TypeDefinition](ctx.indexStack.size)
      random_acc_parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
      kl._2
        .defineMethod(name, BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
        .intercept(ToImpl(MutableStep(ctx, stepIdx = name, None)))
        .defineMethod(s"${name}_back", BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
        .intercept(ToImpl(MutableBackwardsStep(ctx, stepIdx = name, None)))
        .defineMethod(name+"_nth", BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
        .withParameters(random_acc_parameters)
        .intercept(ToImpl(MutableRandomAccessStep(kl._1, ctx, stepIdx = name, max)))
    }
    this.endKlasses match {
      case List() => BytecodeResult((mainKlass._1, addStep(mainKlass)), privateKlasses, List())
      case _ => {
        val newEndKlasses = this.endKlasses.map({ kl => (kl._1, addStep(kl)) })
        BytecodeResult(mainKlass, privateKlasses, newEndKlasses)
      }
    }
  }
//  /**
//    * Add a recursive step to a result.
//    *
//    * This means that all end classes will be given a recursion method that
//    * leads back to the result's main class.
//    * @param name the name of the recursion method
//    * @param ctx the context
//    * @return the modified result
//    */
//  def withRecStep(ctx: IdxClassBackendCtx, name: String): BytecodeResult = {
//    def addStep(kl: (String, Builder[Object])): Builder[Object] = {
//      println(s"adding recursive step: ${kl._1} --$name--> ${mainKlass._1}")
//      val random_acc_parameters = new util.ArrayList[TypeDefinition](ctx.coordinates.stepSize)
//      random_acc_parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
//      kl._2
//        .defineMethod(name, BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
//        .intercept(ToImpl(CreateStepObj(mainKlass._1, ctx, stepIdx = Some(name), None, "")))
//        .defineMethod(name+"_nth", BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
//        .withParameters(random_acc_parameters)
//        .intercept(ToImpl(CreateRandomAccessStepObj(kl._1, ctx, stepIdx = Some(name))))
//    }
//    this.endKlasses match {
//      case List() => BytecodeResult((mainKlass._1, addStep(mainKlass)), privateKlasses, List())
//      case _ => {
//        val newEndKlasses = this.endKlasses.map({ kl => (kl._1, addStep(kl)) })
//        BytecodeResult(mainKlass, privateKlasses, newEndKlasses)
//      }
//    }
//  }

  def withStepOrGetter(ctx: IdxClassBackendCtx, stepName: String, oTarget: Option[BytecodeResult]): BytecodeResult = {
    oTarget match {
      case None => withGetter(ctx, stepName)
      case Some(target) => this.withStep(ctx, stepName, 1, target)
    }
  }

  def withStep(ctx: IdxClassBackendCtx, stepName: String, delta: Int, target: BytecodeResult): BytecodeResult = {
    def addStep(kl: (String, Builder[Object])): Builder[Object] = {
      //      println(s"adding step: ${kl._1} --$stepName--> ${target.mainKlass._1}")
      kl._2
        .defineMethod(stepName, BackendUtils.getTypeDesc(ctx.packge, target.mainKlass._1), Opcodes.ACC_PUBLIC)
        .intercept(ToImpl(CreateStepObj(target.mainKlass._1, ctx, Some(stepName), None)))
    }
    this.endKlasses match {
      case List() => BytecodeResult((this.mainKlass._1, addStep(this.mainKlass)), this.privateKlasses ++ (target.mainKlass :: target.privateKlasses), target.endKlasses)
      case _ => {
        val boundaryKlasses = this.endKlasses.map({ kl => (kl._1, addStep(kl)) })
        BytecodeResult(this.mainKlass, this.privateKlasses ++ boundaryKlasses ++ (target.mainKlass :: target.privateKlasses), target.endKlasses)
      }
    }
  }

  def withGetter(ctx: IdxClassBackendCtx, stepName: String): BytecodeResult = {
//    println(s"adding getter $stepName to klass ${ctx.packge+"."+this.mainKlass._1}")
    val withGetter = this.mainKlass._2
      .defineMethod(stepName, classOf[Int], Opcodes.ACC_PUBLIC)
      .intercept(ToImpl(new StackManipulation {
        val offset = ctx.offset

        override def apply(mv: MethodVisitor, implCtx: Context) = {
          val calleeDesc = implCtx.getInstrumentedType.getName.replace(".", "/")//mainKlass._1
          mv.visitLdcInsn(s"from withGetter (mainKlass=$calleeDesc, idxStack=${ctx.indexStack})")
          mv.visitInsn(Opcodes.POP)
          mv.visitInsn(Opcodes.ICONST_0)
          var scale = 1
          for (idx <- ctx.indexStack) {
            mv.visitLdcInsn(s"index: ${idx._1}, size=${idx._2}")
            mv.visitInsn(Opcodes.POP)
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitFieldInsn(Opcodes.GETFIELD, calleeDesc, s"idx_${idx._1}", "I")
            assert(idx._2.isDefined, "infinite dimension case not yet implemented")
            scale = scale * idx._2.get
            mv.visitLdcInsn(scale)
            mv.visitInsn(Opcodes.IMUL)
            mv.visitInsn(Opcodes.IADD)
          }
          //          println(s"using offset ${this.offset} for ${mainKlass._1}::$stepName")
          mv.visitLdcInsn(this.offset)
          mv.visitInsn(Opcodes.IADD)
          mv.visitInsn(Opcodes.IRETURN)
          new StackManipulation.Size(0, 3)
        }

        override def isValid = true
      }))
    this.setMainKlass((mainKlass._1, withGetter))
  }

  override def toString: String = {
    s"BytecodeResult($mainKlass, ${privateKlasses.map(_._1).mkString("[", ", ", "]")}, ${endKlasses.map(_._1).mkString("[", ", ", "]")})"
  }
}

private case class MutableRandomAccessStep(calleeClassName: String, ctx: IdxClassBackendCtx, stepIdx: String, max: Option[Int], info: String = "") extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, implCtx: Context) = {
    val classDesc = (ctx.packge+"."+calleeClassName).replace(".", "/")
    mv.visitLdcInsn(s"from MutableRandomAccessStep(classDesc=$classDesc, ctx={...}, stepIdx=$stepIdx, info='$info')")
    mv.visitInsn(Opcodes.POP)
    val callerClassDesc = implCtx.getInstrumentedType.asErasure().getCanonicalName
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.DUP)
    mv.visitFieldInsn(Opcodes.GETFIELD, classDesc, s"idx_$stepIdx", "I")
    mv.visitVarInsn(Opcodes.ILOAD, 1)
    mv.visitInsn(Opcodes.IADD)
    mv.visitFieldInsn(Opcodes.PUTFIELD, classDesc, s"idx_$stepIdx", "I")

    var crash: Option[Label] = None
    max match {
      case Some(m) =>
        crash = Some(new Label())
        //FIXME: continuehere
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, classDesc, s"idx_$stepIdx", "I")
        mv.visitLdcInsn(m-1)
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, crash.get)
      case None => ()
    }
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.ARETURN)
    crash match {
      case Some(lbl) => {
        //FIXME start using exception checks again if possible!
        mv.visitLabel(lbl)
        val locals = new Array[Object](2)
        locals(0) = implCtx.getInstrumentedType.getName.replace(".", "/")
        locals(1) = Opcodes.INTEGER
//        for (i <- 1 to ctx.coordinates.size) {
//          locals(i) = Opcodes.INTEGER
//        }
        mv.visitFrame(Opcodes.F_FULL, 2, locals, 0, new Array[Object](0))
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/AssertionError")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(s"${implCtx.getInstrumentedType.asErasure().getSimpleName}:" +
          s"  -> index ${stepIdx} accessed out of bounds [0..${max.get})")
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V", false)
        mv.visitInsn(Opcodes.ATHROW)
      }
      case None => ()
    }
    new StackManipulation.Size(0, 3)
  }
}

//private case class CreateRandomAccessStepObj(calleeClassName: String, ctx: IdxClassBackendCtx, stepIdx: Option[String], info: String = "") extends  StackManipulation {
//  override def isValid = true
//
//  override def apply(mv: MethodVisitor, implementationContext: Context) = {
//    val calleeClassDesc = (ctx.packge+"."+calleeClassName).replace(".", "/")
//    mv.visitLdcInsn(s"from CreateRandomAccessStepObj(calleeClassDesc=$calleeClassDesc, ctx={...}, stepIdx=$stepIdx, info='$info')")
//    mv.visitInsn(Opcodes.POP)
//    val maxStackSize = 4
//    val callerClassDesc = implementationContext.getInstrumentedType.asErasure().getCanonicalName
//    mv.visitTypeInsn(Opcodes.NEW, calleeClassDesc)
//    mv.visitInsn(Opcodes.DUP)
//    for (i <- ctx.coordinates.indices) {
//      val idx = ctx.coordinates(i)
//      mv.visitVarInsn(Opcodes.ALOAD, 0)
//      mv.visitFieldInsn(Opcodes.GETFIELD, callerClassDesc.replace(".","/"), s"idx_${idx._1}", "I")
//      if (stepIdx.contains(idx._1)) {
//        mv.visitVarInsn(Opcodes.ILOAD, 1)
//        mv.visitInsn(Opcodes.IADD)
//      }
//    }
//    val ctorDescr = s"(${"I"*ctx.coordinates.size})V"
//    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, calleeClassDesc, "<init>", ctorDescr, false)
//    mv.visitInsn(Opcodes.ARETURN)
//    new StackManipulation.Size(1, maxStackSize)
//  }
//}

/**
  * Allocates an object of the callee-class type, passing any locally existing coordinates to its constructor.
  * @param calleeClassName the class name (w/o the package) of which an object shall be allocated
  * @param ctx the current compilation context
  * @param stepIdx if present, the index which we are increasing. Needs to be contained in ctx.
  */
private case class CreateStepObj(calleeClassName: String, ctx: IdxClassBackendCtx, stepIdx: Option[String], base: Option[Int], info: String = "") extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, implementationContext: Context) = {
    val calleeClassDesc = (ctx.packge+"."+calleeClassName).replace(".", "/")
    mv.visitLdcInsn(s"from CreateStepObj(calleeClassDesc=$calleeClassDesc, ctx={...}, stepIdx=$stepIdx, base=$base, info='$info')")
    mv.visitInsn(Opcodes.POP)
    val maxStackSize = 5
    val callerClassDesc = implementationContext.getInstrumentedType.asErasure().getCanonicalName
    mv.visitTypeInsn(Opcodes.NEW, calleeClassDesc)
    mv.visitInsn(Opcodes.DUP)
    for (i <- ctx.indexStack.indices) {
      val idx = ctx.indexStack(i)
      mv.visitVarInsn(Opcodes.ALOAD, 0)
      mv.visitFieldInsn(Opcodes.GETFIELD, callerClassDesc.replace(".","/"), s"idx_${idx._1}", "I")
      base match {
        case Some(b) => {
          mv.visitLdcInsn(b)
          mv.visitInsn(Opcodes.IMUL)
        }
        case None => ()
      }
      if (stepIdx.contains(idx._1)) {
        mv.visitLdcInsn(ctx.offset+1)
        mv.visitInsn(Opcodes.IADD)
      }
    }
    val ctorDescr = s"(${"I"*ctx.indexStack.size})V"
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, calleeClassDesc, "<init>", ctorDescr, false)
    mv.visitInsn(Opcodes.ARETURN)
    new StackManipulation.Size(1, maxStackSize)
  }
}

/**
  * Allocates an object of the callee-class type, passing any locally existing coordinates to its constructor.
  * @param ctx the current compilation context
  * @param stepIdx if present, the index which we are increasing. Needs to be contained in ctx.
  */
private case class MutableStep(ctx: IdxClassBackendCtx, stepIdx: String, base: Option[Int]) extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, iCtx: Context) = {
    val classDesc = iCtx.getInstrumentedType.asErasure().getCanonicalName.replace(".", "/")
    mv.visitLdcInsn(s"from MutableStep(ctx={...}, stepIdx=$stepIdx), base=$base")
    mv.visitInsn(Opcodes.POP)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.DUP)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitFieldInsn(Opcodes.GETFIELD, classDesc, s"idx_$stepIdx", "I")
    base match {
      case Some(b) => {
        mv.visitLdcInsn(b)
        mv.visitInsn(Opcodes.IMUL)
      }
      case None => {}
    }
    mv.visitLdcInsn(ctx.offset)
    mv.visitInsn(Opcodes.IADD)
    mv.visitInsn(Opcodes.ICONST_1)
    mv.visitInsn(Opcodes.IADD)
    mv.visitFieldInsn(Opcodes.PUTFIELD, classDesc, s"idx_$stepIdx", "I")
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.ARETURN)
    new StackManipulation.Size(1, 4)
  }
}
/**
  * Allocates an object of the callee-class type, passing any locally existing coordinates to its constructor.
  * @param ctx the current compilation context
  * @param stepIdx if present, the index which we are increasing. Needs to be contained in ctx.
  */

private case class MutableBackwardsStep(ctx: IdxClassBackendCtx, stepIdx: String, base: Option[Int]) extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, iCtx: Context) = {
    val classDesc = iCtx.getInstrumentedType.getDescriptor
    mv.visitLdcInsn(s"from MutableBackwardsStep(ctx={...}, stepIdx=$stepIdx)")
    mv.visitInsn(Opcodes.POP)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.DUP)
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitFieldInsn(Opcodes.GETFIELD, classDesc, s"idx_$stepIdx", "I")
    mv.visitInsn(Opcodes.ICONST_1)
    mv.visitInsn(Opcodes.ISUB)
    base match {
      case Some(b) => {
        mv.visitLdcInsn(b)
        mv.visitInsn(Opcodes.IDIV)
      }
      case None => {}
    }
    mv.visitFieldInsn(Opcodes.PUTFIELD, classDesc, s"idx_$stepIdx", "I")
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.ARETURN)
    new StackManipulation.Size(1, 4)
  }
}

private case class ToAppender(stackManipulation: StackManipulation) extends ByteCodeAppender {
  override def apply(methodVisitor: MethodVisitor, implementationContext: Implementation.Context, instrumentedMethod: MethodDescription) = {
    val sz = stackManipulation.apply(methodVisitor, implementationContext)
    new ByteCodeAppender.Size(sz.getMaximalSize, instrumentedMethod.getStackSize)
  }
}

private case class ToImpl(s: StackManipulation) extends Implementation {
  override def appender(implementationTarget: Target) = ToAppender(s)

  override def prepare(instrumentedType: InstrumentedType) = instrumentedType
}
