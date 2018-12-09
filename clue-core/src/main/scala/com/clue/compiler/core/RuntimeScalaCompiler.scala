package com.anylambda.compiler.core

import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.util.{Calendar, Timer, UUID}
import java.util.jar.{JarEntry, JarOutputStream}

import com.anylambda.exceptions.{CompileException, JarPackagingException}
import com.anylambda.util.Logging
import javax.print.attribute.standard.Severity

import scala.collection.mutable
import scala.tools.nsc.Global

object RuntimeScalaCompiler {

  class Builder() extends Logging {
    val DEFAULT_CLASS_PATH_SEGMENT = "classes"
    val DEFAULT_JAR_PATH_SEGMENT = "build"

    var srcDir: String = _
    var dependenciesDirs: Option[List[String]] = None
    var resDir: String = _
    var classFilePath = DEFAULT_CLASS_PATH_SEGMENT
    var jarFilePath = DEFAULT_JAR_PATH_SEGMENT
    var jarName = UUID.randomUUID().toString

    def setSourceDir(srcDir: String):Builder = { this.srcDir = srcDir; this }
    def setDependenciesDirs(dependenciesDirs: List[String]):Builder = { this.dependenciesDirs = Some(dependenciesDirs); this }
    def setResultDir(resultDir: String):Builder = { this.resDir = resultDir; this }
    def setClassFilePath(classFilePath: String):Builder = { this.classFilePath = classFilePath; this }
    def setJarFilePath(jarFilePath: String):Builder = { this.jarFilePath = jarFilePath; this }
    def setJarName(jarName: String):Builder = { this.jarName = jarName; this }

    def build(): RuntimeScalaCompiler = {
      //Validate source directory
      require(srcDir != null && srcDir.nonEmpty, "Source directory cannot be null or empty")
      require(new File(srcDir).exists(), s"Source directory does not exists: ${srcDir}")

      //Validate result directory and create
      require(resDir != null && resDir.nonEmpty, "Destination directory cannot be null or empty")
      val resPath = new File(resDir)
      require(resPath.exists() || resPath.mkdirs(), s"Cannot create destination directory: ${resDir}")

      if(dependenciesDirs.isDefined)
        dependenciesDirs.get.foreach(dir => require(new File(dir).exists()))

      //Validate class directory and create
      require(classFilePath != null && classFilePath.nonEmpty, "Class directory cannot be null or empty")
      val resultClassDir = new File(resPath, classFilePath)
      require(resultClassDir.mkdirs(), s"Cannot create class directory: ${resultClassDir.getAbsolutePath}")

      //Validate jar name
      require(jarName != null && jarName.nonEmpty)

      //Validate jar directory and create
      require(jarFilePath != null && jarFilePath.nonEmpty, "Jar directory cannot be null or empty")
      val resultJarDir = new File(resPath, jarFilePath)
      require(resultJarDir.mkdirs(), s"Cannot create jar directory: ${resultJarDir.getAbsolutePath}")

      new RuntimeScalaCompiler(srcDir, dependenciesDirs, resultClassDir.getAbsolutePath, jarName, resultJarDir.getAbsolutePath)
    }
  }
}

/**
  * Runtime Scala Code Compiler.
  * This class compile scala codes with dependencies in runtime.
  *
  * This class cannot instantiated with public constructor.
  *
  * @param srcDir Source file directory(root) to compile.
  * @param dependenciesDirs Dependency jar directories
  * @param classDir Class file output directory
  * @param jarName Jar name to create
  * @param jarDir Jar directory to create
  */
@throws[FileNotFoundException]
@throws[IllegalArgumentException]
class RuntimeScalaCompiler private (srcDir: String, dependenciesDirs: Option[List[String]],
                                    classDir: String, jarName: String, jarDir: String) extends Logging {
  private val compileSettings = new scala.tools.nsc.Settings
  private val compileResultReporter = new CompileResultReporter

  require(srcDir != null || !srcDir.isEmpty, "Source file directory cannot be null or empty")
  require(new File(srcDir).exists(), s"Source file directory does not exists: ${srcDir}")

  private val dependencyLibs = if(dependenciesDirs.isEmpty) List[String]() else dependenciesDirs.get.flatMap(getFilesRecursively(_, ".jar"))
  private val scalaFiles = getScalaFiles(srcDir)
  private val jarFile = new File(jarDir, jarName.concat(".jar"))

  private def getFilesRecursively(dir: String, endsWith: String): List[String] = {
    logger.trace(s"Get files recursively in ${dir} which extension is ${endsWith}")

    val file = new File(dir)
    if(!file.exists())
      throw new FileNotFoundException(s"${file.getAbsolutePath} does not exists.")

    if (file.isFile)
      if (file.getAbsolutePath.endsWith(endsWith))
        List[String](file.getAbsolutePath)
      else
        List[String]()
    else {
      if (file.listFiles().isEmpty)
        List[String]()
      else
        file.listFiles().toList.flatMap(f => getFilesRecursively(f.getAbsolutePath, endsWith))
    }
  }

  /**
    * Get All Scala files in directory recursively
    * @param dir Directory to find scala files
    * @return scala file list
    */
  @throws[FileNotFoundException]
  private def getScalaFiles(sourceRootDir: String): List[String] = {
    logger.trace(s"Get .scala files in source directory: ${sourceRootDir}")

    val scalaFilesToCompile = getFilesRecursively(sourceRootDir, ".scala")
    if(scalaFilesToCompile.isEmpty)
      throw new FileNotFoundException(s"There is no scala file to compile: ${sourceRootDir}")
    else
      scalaFilesToCompile
  }

  @throws[CompileException]
  def compile(): CompileResult ={
    val compileStartTime = System.currentTimeMillis()

    //Compile scala files to class
    val compileClassResult = compileClasses()
    if(!compileClassResult.succeed){
      val lastErrMsg = compileClassResult.msgs.filter(_.level == Severity.ERROR).reduce((m1, m2) => {
        if(m1.timeStamp.before(m2.timeStamp)) m1 else m2
      })
      throw new CompileException(lastErrMsg.msg)
    }

    //Packaging created class files to jar
    addClassesToJar()

    val compileEndTime = System.currentTimeMillis()

    CompileResult(compileEndTime - compileStartTime, jarFile.getAbsolutePath)
  }

  /**
    * Compile scala files to class files
    * @param destPath Destination path to save class files
    * @return Class Compliation Result
    */
  private def compileClasses(): ClassCompileResult ={
    logger.trace(s"Compile ${srcDir} to ${classDir}")

    compileSettings.outputDirs.setSingleOutput(classDir)
    logger.info(s"Compile class output directory has setted: ${classDir}")

    //Add dependency libraries to compile class path
    logger.info(s"Add dependency libraries to compile classpath: ${dependencyLibs.mkString(File.pathSeparator)}")
    compileSettings.classpath.append(dependencyLibs.mkString(File.pathSeparator))

    try {
      logger.info(s"Scala files to compile: ${scalaFiles.mkString(",")}")
      val compiler = new Global(compileSettings, compileResultReporter)
      val compileRunner: compiler.Run  = new compiler.Run
      compileRunner.compile(scalaFiles)
      compileResultReporter.getCompileResult
    }
    catch{
      case e: Exception=>
        val errMsg = ClassCompileMessage(Severity.ERROR, e.getMessage, Calendar.getInstance().getTime)
        ClassCompileResult(false, List[ClassCompileMessage](errMsg))
    }
  }

  private def addClassesToJar(): Unit ={
    var jarOutputStream: JarOutputStream = null
    try {
      jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile.getAbsolutePath))
      logger.info(s"Jar output stream has created. Jar File: ${jarFile.getAbsolutePath}")

      val classFileDir = new File(classDir)
      val classFilePath = classFileDir.toURI.toString
      val fileQueue = new mutable.Queue[File]()

      classFileDir.listFiles().foreach(fileQueue.enqueue(_))
      while (fileQueue.nonEmpty) {
        val nextFile = fileQueue.dequeue()

        val relativeDir = nextFile.toURI.toString.substring(classFilePath.length)
        val jarEntry = new JarEntry(relativeDir)
        jarEntry.setTime(nextFile.lastModified())
        logger.info(s"Add jar entry to jar: ${relativeDir}")
        jarOutputStream.putNextEntry(jarEntry)
        if (nextFile.isDirectory)
          nextFile.listFiles().foreach(fileQueue.enqueue(_))
        else {
          val buf = new Array[Byte](1024)
          val in = new FileInputStream(nextFile)
          Stream.continually(in.read(buf)).takeWhile(_ != -1).foreach(jarOutputStream.write(buf, 0, _))
          in.close()
        }
        jarOutputStream.closeEntry()
      }
    }
    catch{
      case e:Exception =>
        throw new JarPackagingException(s"Error occured during packaging jar: ${new File(jarDir, jarName.concat(".jar"))}", e)
    }
    finally {
      if(jarOutputStream != null)
        jarOutputStream.close()
    }
  }
}
