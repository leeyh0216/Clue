package com.clue.core.dependencies

import java.io.File

import org.eclipse.aether.resolution.DependencyResolutionException
import org.junit.rules.TemporaryFolder
import org.junit.{Assert, Test}

class DependencyDownloaderTest {

  @Test(expected = classOf[IllegalArgumentException])
  def testBuildWithoutLocalRepo(): Unit ={
    new DependencyLibDownloader.Builder().build()
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testBuildWithInvalidLocalRepo(): Unit = {
    val tmpFolder = new TemporaryFolder()
    tmpFolder.create()
    val localRepoDir = tmpFolder.getRoot.getAbsolutePath
    tmpFolder.getRoot.delete()

    new DependencyLibDownloader.Builder().setLocalRepoDir(localRepoDir).build()
  }

  @Test
  def testDownloadDependencies(): Unit ={
    val tmpFolder = new TemporaryFolder()
    tmpFolder.create()
    val localRepoDir = tmpFolder.getRoot.getAbsolutePath

    val downloader = new DependencyLibDownloader.Builder().setLocalRepoDir(localRepoDir).build()
    downloader.downloadArtifactory("org.scala-lang","scala-library","2.12.8")

    Assert.assertTrue(new File(localRepoDir, "org/scala-lang/scala-library/2.12.8/scala-library-2.12.8.jar").exists)
  }

  @Test(expected = classOf[DependencyResolutionException])
  def testBuildWithInvalidDependency(): Unit ={
    val tmpFolder = new TemporaryFolder()
    tmpFolder.create()
    val localRepoDir = tmpFolder.getRoot.getAbsolutePath

    val downloader = new DependencyLibDownloader.Builder().setLocalRepoDir(localRepoDir).build()
    downloader.downloadArtifactory("unknown.group","unknown.name","unknown.version")
  }
}
