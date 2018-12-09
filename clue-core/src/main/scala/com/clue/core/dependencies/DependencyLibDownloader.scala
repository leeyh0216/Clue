package com.clue.core.dependencies

import java.io.File
import java.util

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.resolution.{DependencyRequest, DependencyResolutionException}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession}

import scala.collection.mutable

//https://github.com/eclipse/aether-demo
object DependencyLibDownloader {
  val repositorySystem = MavenRepositorySystemUtils.newServiceLocator()
    .addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    .addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    .addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    .getService(classOf[RepositorySystem])

  private def createNewRemoteRepository(id: String, tp: String, url: String): RemoteRepository = new RemoteRepository.Builder(id, tp, url).build()

  class Builder() {
    var localRepoDir: String = _
    var remoteRepositories: List[RemoteRepo] = List[RemoteRepo](RemoteRepo("central", "default", "http://central.maven.org/maven2/"))

    def setRemoteRepositories(remoteRepositories: List[RemoteRepo]): Builder = { this.remoteRepositories = remoteRepositories; this }
    def setLocalRepoDir(localRepoDir: String): Builder = { this.localRepoDir = localRepoDir; this }

    def build(): DependencyLibDownloader ={
      require(localRepoDir != null && !localRepoDir.isEmpty, "Local Repository Directory cannot be null or empty")
      require(new File(localRepoDir).exists(), s"Local Repository Directory does not exists: ${localRepoDir}")
      require(remoteRepositories != null && !remoteRepositories.isEmpty, "Remote Repositories cannot be null or empty list")

      val localRepository = new LocalRepository(localRepoDir)
      val repositorySystemSession = MavenRepositorySystemUtils.newSession()
      val remoteRepositoryList = remoteRepositories.map(r => createNewRemoteRepository(r.id, r.tp, r.url))
      repositorySystemSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(repositorySystemSession, localRepository))
      new DependencyLibDownloader(repositorySystem, repositorySystemSession, remoteRepositoryList)
    }
  }
}

class DependencyLibDownloader private (_repositorySystem: RepositorySystem, _repositorySystemSession: RepositorySystemSession, remoteRepositories: List[RemoteRepository]) {
  val repositorySystem = _repositorySystem
  val repositorySystemSession = _repositorySystemSession
  val classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE)
  val remoteRepositoryList = new util.ArrayList[RemoteRepository]()
  remoteRepositories.foreach(r => remoteRepositoryList.add(r))

  @throws(classOf[DependencyResolutionException])
  def downloadArtifactory(group: String, name: String, version: String): List[File] ={
    val artifact = new DefaultArtifact(group, name, "jar", version)

    val collectRequest = new CollectRequest()
    collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE))
    collectRequest.setRepositories(remoteRepositoryList)

    val dependencyRequest = new DependencyRequest(collectRequest, classpathFilter)
    val results = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest).getArtifactResults()
    val downloadedFiles = new mutable.ListBuffer[File]
    for(idx <- 0 until results.size())
      downloadedFiles += results.get(idx).getArtifact.getFile
    downloadedFiles.toList
  }
}
