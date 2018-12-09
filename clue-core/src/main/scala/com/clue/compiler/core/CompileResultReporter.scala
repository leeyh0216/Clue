package com.anylambda.compiler.core

import java.util.Calendar

import com.anylambda.util.Logging

import scala.collection.mutable
import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.Reporter

class CompileResultReporter extends Reporter with Logging{

  var resultMsgs = new mutable.MutableList[ClassCompileMessage]
  var succeed = true

  override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit = {
    logger.info(s"Compile Progress: [${severity.toString()}] ${msg} at ${pos}")
    val javaSeverity =
      if(severity == INFO) javax.print.attribute.standard.Severity.REPORT
      else if(severity == WARNING) javax.print.attribute.standard.Severity.WARNING
      else javax.print.attribute.standard.Severity.ERROR

    resultMsgs +:= ClassCompileMessage(javaSeverity, msg, Calendar.getInstance().getTime)
    val currentResult = if(severity == ERROR) false else true
    succeed = succeed | currentResult
  }

  def getCompileResult: ClassCompileResult = {
    ClassCompileResult(succeed, resultMsgs.toList)
  }




}
