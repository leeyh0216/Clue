package com.clue.core.compiler

import java.io.File
import java.nio.file.Files

import org.junit.rules.TemporaryFolder
import org.junit.{Assert, Test}

class RuntimeScalaCompilerTest {

  @Test(expected = classOf[IllegalArgumentException])
  def testBuilderWithoutSetSource(): Unit ={
    new RuntimeScalaCompiler.Builder().build()
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testBuilderWithoutSetResultDir(): Unit ={
    val tmpFolder = new TemporaryFolder()
    tmpFolder.create()
    val srcDir = tmpFolder.getRoot.getAbsolutePath

    val builder = new RuntimeScalaCompiler.Builder().setSourceDir(srcDir)
    builder.build()
  }

  @Test
  def testBuilderCreateClassDir(): Unit ={
    val tmpFolder1 = new TemporaryFolder()
    tmpFolder1.create()
    val srcDir = tmpFolder1.getRoot.getAbsolutePath

    Files.copy(
      new File("src/test/resources/sources/pure_scala_code/test.scala").toPath,
      new File(srcDir, "test.scala").toPath
    )

    val tmpFolder2 = new TemporaryFolder()
    tmpFolder2.create()
    val resDir = tmpFolder2.getRoot.getAbsolutePath

    val builder = new RuntimeScalaCompiler.Builder().setSourceDir(srcDir).setResultDir(resDir)
    builder.build()

    Assert.assertTrue(new File(resDir, "classes").exists())
  }

  @Test
  def testBuilderCreateCustomClassDir(): Unit ={
    val tmpFolder1 = new TemporaryFolder()
    tmpFolder1.create()
    val srcDir = tmpFolder1.getRoot.getAbsolutePath

    Files.copy(
      new File("src/test/resources/sources/pure_scala_code/test.scala").toPath,
      new File(srcDir, "test.scala").toPath
    )

    val customClassFilePath = "customclasses"
    val tmpFolder2 = new TemporaryFolder()
    tmpFolder2.create()
    val resDir = tmpFolder2.getRoot.getAbsolutePath

    val builder = new RuntimeScalaCompiler
                        .Builder()
                        .setSourceDir(srcDir)
                        .setClassFilePath(customClassFilePath)
                        .setResultDir(resDir)
    builder.build()

    Assert.assertTrue(new File(resDir, customClassFilePath).exists())
  }

  @Test
  def testBuilderCreateJarDir(): Unit ={
    val tmpFolder1 = new TemporaryFolder()
    tmpFolder1.create()
    val srcDir = tmpFolder1.getRoot.getAbsolutePath

    Files.copy(
      new File("src/test/resources/sources/pure_scala_code/test.scala").toPath,
      new File(srcDir, "test.scala").toPath
    )

    val tmpFolder2 = new TemporaryFolder()
    tmpFolder2.create()
    val resDir = tmpFolder2.getRoot.getAbsolutePath

    val builder = new RuntimeScalaCompiler.Builder().setSourceDir(srcDir).setResultDir(resDir)
    builder.build()

    Assert.assertTrue(new File(resDir, "build").exists())
  }

  @Test
  def testBuilderCreateCustomJarDir(): Unit ={
    val tmpFolder1 = new TemporaryFolder()
    tmpFolder1.create()
    val srcDir = tmpFolder1.getRoot.getAbsolutePath

    Files.copy(
      new File("src/test/resources/sources/pure_scala_code/test.scala").toPath,
      new File(srcDir, "test.scala").toPath
    )

    val customJarFilePath = "custombuild"
    val tmpFolder2 = new TemporaryFolder()
    tmpFolder2.create()
    val resDir = tmpFolder2.getRoot.getAbsolutePath

    val builder = new RuntimeScalaCompiler
    .Builder()
      .setSourceDir(srcDir)
      .setClassFilePath(customJarFilePath)
      .setResultDir(resDir)
    builder.build()

    Assert.assertTrue(new File(resDir, customJarFilePath).exists())
  }

  @Test
  def testCompileClasses(): Unit ={
    val srcDir = new File("src/test/resources/sources/pure_scala_code")
    val libsDir = new File("src/test/resources/scalalib")

    val tmpFolder = new TemporaryFolder()
    tmpFolder.create()

    val rsc = new RuntimeScalaCompiler.Builder()
                    .setSourceDir(srcDir.getAbsolutePath)
                    .setResultDir(tmpFolder.getRoot.getAbsolutePath)
                    .setDependenciesDirs(Array(libsDir.toString).toList)
                    .setJarName("test")
                    .build()
    rsc.compile()

    Assert.assertTrue(new File(tmpFolder.getRoot, "build/test.jar").exists())
    Assert.assertTrue(new File(tmpFolder.getRoot, "classes/com/clue/test/code/PureScalaCode.class").exists())
    Assert.assertTrue(new File(tmpFolder.getRoot, "classes/com/clue/test/code/PureScalaCode$.class").exists())
  }
}
