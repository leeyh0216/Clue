package com.anylambda.exceptions

class CompileException(msg: String, e: Exception) extends Exception(msg, e) {
  def this(msg: String) = this(msg, null)
}
