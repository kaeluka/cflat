package com.github.kaeluka.cflat.ast.cflat.backend

import com.github.kaeluka.cflat.ast.TypeSpec

trait Backend[ctx] {
  def emptyCtx(spec: TypeSpec, packge: String, name: String): ctx
  def compile(c: ctx, spec: TypeSpec): ctx
  def postCompile(res: ctx): ctx = { res }

  def compileProgram(packge: String, nameHint : String, t : TypeSpec, c : ctx = null.asInstanceOf[ctx]): ctx = {
    var ctx = c
    if (ctx == null) {
      ctx = this.emptyCtx(t, packge, nameHint)
    }
    postCompile(compile(ctx, t))
  }
}
