package com.clue.test.code

object PureScalaCode {
  def main(args: Array[String]): Unit ={
    val pureScalaCode = new PureScalaCode()
    pureScalaCode.printHelloWorld()
  }
}

class PureScalaCode {
  def printHelloWorld(): Unit ={
    println("Hello World")
  }
}