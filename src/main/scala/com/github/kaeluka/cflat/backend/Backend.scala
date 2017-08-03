package com.github.kaeluka.cflat.backend

import com.github.kaeluka.cflat.ast.TypeSpec

sealed trait CompilerError
case class StringError(msg : String) extends CompilerError

trait Backend[ctx, t] {
  def emptyCtx(t : TypeSpec, name : String) : ctx
  def compile(implicit c : ctx, nameHint: String, t : TypeSpec) : Either[CompilerError, t]
  def postCompile(c : ctx, res : t) : Either[CompilerError, t] = { Right(res) }

  def compileProgram(nameHint : String, t : TypeSpec, c : ctx = null.asInstanceOf[ctx]) : Either[CompilerError, t] = {
    var ctx = c
    if (ctx == null) {
      ctx = this.emptyCtx(t, nameHint)
    }
    compile(ctx, nameHint, t) match {
      case Right(res) => postCompile(ctx, res)
      case Left(e) => Left(e)
    }
  }
}
