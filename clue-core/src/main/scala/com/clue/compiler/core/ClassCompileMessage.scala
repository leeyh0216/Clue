package com.anylambda.compiler.core

import java.util.Date

import javax.print.attribute.standard.Severity

case class ClassCompileMessage(level: Severity, msg: String, timeStamp: Date)