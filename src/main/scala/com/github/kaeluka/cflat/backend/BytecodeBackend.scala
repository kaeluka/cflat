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
import net.bytebuddy.jar.asm.{MethodVisitor, Opcodes}

import scala.collection.mutable.ArrayBuffer

class IdxCtr {
  var lastIdx = -1
  def genIdx() : Int = {
    this.lastIdx = this.lastIdx + 1
    this.lastIdx
  }
}

case class BytecodeBackendCtx(wholeSpec : TypeSpec, klToExtend : (String, Builder[Object]), indexStack : List[(String, Option[Int], String)] = List(), doKlassDump : Boolean) {

  val bb: ByteBuddy = new ByteBuddy()
  val treeBranchSizes : ArrayBuffer[Int] = ArrayBuffer()
  var nameIdx : Map[String,Int] = Map()
  var idxCtr = new IdxCtr()

  def genName(hint : String) : String = {
    val idx = this.nameIdx.getOrElse(hint, 0)
    this.nameIdx = this.nameIdx + ((hint, idx + 1))
    if (idx > 0) {
      hint + idx
    } else {
      hint
    }
  }

  def genIndex() : Int = {
    this.idxCtr.genIdx()
  }

  def withStar[t](recName : String, kl : String) : BytecodeBackendCtx = {
    BytecodeBackendCtx(wholeSpec, klToExtend, (recName, None, kl) :: indexStack, doKlassDump)
  }

  def pushIndex[t](recName : String, n : Int, kl : String) : BytecodeBackendCtx = {
    BytecodeBackendCtx(wholeSpec, klToExtend, (recName, Some(n), kl) :: indexStack, doKlassDump)
  }

  def withKlassToExtend(kl : (String, Builder[Object])) : BytecodeBackendCtx = {
    BytecodeBackendCtx(wholeSpec, kl, indexStack, doKlassDump)
  }

  def withKlassDump() : BytecodeBackendCtx = {
    BytecodeBackendCtx(wholeSpec, klToExtend, indexStack, doKlassDump = true)
  }
}

object BytecodeBackendUtil {

  def getTypeDesc(kl : String) : TypeDescription = {
    new TypeDescrFix(kl)
  }

  def getPath(instance : Any, path : String, print : Boolean = false) : Int = {
    val segments = path.split("\\.")
    var cur = instance
    for (seg <- segments) {
      val mthd = try {
        cur.getClass.getMethod(seg)
      } catch {
        case e: NoSuchMethodException =>
          throw new RuntimeException(s"available methods were:\n - ${cur.getClass.getDeclaredMethods.mkString("\n - ")}", e)
      }
      if (print) {
        System.err.println(s"cur=$cur / cur.getClass=${cur.getClass.getSimpleName} / calling: $mthd")
        System.err.println(s"methods=${cur.getClass.getDeclaredMethods.mkString(", ")}")
      }
      try {
        cur = cur.getClass.getMethod(seg).invoke(cur)
      } catch {
        case e: InvocationTargetException =>
          throw e.getTargetException
      }
    }
    cur.asInstanceOf[Int]
//    cur.getClass.getMethod("get").invoke(cur).asInstanceOf[Int]
  }

  def loadDynamicClass(kl : (String, DynamicType.Builder[Object])) : Class[_] = {
    try {
      kl._2.make().load(getClass.getClassLoader).getLoaded
    } catch {
      case e : IllegalStateException => {
        println(s"error loading class ${kl._1}: $e")
        getClass.getClassLoader.loadClass(kl._1.toUpperCase)
      }
    }
  }

  def mergeLayerDescs(x : List[Option[Int]], y : List[Option[Int]]) : List[Option[Int]] = {
    val (short, long) = if (x.size < y.size) { (x, y) } else { (y, x) }
    short.zip(long).map({case (oa,ob) =>
      for (a <- oa; b <- ob) yield a + b
    }) ++ long.drop(short.size)
  }

  def cantorPairingFunction(k1 : Long, k2 : Long) : Long = {
    val ret = (k1 + k2)*(k1 + k2 + 1)/2+k2
    ret
  }

  def cantorPairingFunctionRev(z : Long) : (Long, Long) = {
    val w = Math.floor((Math.sqrt(8*z + 1) - 1)/2).asInstanceOf[Long]
    val t = (w*w + w)/2
    val y = z - t
    val x = w - y
    (x, y)
  }

  def test(): Unit = {
    def testPairingFunction(f : (Long, Long) => Long, frev : Long => (Long, Long)) = {
      for (i <- 0 to 100; j <- 0 to 100) {
        assert(frev(f(i, j)) == (i, j))
      }
    }
    testPairingFunction(cantorPairingFunction, cantorPairingFunctionRev)
  }
  test()
}

case class BytecodeResult(mainKlass : (String, DynamicType.Builder[Object]), privateKlasses: List[(String, DynamicType.Builder[Object])], endKlasses: List[(String, DynamicType.Builder[Object])]) {

  def writeToFile(dir: Path): Unit = {
    println(s"writing to file: $this")
    val dirFile = dir.toFile
    assert(!dirFile.exists || dirFile.isDirectory)
    if (!dirFile.exists()) {
      dirFile.mkdirs()
    }
    var total : DynamicType.Unloaded[_] = null

    for (kl <- this.privateKlasses) {
      if (total != null) {
        total = total.include(kl._2.make())
      } else {
        System.err.print(s"trying to make ${kl._1}..")
        total = kl._2.make()
        System.err.println("done")
      }
    }

    if (total != null) {
      total = this.mainKlass._2.make().include(total)
    } else {
      total = this.mainKlass._2.make()
    }
    total.saveIn(dirFile)
  }

  def getLoaded : Class[_] = {
    for (p <- this.privateKlasses.map(_._2)) {
      try {
        val x = p.make.load(this.getClass.getClassLoader).getLoaded
        println(s"loaded ${x.getName}")
      } catch {
        case e : IllegalStateException => System.err.println(s"error loading private class: $e")
      }
    }
    this.mainKlass._2.make.load(this.getClass.getClassLoader).getLoaded
  }

  def withMainKlass(kl : (String, DynamicType.Builder[Object])) : BytecodeResult = {
    BytecodeResult(kl, privateKlasses, endKlasses).withPrivKlass(mainKlass)
  }
  def setMainKlass(kl: (String, Builder[Object])): BytecodeResult = {
    BytecodeResult(kl, privateKlasses, endKlasses)
  }

  def withPrivKlass(kl : (String, DynamicType.Builder[Object])) : BytecodeResult = {
    BytecodeResult(mainKlass, kl :: privateKlasses, endKlasses)
  }
  def withMerged(res : BytecodeResult) : BytecodeResult = {
    res.privateKlasses.foldRight(this.withPrivKlass(res.mainKlass))({
      case (kl, acc) => acc.withPrivKlass(kl)
    })
  }

  def withRecStep(name: String, ctx: BytecodeBackendCtx) : BytecodeResult = {
    def addStep(kl : (String, Builder[Object])) : Builder[Object] = {
      println(s"adding recursive step: ${kl._1} --$name--> ${mainKlass._1}")
      kl._2.defineMethod(name, BytecodeBackendUtil.getTypeDesc(mainKlass._1), Opcodes.ACC_PUBLIC).intercept(ToImpl(CreateStepObj(kl._1, ctx, Some(name))))
    }
    this.endKlasses match {
      case List() => BytecodeResult((mainKlass._1, addStep(mainKlass)), privateKlasses, List())
      case _ => {
        val newEndKlasses = this.endKlasses.map({ kl => (kl._1, addStep(kl)) })
        BytecodeResult(mainKlass, privateKlasses, newEndKlasses)
      }
    }
  }

  def withStep(ctx: BytecodeBackendCtx, stepName: String, target: BytecodeResult) : BytecodeResult = {
    def addStep(kl : (String, Builder[Object])) : Builder[Object] = {
      println(s"adding step: ${kl._1} --$stepName--> ${target.mainKlass._1}")
      kl._2.defineMethod(stepName, BytecodeBackendUtil.getTypeDesc(target.mainKlass._1), Opcodes.ACC_PUBLIC).intercept(ToImpl(CreateStepObj(target.mainKlass._1, ctx, Some(stepName))))
    }
    this.endKlasses match {
      case List() => BytecodeResult((this.mainKlass._1, addStep(this.mainKlass)), this.privateKlasses ++ (target.mainKlass :: target.privateKlasses), target.endKlasses)
      case _ => {
        val boundaryKlasses = this.endKlasses.map({ kl => (kl._1, addStep(kl)) })
        BytecodeResult(this.mainKlass, this.privateKlasses ++ boundaryKlasses ++ (target.mainKlass :: target.privateKlasses), target.endKlasses)
      }
    }
  }

  override def toString: String = {
    s"BytecodeResult($mainKlass, ${privateKlasses.map(_._1).mkString("[", ", ", "]")}, ${endKlasses.map(_._1).mkString("[", ", ", "]")})"
  }
}

class BytecodeBackend extends Backend[BytecodeBackendCtx, BytecodeResult] {
  override def emptyCtx(x : TypeSpec, name : String) = {
    val kl: (String, Builder[Object]) = (name, this.addStepConstructor(this.emptyKl(name), List()))
    BytecodeBackendCtx(x, kl, List(), doKlassDump = false)
  }

  override def compile(implicit c: BytecodeBackendCtx, name : String, t: TypeSpec) : Either[CompilerError, BytecodeResult] = {
    val (klName : String, builder : Builder[Object]) = c.klToExtend
    println(s"compiling $t into class $klName")
    t match {
      case Eps() => {
        println(s"done compiling $t")
        Right(BytecodeResult((klName, builder), List(), List()))
      }
      case Value(tag) => {
        println(s"==> public final class $klName | terminal class | indexStack = ${c.indexStack.mkString(", ")}")
        val parameters = new util.ArrayList[TypeDefinition](c.indexStack.size)
        for (_ <- c.indexStack) {
          parameters.add(new TypeDescription.ForLoadedType(classOf[Int]))
        }
        val ret = builder
          .defineMethod(tag, classOf[Int], Opcodes.ACC_PUBLIC)
          .intercept(ToImpl(new StackManipulation {
            override def apply(mv: MethodVisitor, implementationContext: Context) = {
              mv.visitInsn(Opcodes.ICONST_0)
              var scale = 1
              for (idx <- c.indexStack) {
                mv.visitLdcInsn(s"idx: ${idx._1}")
                mv.visitInsn(Opcodes.POP)
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitFieldInsn(Opcodes.GETFIELD, klName, s"idx_${idx._1}", "I")
                assert(idx._2.isDefined, "infinite dimension case not yet implemented")
                println(s"scaling ${idx._1} by ${scale}x")
                mv.visitLdcInsn(scale)
                scale = scale * idx._2.get
                mv.visitInsn(Opcodes.IMUL)
                mv.visitInsn(Opcodes.IADD)
              }
              mv.visitLdcInsn(c.wholeSpec.bfsIndexOf(t))
              mv.visitInsn(Opcodes.IADD)
              mv.visitInsn(Opcodes.IRETURN)
              new StackManipulation.Size(0, 3)
            }

            override def isValid = true
          }))
        println(s"done compiling $t")
        Right(BytecodeResult((klName, ret), List(), List()))
      }
      case Rep(loopName, n, loop, exitName, after) => {
        /*
        { loop } -------+--exitName--> { after }
           ^            |
           |            |
           +--loopName--+
         */
        println("pushing index..")
        val cPrime = c.pushIndex(loopName, n, name)
        val cPrimePrime = cPrime.withKlassToExtend((name, addStepConstructor((klName, addIndexField(builder, cPrime.indexStack.head)), cPrime.indexStack)))
//        val cPrimePrime = cPrime

        val afterName = exitName.toUpperCase
        println(s"creating after class $afterName..")
        val afterBuilder = addStepConstructor((afterName, addIndexFields(emptyKl(afterName)._2, cPrimePrime.indexStack)), cPrimePrime.indexStack)

        for (innerComp <- this.compile(cPrimePrime, name, loop).right;
             withRec   <- Right(innerComp.withRecStep(loopName, cPrimePrime)).right;
             afterComp <- this.compile(cPrimePrime.withKlassToExtend((afterName, afterBuilder)), afterName, after).right) yield {
          withRec.withStep(cPrimePrime, exitName, afterComp)
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
//                .map(kl => (kl, compile(c, kl.nameHint, kl).right.get))
        var res : BytecodeResult = BytecodeResult((klName, builder), List(), List())
        for (e <- lExp :: rExp :: rest.toList) {
          val currentRes = this.compile(c.withKlassToExtend(res.mainKlass), name, e)
          currentRes match {
            case Left(_) => return currentRes
            case Right(bcrs) => {
              res = res.setMainKlass(bcrs.mainKlass)
            }
          }
        }
        println(s"done compiling $t")
        Right(res)
      }
    }
  }

  override def postCompile(c: BytecodeBackendCtx, bcrs : BytecodeResult) = {
    if (c.doKlassDump) {
      println("dumping classes...")
      bcrs.writeToFile(Paths.get(s"/tmp/${bcrs.mainKlass._1}"))
    }
    Right(bcrs)
  }

  def emptyKl(name : String) : (String, Builder[Object]) = {
    (name, new ByteBuddy()
      .subclass(classOf[Object], ConstructorStrategy.Default.NO_CONSTRUCTORS)
      .name(name)
      .modifiers(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL).resolve()))
  }

  def addAllStepConstructors(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    var acc = toModify
    for (i <- indexStack.indices) {
      acc = (acc._1, addStepConstructor(acc, indexStack.drop(i)))
    }
    acc._2
  }
  def addStepConstructor(toModify : (String, DynamicType.Builder[Object]), indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    println(s"adding ctor for ${toModify._1}, ${indexStack.size} args")
    val ctorImpl = ToImpl(new StackManipulation {
      override def apply(mv: MethodVisitor, implCtx: Context) = {
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
    println(s"adding coordinate field $index")
    toModify.defineField(s"idx_${index._1}", classOf[Int], Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
  }

  def addIndexFields(toModify : DynamicType.Builder[Object], indexStack : List[(String, Option[Int], String)]) : DynamicType.Builder[Object] = {
    println(s"adding coordinate fields ${indexStack.mkString("[", ", ", "]")}")
    indexStack.foldRight(toModify)({case (f, acc) => addIndexField(acc, f)})
  }

  def addRecursionMethods(toModify : (String, DynamicType.Builder[Object]))(implicit ctx : BytecodeBackendCtx) : (String, DynamicType.Builder[Object]) = {
    ctx.indexStack.foldRight(toModify)({case ((name, osz, kl), acc) => {
      println(s"==> public $kl ${toModify._1}::rec_$name() | recursion method")
      (acc._1, acc._2
        .defineMethod(s"rec_$name", BytecodeBackendUtil.getTypeDesc(kl), Opcodes.ACC_PUBLIC)
        .intercept(ExceptionMethod.throwing(classOf[java.lang.UnsupportedOperationException])))
    }})
  }

  /**
    *
    * @param toModify the named class that shall get a step method
    * @param current the type spec that is currently being compiled
    * @param name the name of the step
    * @param target the class that is being reached after the step
    * @param ctx the current context
    * @return the "toModify" class, with the same name, and an added step method
    */
  def addStepMethod(toModify : (String, DynamicType.Builder[Object]),
                    current : TypeSpec,
                    name : String,
                    target : (String, DynamicType.Builder[Object]))(implicit ctx: BytecodeBackendCtx) : (String, DynamicType.Builder[Object]) = {
        val loaded = BytecodeBackendUtil.getTypeDesc(target._1)
        println(s"==> public ${target._1} ${toModify._1}::$name() | step method")
        (toModify._1, toModify._2
          .defineMethod(name, loaded, Opcodes.ACC_PUBLIC)
          .intercept(ToImpl(CreateStepObj(target._1, ctx, Some(name)))))
  }
}

/**
  * Allocates an object of the callee-class type, passing any locally existing coordinates to its constructor.
  * @param calleeClassDesc the class of which an object shall be allocated
  * @param ctx the current compilation context
  * @param stepIdx if present, the index which we are increasing. Needs to be contained in ctx.
  */
case class CreateStepObj(calleeClassDesc : String, ctx : BytecodeBackendCtx, stepIdx : Option[String]) extends  StackManipulation {
  override def isValid = true

  override def apply(mv: MethodVisitor, implementationContext: Context) = {
    var maxStackSize = 5
    val callerClassDesc: String = implementationContext.getInstrumentedType.asErasure().getCanonicalName
    println(s"########### ${this.getClass.getSimpleName}::$callerClassDesc->$calleeClassDesc. indexStack=${ctx.indexStack.mkString("[ ", ",", " ]")}")
    mv.visitTypeInsn(Opcodes.NEW, calleeClassDesc)
    mv.visitInsn(Opcodes.DUP)
    for (i <- ctx.indexStack.indices) {
      val idx = ctx.indexStack(i)
      mv.visitVarInsn(Opcodes.ALOAD, 0)
      mv.visitFieldInsn(Opcodes.GETFIELD, callerClassDesc, s"idx_${idx._1}", "I")
      if (stepIdx.contains(idx._1)) {
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
      }
    }
    val ctorDescr = s"(${"I"*ctx.indexStack.size})V"
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, calleeClassDesc, "<init>", ctorDescr, false)
    mv.visitInsn(Opcodes.ARETURN)
    new StackManipulation.Size(1, maxStackSize)
  }
}

case class ToAppender(stackManipulation: StackManipulation) extends ByteCodeAppender {
  override def apply(methodVisitor: MethodVisitor, implementationContext: Implementation.Context, instrumentedMethod: MethodDescription) = {
    val sz = stackManipulation.apply(methodVisitor, implementationContext)
    new ByteCodeAppender.Size(sz.getMaximalSize, instrumentedMethod.getStackSize)
  }
}

case class ToImpl(s : StackManipulation) extends Implementation {
  override def appender(implementationTarget: Target) = ToAppender(s)

  override def prepare(instrumentedType: InstrumentedType) = instrumentedType
}
