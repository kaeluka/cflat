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

case class BytecodeBackendCtx(packge: String, wholeSpec: TypeSpec, resToExtend: BytecodeResult, indexStack: List[(String, Option[Int], String)], recursionStack: List[(String, Option[Int], String)], doKlassDump: Option[String], offset: Int) {
  def print(subj: String): BytecodeBackendCtx = {
    println(s"########### $subj ###########")
    println(s"main class    : ${this.resToExtend.mainKlass._1}")
    println(s"hidden classes: ${this.resToExtend.privateKlasses.map(_._1).mkString(", ")}")
    println(s"end classes   : ${this.resToExtend.endKlasses.map(_._1).mkString(", ")}")
    this
  }

  val bb: ByteBuddy = new ByteBuddy()
  val treeBranchSizes: ArrayBuffer[Int] = ArrayBuffer()
  var nameIdx: Map[String,Int] = Map()

  def withStar[t](recName: String, kl: String): BytecodeBackendCtx = {
    BytecodeBackendCtx(packge, wholeSpec, resToExtend, (recName, None, kl) :: indexStack, recursionStack, doKlassDump, offset)
  }

  def pushIndex[t](indexName: String, n: Option[Int], kl: String): BytecodeBackendCtx = {
    BytecodeBackendCtx(packge, wholeSpec, resToExtend, (indexName, n, kl) :: indexStack, recursionStack, doKlassDump, offset = 0)
  }

  def pushRecursion[t](recName: String, innerSize: Option[Int], kl: String): BytecodeBackendCtx = {
    BytecodeBackendCtx(packge, wholeSpec, resToExtend, indexStack, (recName, innerSize, kl) :: recursionStack, doKlassDump, offset = 0)
  }

  def withResToExtend(res: BytecodeResult): BytecodeBackendCtx = {
    BytecodeBackendCtx(packge, wholeSpec, res, indexStack, recursionStack, doKlassDump, offset)
  }

  def mapResToExtend(f: BytecodeResult => BytecodeResult): BytecodeBackendCtx = {
    this.withResToExtend(f(this.resToExtend))
  }

  def mapMainKlass(f: Builder[Object] => Builder[Object]): BytecodeBackendCtx = {
    this.mapResToExtend(_.setMainKlass(this.resToExtend.mainKlass._1, f(this.resToExtend.mainKlass._2)))
  }

  def withKlassDump(jarName: String): BytecodeBackendCtx = {
    BytecodeBackendCtx(packge, wholeSpec, resToExtend, indexStack, recursionStack, doKlassDump = Some(jarName), offset)
  }

  def withIncreasedOffset(deltaOffset: Int): BytecodeBackendCtx = {
    //    println(s"offset is now: ${offset+1}")
    BytecodeBackendCtx(packge, wholeSpec, resToExtend, indexStack, recursionStack, doKlassDump, offset + deltaOffset)
  }

  def finializeIterators() = {
    def addIters(builder: Builder[Object]): Builder[Object] = {
      builder
//        .defineMethod("spliterator", new TypeDescription.ForLoadedType(classOf[java.util.Spliterator[_]]))
//        .intercept(ExceptionMethod.throwing(classOf[NoSuchMethodError]))
    }
    this.mapResToExtend(_.mapAllClasses(addIters))
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

  def cantorPairingFunction(k1: Long, k2: Long): Long = {
    val ret = (k1 + k2)*(k1 + k2 + 1)/2+k2
    ret
  }

  def cantorPairingFunctionRev(z: Long): (Long, Long) = {
    val w = Math.floor((Math.sqrt(8*z + 1) - 1)/2).asInstanceOf[Long]
    val t = (w*w + w)/2
    val y = z - t
    val x = w - y
    (x, y)
  }

  def test(): Unit = {
    def testPairingFunction(f: (Long, Long) => Long, frev: Long => (Long, Long)) = {
      for (i <- 0 to 100; j <- 0 to 100) {
        assert(frev(f(i, j)) == (i, j))
      }
    }
    testPairingFunction(cantorPairingFunction, cantorPairingFunctionRev)
  }
  test()
}

object BytecodeResult {
  def emptyKl(packge: String, name: String): (String, Builder[Object]) = {
    (name, new ByteBuddy()
      .subclass(classOf[Object], ConstructorStrategy.Default.NO_CONSTRUCTORS)
      .name(packge+"."+name)
      .modifiers(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL).resolve())
      .defineField("stage", classOf[Int], Opcodes.ACC_PUBLIC))
  }

  def empty(packge: String, name: String): BytecodeResult = {
    BytecodeResult(emptyKl(packge, name), List(), List())
  }
}
case class BytecodeResult(mainKlass: (String, DynamicType.Builder[Object]), privateKlasses: List[(String, DynamicType.Builder[Object])], endKlasses: List[(String, DynamicType.Builder[Object])]) {

  def writeToFile(jar: Path): Unit = {
    val jarFile = jar.toFile
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
    total.toJar(jarFile)
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
  def withMutableStep(packge: String, className: String, stepName: String): BytecodeResult = {
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context) = {
        val classDesc: String = implCtx.getInstrumentedType.asErasure().getName.replace(".", "/")
        mv.visitLdcInsn("from withMutableStep(stepName)")
        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitInsn(Opcodes.DUP)
        mv.visitFieldInsn(Opcodes.GETFIELD, classDesc, s"idx_$stepName", "I")
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitFieldInsn(Opcodes.PUTFIELD, classDesc, s"idx_$stepName", "I")
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitInsn(Opcodes.ARETURN)
        new StackManipulation.Size(0, 3)
      }
      override def isValid = true
    })
    setMainKlass((mainKlass._1, mainKlass._2
      .defineMethod(stepName, BackendUtils.getTypeDesc(packge, className), Opcodes.ACC_PUBLIC)
      .intercept(ctorImpl)))
  }
  def withStepConstructor(indexStack: List[(String, Option[Int], String)]): BytecodeResult = {
    //    println(s"adding ctor for ${mainKlass._1}, ${indexStack.size} args")
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context) = {
        //        mv.visitLdcInsn("from withStepConstructor")
        //        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        var crash: Option[Label] = None
        for (idx <- indexStack.zipWithIndex) {
          if (idx._1._2.isDefined) {
            if (crash.isEmpty) {
              crash = Option(new Label())
            }
            // bounds check:
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
            //FIXME start using exception checks again if possible!
//            mv.visitLabel(lbl)
//            val locals = new Array[Object](1+indexStack.size)
//            locals(0) = implCtx.getInstrumentedType.getName.replace(".", "/")
//            for (i <- 1 to indexStack.size) {
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
        new StackManipulation.Size(0, if (indexStack.nonEmpty) { 2 } else { 1 })
      }
      override def isValid = true
    })
    val parameters = new java.util.ArrayList[TypeDefinition]()
    for (idx <- indexStack) {
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
    //    println(s"adding coordinate fields ${indexStack.mkString("[", ", ", "]")}")
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

  /**
    * Add a recursive step to a result.
    *
    * This means that all end classes will be given a recursion method that
    * leads back to the result's main class.
    * @param name the name of the recursion method
    * @param ctx the context
    * @return the modified result
    */
  def withMutableRecStep(ctx: BytecodeBackendCtx, name: String): BytecodeResult = {
    def addStep(kl: (String, Builder[Object])): Builder[Object] = {
      //      println(s"adding recursive step: ${kl._1} --$name--> ${mainKlass._1}")
      val random_acc_parameters = new util.ArrayList[TypeDefinition](ctx.indexStack.size)
      random_acc_parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
      kl._2
        .defineMethod(name, BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
        .intercept(ToImpl(MutableStep(ctx, stepIdx = name, None)))
        .defineMethod(name+"_nth", BackendUtils.getTypeDesc(ctx.packge, mainKlass._1), Opcodes.ACC_PUBLIC)
        .withParameters(random_acc_parameters)
        .intercept(ToImpl(MutableRandomAccessStep(kl._1, ctx, stepIdx = name)))
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
//  def withRecStep(ctx: BytecodeBackendCtx, name: String): BytecodeResult = {
//    def addStep(kl: (String, Builder[Object])): Builder[Object] = {
//      println(s"adding recursive step: ${kl._1} --$name--> ${mainKlass._1}")
//      val random_acc_parameters = new util.ArrayList[TypeDefinition](ctx.indexStack.size)
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

  def withStepOrGetter(ctx: BytecodeBackendCtx, stepName: String, oTarget: Option[BytecodeResult]): BytecodeResult = {
    oTarget match {
      case None => withGetter(ctx, stepName)
      case Some(target) => this.withStep(ctx, stepName, 1, target)
    }
  }

  def withStep(ctx: BytecodeBackendCtx, stepName: String, delta: Int, target: BytecodeResult): BytecodeResult = {
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

  def withGetter(ctx: BytecodeBackendCtx, stepName: String): BytecodeResult = {
    println(s"adding getter $stepName to klass ${ctx.packge+"."+this.mainKlass._1}")
    val withGetter = this.mainKlass._2
      .defineMethod(stepName, classOf[Int], Opcodes.ACC_PUBLIC)
      .intercept(ToImpl(new StackManipulation {
        val offset = ctx.offset

        override def apply(mv: MethodVisitor, implCtx: Context) = {
          val calleeDesc = implCtx.getInstrumentedType.getName.replace(".", "/")//mainKlass._1
          mv.visitLdcInsn(s"from withGetter (mainKlass=$calleeDesc)")
          mv.visitInsn(Opcodes.POP)
          mv.visitInsn(Opcodes.ICONST_0)
          var scale = 1
          for (idx <- ctx.indexStack) {
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitFieldInsn(Opcodes.GETFIELD, calleeDesc, s"idx_${idx._1}", "I")
            assert(idx._2.isDefined, "infinite dimension case not yet implemented")
            mv.visitLdcInsn(scale)
            scale = scale * idx._2.get
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

private case class MutableRandomAccessStep(calleeClassName: String, ctx: BytecodeBackendCtx, stepIdx: String, info: String = "") extends  StackManipulation {
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
    mv.visitVarInsn(Opcodes.ALOAD, 0)
    mv.visitInsn(Opcodes.ARETURN)
    new StackManipulation.Size(0, 3)
  }
}

//private case class CreateRandomAccessStepObj(calleeClassName: String, ctx: BytecodeBackendCtx, stepIdx: Option[String], info: String = "") extends  StackManipulation {
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
//    for (i <- ctx.indexStack.indices) {
//      val idx = ctx.indexStack(i)
//      mv.visitVarInsn(Opcodes.ALOAD, 0)
//      mv.visitFieldInsn(Opcodes.GETFIELD, callerClassDesc.replace(".","/"), s"idx_${idx._1}", "I")
//      if (stepIdx.contains(idx._1)) {
//        mv.visitVarInsn(Opcodes.ILOAD, 1)
//        mv.visitInsn(Opcodes.IADD)
//      }
//    }
//    val ctorDescr = s"(${"I"*ctx.indexStack.size})V"
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
private case class CreateStepObj(calleeClassName: String, ctx: BytecodeBackendCtx, stepIdx: Option[String], base: Option[Int], info: String = "") extends  StackManipulation {
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
private case class MutableStep(ctx: BytecodeBackendCtx, stepIdx: String, base: Option[Int]) extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, implementationContext: Context) = {
    val classDesc = implementationContext.getInstrumentedType.asErasure().getCanonicalName.replace(".", "/")
    mv.visitLdcInsn(s"from MutableStep(ctx={...}, stepIdx=$stepIdx)")
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
