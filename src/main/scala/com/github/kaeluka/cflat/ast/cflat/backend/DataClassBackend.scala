package com.github.kaeluka.cflat.ast.cflat.backend
import com.github.kaeluka.cflat.ast.{Rep, TypeSpec}

case class DataClassBackendCtx(spec: TypeSpec)

class DataClassBackend extends Backend[DataClassBackendCtx] {
  override def emptyCtx(spec: TypeSpec, packge: String, name: String) = {
    DataClassBackendCtx(spec)
  }

  override def compile(c: DataClassBackendCtx, spec: TypeSpec) = {
//    spec match {
//      case Rep(n, ethrNameOrLoop, afterName, aft)
//    }
    null
  }
}
