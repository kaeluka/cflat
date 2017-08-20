package com.github.kaeluka.cflat.backend

import com.github.kaeluka.cflat.ast.TypeSpec

trait Backend[ctx] {
  def emptyCtx(spec: TypeSpec, name: String): ctx
  def compile(c: ctx, nameHint: String, spec: TypeSpec): ctx
  def postCompile(res: ctx): ctx = { res }

  def compileProgram(nameHint : String, t : TypeSpec, c : ctx = null.asInstanceOf[ctx]): ctx = {
    var ctx = c
    if (ctx == null) {
      ctx = this.emptyCtx(t, nameHint)
    }
    postCompile(compile(ctx, nameHint, t))
  }
}
